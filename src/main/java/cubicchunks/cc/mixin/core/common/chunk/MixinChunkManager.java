package cubicchunks.cc.mixin.core.common.chunk;

import static net.minecraft.util.math.SectionPos.extractX;
import static net.minecraft.util.math.SectionPos.extractY;
import static net.minecraft.util.math.SectionPos.extractZ;
import static net.minecraft.world.server.ChunkManager.MAX_LOADED_LEVEL;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ISectionHolder;
import cubicchunks.cc.chunk.ISectionStatusListener;
import cubicchunks.cc.chunk.ticket.CubeTaskPriorityQueueSorter;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.annotation.Nullable;

@Mixin(ChunkManager.class)
public abstract class MixinChunkManager implements IChunkManager {

    private CubeTaskPriorityQueueSorter cubeTaskPriorityQueueSorter;

    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedSections = new Long2ObjectLinkedOpenHashMap<>();
    private final LongSet unloadableSections = new LongOpenHashSet();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> sectionsToUnload = new Long2ObjectLinkedOpenHashMap<>();

    @Shadow @Final private ServerWorldLightManager lightManager;

    @Shadow private boolean immutableLoadedChunksDirty;

    @Shadow @Final private IChunkStatusListener field_219266_t;

    @Inject(method = "<init>", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onConstruct(ServerWorld worldIn, File worldDirectory, DataFixer p_i51538_3_, TemplateManager templateManagerIn,
                             Executor p_i51538_5_, ThreadTaskExecutor mainThreadIn, IChunkLightProvider p_i51538_7_,
                             ChunkGenerator generatorIn, IChunkStatusListener p_i51538_9_, Supplier p_i51538_10_,
                             int p_i51538_11_, CallbackInfo ci, DelegatedTaskExecutor delegatedtaskexecutor,
                             ITaskExecutor itaskexecutor, DelegatedTaskExecutor delegatedtaskexecutor1) {

        this.cubeTaskPriorityQueueSorter = new CubeTaskPriorityQueueSorter(ImmutableList.of(delegatedtaskexecutor,
                itaskexecutor, delegatedtaskexecutor1), p_i51538_5_, Integer.MAX_VALUE);
    }

    @Nullable
    @Override
    public ChunkHolder setSectionLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
        if (oldLevel > MAX_LOADED_LEVEL && newLevel > MAX_LOADED_LEVEL) {
            return holder;
        } else {
            if (holder != null) {
                holder.setChunkLevel(newLevel);
            }

            if (holder != null) {
                if (newLevel > MAX_LOADED_LEVEL) {
                    this.unloadableSections.add(sectionPosIn);
                } else {
                    this.unloadableSections.remove(sectionPosIn);
                }
            }

            if (newLevel <= MAX_LOADED_LEVEL && holder == null) {
                holder = this.sectionsToUnload.remove(sectionPosIn);
                if (holder != null) {
                    holder.setChunkLevel(newLevel);
                } else {

                    holder = new ChunkHolder(new ChunkPos(extractX(sectionPosIn), extractZ(sectionPosIn)), newLevel, this.lightManager,
                            this.cubeTaskPriorityQueueSorter, (ChunkHolder.IPlayerProvider) this);
                    ((ISectionHolder)holder).setYPos(extractY(sectionPosIn));
                }
                this.loadedSections.put(sectionPosIn, holder);
                this.immutableLoadedChunksDirty = true;
            }

            return holder;
        }
    }

    @Override
    public LongSet getUnloadableSections()
    {
        return this.unloadableSections;
    }

    @Override
    public ChunkHolder getSectionHolder(long sectionPosIn)
    {
        return loadedSections.get(sectionPosIn);
    }

    // TODO: remove when cubic chunks versions are done
    @SuppressWarnings({"UnresolvedMixinReference"})
    @Inject(method = "lambda$func_219244_a$13", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;statusChanged(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/ChunkStatus;)V")
    )
    private void on_func_219244_a_StatusChange(ChunkStatus chunkStatusIn, ChunkPos chunkpos,
            ChunkHolder chunkHolderIn, Either<?, ?> p_223180_4_, CallbackInfoReturnable<CompletionStage<?>> cir) {
        if (((ISectionHolder) chunkHolderIn).getSectionPos() != null) {
            ((ISectionStatusListener) field_219266_t).sectionStatusChanged(
                    ((ISectionHolder) chunkHolderIn).getSectionPos(),
                    chunkStatusIn);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$scheduleSave$10", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;statusChanged(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/ChunkStatus;)V")
    )
    private void onScheduleSaveStatusChange(ChunkHolder chunkHolderIn, CompletableFuture<?> completablefuture,
            long chunkPosIn, IChunk p_219185_5_, CallbackInfo ci) {
        if (((ISectionHolder) chunkHolderIn).getSectionPos() != null) {
            ((ISectionStatusListener) field_219266_t).sectionStatusChanged(
                    ((ISectionHolder) chunkHolderIn).getSectionPos(),  null);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$null$18", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;statusChanged(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/ChunkStatus;)V"))
    private void onGenerateStatusChange(ChunkStatus chunkStatusIn, ChunkHolder chunkHolderIn, ChunkPos chunkpos, List<?> p_223148_4_,
            CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (((ISectionHolder) chunkHolderIn).getSectionPos() != null) {
            ((ISectionStatusListener) field_219266_t).sectionStatusChanged(
                    ((ISectionHolder) chunkHolderIn).getSectionPos(),  null);
        }
    }
}
