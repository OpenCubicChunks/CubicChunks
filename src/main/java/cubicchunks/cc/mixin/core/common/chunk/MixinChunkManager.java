package cubicchunks.cc.mixin.core.common.chunk;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ISectionHolder;
import cubicchunks.cc.chunk.ticket.CubeTaskPriorityQueueSorter;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.math.ChunkPos;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static net.minecraft.util.math.SectionPos.*;
import static net.minecraft.world.server.ChunkManager.MAX_LOADED_LEVEL;

@Mixin(ChunkManager.class)
public class MixinChunkManager implements IChunkManager {

    private CubeTaskPriorityQueueSorter cubeTaskPriorityQueueSorter;

    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedSections = new Long2ObjectLinkedOpenHashMap<>();
    private final LongSet unloadableSections = new LongOpenHashSet();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> sectionsToUnload = new Long2ObjectLinkedOpenHashMap<>();

    @Shadow @Final private ServerWorldLightManager lightManager;

    @Shadow private boolean immutableLoadedChunksDirty;

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
}
