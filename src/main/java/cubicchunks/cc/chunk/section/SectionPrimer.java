package cubicchunks.cc.chunk.section;

import cubicchunks.cc.chunk.ISection;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.lighting.WorldLightManager;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class SectionPrimer implements ISection {

    private SectionPos sectionPos;
    private ChunkSection section;


    //TODO: add TickList<Block> and TickList<Fluid>
    public SectionPrimer(SectionPos pos, @Nullable ChunkSection sectionIn)
    {
        this.sectionPos = pos;
        if(sectionIn == null) {
            this.section = new ChunkSection(pos.getY(), (short)0, (short)0, (short)0);
        }
        else {
            this.section = sectionIn;
        }
    }

    public ChunkSection getSection() {
        return this.section;
    }

    public BlockState getBlockState(BlockPos pos) {
        return ChunkSection.isEmpty(this.section) ?
                Blocks.AIR.getDefaultState() :
                this.section.getBlockState(pos.getX() & 15, pos.getX() & 15, pos.getZ() & 15);
    }

    @Nullable
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.section == Chunk.EMPTY_SECTION && state.getBlock() == Blocks.AIR) {
            return state;
        } else {
            return this.section.setBlockState(x, y, z, state);

            //TODO: finish implementing
            /*if (state.getLightValue(this, pos) > 0) {
                this.lightPositions.add(new BlockPos((x & 15) + this.getPos().getXStart(), y, (z & 15) + this.getPos().getZStart()));
            }

            ChunkSection chunksection = this.getSection(y >> 4);
            BlockState blockstate = chunksection.setBlockState(x & 15, y & 15, z & 15, state);
            if (this.status.isAtLeast(ChunkStatus.FEATURES) && state != blockstate && (state.getOpacity(this, pos) != blockstate.getOpacity(this, pos) || state.getLightValue(this, pos) != blockstate.getLightValue(this, pos) || state.isTransparent() || blockstate.isTransparent())) {
                WorldLightManager worldlightmanager = this.getWorldLightManager();
                worldlightmanager.checkBlock(pos);
            }

            EnumSet<Heightmap.Type> enumset1 = this.getStatus().getHeightMaps();
            EnumSet<Heightmap.Type> enumset = null;

            for(Heightmap.Type heightmap$type : enumset1) {
                Heightmap heightmap = this.heightmaps.get(heightmap$type);
                if (heightmap == null) {
                    if (enumset == null) {
                        enumset = EnumSet.noneOf(Heightmap.Type.class);
                    }

                    enumset.add(heightmap$type);
                }
            }

            if (enumset != null) {
                Heightmap.updateChunkHeightmaps(this, enumset);
            }

            for(Heightmap.Type heightmap$type1 : enumset1) {
                this.heightmaps.get(heightmap$type1).update(x & 15, y, z & 15, state);
            }

            return blockstate;*/
        }
    }
}
