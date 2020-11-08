package io.github.opencubicchunks.cubicchunks.chunk.heightmap;

import com.google.common.annotations.VisibleForTesting;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.utils.AddressTools;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.BitSet;

public class CCHeightmap extends Heightmap {

        public static final int MAX_SCALE = 10; // TODO: set real value
        private static final int NODE_COUNT_BITS = 1;
        public static final int NODE_COUNT = 1 << NODE_COUNT_BITS;

        private final BitStorage heights;
        private final BitSet dirtyPositions;
        private final int scale, scaledY;
        private CCHeightmap parent;
        private final CCHeightmap[] nodes = new CCHeightmap[NODE_COUNT];
        private final IBigCube cube; // null if not cube scale
        private boolean hasLoadedCubes = false;
        private final Heightmap.Types types;

        public CCHeightmap(Heightmap.Types types) {
            this(MAX_SCALE, 0, null, types);
        }

        public CCHeightmap(int scale, int scaledY, CCHeightmap parent, Heightmap.Types types) {
            this(scale, scaledY, parent, null, types);
        }

        public CCHeightmap(int scale, int scaledY, CCHeightmap parent, IBigCube cube, Heightmap.Types types) {
            super((ChunkAccess) cube, types);
            this.heights = new BitStorage(6 + scale*NODE_COUNT_BITS, IBigCube.DIAMETER_IN_BLOCKS * IBigCube.DIAMETER_IN_BLOCKS);
            this.dirtyPositions = new BitSet(IBigCube.DIAMETER_IN_BLOCKS * IBigCube.DIAMETER_IN_BLOCKS);
            this.scale = scale;
            this.scaledY = scaledY;
            this.cube = cube;
            this.parent = parent;
            this.types = types;
        }

        public int getHeight(int x, int z) {
            int idx = index(x, z);
            if (!dirtyPositions.get(idx)) {
                int rawY = heights.get(idx);
                if (rawY == 0) {
                    return Integer.MIN_VALUE;
                }
                return rawY - 1 + scaledYBottomY(scaledY, scale);
            }

            int maxY = Integer.MIN_VALUE;
            if (scale == 0) {
                for (int dy = IBigCube.DIAMETER_IN_BLOCKS; dy > 0; dy--) {
                    if (!cube.getBlockState(x, dy, z).isAir()) { // TODO: use test predicates
                        int minY = scaledY * IBigCube.DIAMETER_IN_BLOCKS;
                        maxY = minY + dy;;
                        break;
                    }
                }
            } else {
                for (int i = nodes.length - 1; i >= 0; i--) {
                    CCHeightmap node = nodes[i];
                    if (node == null) {
                        continue;
                    }
                    int y = node.getHeight(x, z);
                    if (y != Integer.MIN_VALUE) {
                        maxY = y;
                        break;
                    }
                }
            }
            heights.set(idx, maxY == Integer.MIN_VALUE ? 0 : maxY + 1 - scaledYBottomY(scaledY, scale) * IBigCube.DIAMETER_IN_BLOCKS);
            dirtyPositions.clear(idx);
            return maxY;
        }

        public void markDirty(int x, int z) {
            dirtyPositions.set(index(x, z));
            if (parent != null) {
                parent.markDirty(x, z);
            }
        }

        public void unloadCube(IBigCube cube) {

        }

        public void loadCube(IBigCube newCube, boolean markDirty) {
            if (markDirty) {
                dirtyPositions.set(0, IBigCube.DIAMETER_IN_BLOCKS * IBigCube.DIAMETER_IN_BLOCKS - 1);
            }
            hasLoadedCubes = true;
            if (this.scale == 0) {
                return;
            }
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[1] != null) {
                    continue;
                }
                int newScaledY = indexToScaledY(i, scale, scaledY);
                CCHeightmap newMap = loadNode(newScaledY, scale - 1, newCube);
                nodes[i] = newMap;
            }
            int idx = indexOfRawHeightNode(newCube.getCubePos().getY(), scale, scaledY);
            nodes[idx].loadCube(newCube, markDirty);
        }

        public CCHeightmap getParent() {
            return parent;
        }

        public CCHeightmap getChild(int i) {
            return nodes[i];
        }

        public CCHeightmap getCubeNode(int y) {
            if (scale == 0) {
                if (y != scaledY) {
                    throw new IllegalArgumentException("Invalid Y: " + y + ", expected " + scaledY);
                }
                return this;
            }
            int idx = indexOfRawHeightNode(y, scale, scaledY);
            CCHeightmap node = nodes[idx];
            if (node == null) {
                return null;
            }
            return node.getCubeNode(y);
        }

        public IBigCube getCube() {
            return cube;
        }

        private CCHeightmap loadNode(int newScaledY, int scale, IBigCube newCube) {
            // TODO: loading from disk
            if (scale == 0) {
                return new CCHeightmap(scale, newScaledY, this, newCube, this.types);
            }
            return new CCHeightmap(scale, newScaledY, this, this.types);
        }

        private int index(int x, int z) {
            return AddressTools.getLocalAddress(x, z);
        }

        @VisibleForTesting
        static int indexOfRawHeightNode(int y, int nodeScale, int nodeScaledY) {
            if (nodeScale == 0) {
                throw new UnsupportedOperationException("Why?");
            }
            if (nodeScale == MAX_SCALE) {
                return y < 0 ? 0 : 1;
            }
            int scaled = y >> ((nodeScale - 1) * NODE_COUNT_BITS);
            return scaled - (nodeScaledY << NODE_COUNT_BITS);
        }

        @VisibleForTesting
        static int indexToScaledY(int index, int nodeScale, int nodeScaledY) {
            if (nodeScale == 0) {
                throw new UnsupportedOperationException("Why?");
            }
            if (nodeScale == MAX_SCALE) {
                return index == 0 ? -1 : 0;
            }
            return (nodeScaledY << NODE_COUNT_BITS) + index;
        }

        @VisibleForTesting
        static int scaledYBottomY(int scaledY, int scale) {
            if (scale == MAX_SCALE) {
                return -(1 << ((scale - 1) * NODE_COUNT_BITS));
            }
            return scaledY << (scale * NODE_COUNT_BITS);
        }
    }