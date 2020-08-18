package io.github.opencubicchunks.cubicchunks.world;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;

import java.util.Set;

public class CubicHeightmap {

    /**
     * @See Heightmap.updateChunkHeightmap
     */
    public static void updateChunkHeightmaps(IChunk chunkIn, Set<Heightmap.Type> types, int minY, int maxY) {

        int i = types.size();
        ObjectList<ICubicHeightmap> heightmaps = new ObjectArrayList<>(i);
        ObjectListIterator<ICubicHeightmap> heightmapIterator = heightmaps.iterator();

        try (BlockPos.PooledMutable blockpos$pooledmutable = BlockPos.PooledMutable.retain()) {

            for(int k = 0; k < 16; ++k) {
                for(int l = 0; l < 16; ++l) {
                    for(Heightmap.Type heightmap$type : types) {
                        heightmaps.add((ICubicHeightmap) chunkIn.getHeightmap(heightmap$type));
                    }

                    for(int i1 = maxY; i1 >= minY; --i1) {
                        blockpos$pooledmutable.setPos(k, i1, l);
                        BlockState blockstate = chunkIn.getBlockState(blockpos$pooledmutable);
                        if (blockstate.getBlock() != Blocks.AIR) {
                            while(heightmapIterator.hasNext()) {
                                ICubicHeightmap heightmap = heightmapIterator.next();
                                heightmap.update(k, l, i1 + 1, blockstate);
                            }

                            if (heightmaps.isEmpty()) {
                                break;
                            }

                            heightmapIterator.back(i);
                        }
                    }
                }
            }
        }
    }

}
