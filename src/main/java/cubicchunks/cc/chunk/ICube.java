package cubicchunks.cc.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkStatus;

public interface ICube extends IBlockReader {
    SectionPos getSectionPos();

    void setCubeStatus(ChunkStatus status);
    ChunkStatus getCubeStatus();

    BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving);

    void addTileEntity(BlockPos pos, TileEntity tileEntity);
    void removeTileEntity(BlockPos pos);

    // TODO: isModified
    boolean isModified();

    void setModified(boolean modified);
}
