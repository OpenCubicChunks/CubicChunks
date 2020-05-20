package cubicchunks.cc.mixin.core.common.chunk;

import cubicchunks.cc.chunk.ICubeHolder;
import cubicchunks.cc.chunk.ticket.CubeTaskPriorityQueueSorter;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkTaskPriorityQueueSorter;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

import static net.minecraft.util.math.SectionPos.*;
import static net.minecraft.world.server.ChunkManager.MAX_LOADED_LEVEL;

@Mixin(ChunkManager.class)
public class MixinChunkManager {

    private CubeTaskPriorityQueueSorter cubeTaskPriorityQueueSorter;

    @Shadow @Final private ChunkTaskPriorityQueueSorter field_219263_q;

    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedChunks;
    @Shadow @Final private LongSet unloadableChunks;
    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> chunksToUnload;

    @Shadow @Final private ServerWorldLightManager lightManager;

    @Shadow private boolean immutableLoadedChunksDirty;

    @Nullable
    private ChunkHolder setSectionLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
        if (oldLevel > MAX_LOADED_LEVEL && newLevel > MAX_LOADED_LEVEL) {
            return holder;
        } else {
            if (holder != null) {
                holder.setChunkLevel(newLevel);
            }

            if (holder != null) {
                if (newLevel > MAX_LOADED_LEVEL) {
                    this.unloadableChunks.add(sectionPosIn);
                } else {
                    this.unloadableChunks.remove(sectionPosIn);
                }
            }

            if (newLevel <= MAX_LOADED_LEVEL && holder == null) {
                holder = this.chunksToUnload.remove(sectionPosIn);
                if (holder != null) {
                    holder.setChunkLevel(newLevel);
                } else {

                    holder = new ChunkHolder(new ChunkPos(extractX(sectionPosIn), extractZ(sectionPosIn)), newLevel, this.lightManager, this.field_219263_q, (ChunkHolder.IPlayerProvider) this);
                    ((ICubeHolder)holder).setYPos(extractY(sectionPosIn));
                }
                this.loadedChunks.put(sectionPosIn, holder);
                this.immutableLoadedChunksDirty = true;
            }

            return holder;
        }
    }
}
