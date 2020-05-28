package cubicchunks.cc.mixin.core.common.chunk;

import cubicchunks.cc.chunk.IChunkSection;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.EmptyTickList;
import net.minecraft.world.ITickList;
import net.minecraft.world.SerializableTickList;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkSection.class)
public abstract class MixinChunkSection implements IChunkSection {

    private ChunkStatus status = ChunkStatus.EMPTY;

    //private ITickList<Block> blocksToBeTicked;


    public ChunkStatus getCubeStatus() {
        return status;
    }
}
