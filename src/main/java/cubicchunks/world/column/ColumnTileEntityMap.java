/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.world.column;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ColumnTileEntityMap implements Map<BlockPos, TileEntity> {

    private final ICubicWorld world;
    private CubeMap cubeMap;
    private final int columnX;
    private final int columnZ;

    public ColumnTileEntityMap(CubeMap cubeMap, ICubicWorld world, int columnX, int columnZ) {
        this.cubeMap = cubeMap;
        this.world = world;
        this.columnX = columnX;
        this.columnZ = columnZ;
    }

    @Override public int size() {
        return cubeMap.all().stream()
                .map(Cube::getTileEntityMap)
                .map(Map::size)
                .reduce((a, b) -> a + b).orElse(0);
    }

    @Override public boolean isEmpty() {
        return cubeMap.all().stream()
                .map(Cube::getTileEntityMap)
                .allMatch(Map::isEmpty);
    }

    @Override public boolean containsKey(Object o) {
        if (!(o instanceof BlockPos)) {
            return false;
        }
        BlockPos pos = (BlockPos) o;
        int y = Coords.blockToCube(pos.getY());
        Cube cube = world.getCubeCache().getLoadedCube(columnX, y, columnZ);
        return cube == null ? false : cube.getTileEntityMap().containsKey(o);
    }

    @Override public boolean containsValue(Object o) {
        if (!(o instanceof TileEntity)) {
            return false;
        }
        BlockPos pos = (BlockPos) o;
        int y = Coords.blockToCube(pos.getY());
        Cube cube = world.getCubeCache().getLoadedCube(columnX, y, columnZ);
        return cube == null ? false : cube.getTileEntityMap().containsValue(o);
    }

    @Override public TileEntity get(Object o) {
        if (!(o instanceof BlockPos)) {
            return null;
        }
        BlockPos pos = (BlockPos) o;
        int y = Coords.blockToCube(pos.getY());
        Cube cube = world.getCubeCache().getLoadedCube(columnX, y, columnZ);
        return cube == null ? null : cube.getTileEntityMap().get(o);
    }

    @Override public TileEntity put(BlockPos blockPos, TileEntity tileEntity) {
        int y = Coords.blockToCube(blockPos.getY());
        Cube cube = world.getCubeCache().getCube(columnX, y, columnZ);
        return cube.getTileEntityMap().put(blockPos, tileEntity);
    }

    @Override public TileEntity remove(Object o) {
        if (!(o instanceof BlockPos)) {
            return null;
        }
        BlockPos pos = (BlockPos) o;
        int y = Coords.blockToCube(pos.getY());
        Cube cube = world.getCubeCache().getLoadedCube(columnX, y, columnZ);
        return cube == null ? null : cube.getTileEntityMap().remove(pos);
    }

    @Override public void putAll(Map<? extends BlockPos, ? extends TileEntity> map) {
        map.forEach(this::put);
    }

    @Override public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override public Set<BlockPos> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override public Collection<TileEntity> values() {
        return new Collection<TileEntity>() {

            @Override public int size() {
                return ColumnTileEntityMap.this.size();
            }

            @Override public boolean isEmpty() {
                return ColumnTileEntityMap.this.isEmpty();

            }

            @Override public boolean contains(Object o) {
                return ColumnTileEntityMap.this.containsValue(o);
            }

            @Override public Iterator<TileEntity> iterator() {
                return new Iterator<TileEntity>() {
                    Iterator<Cube> cubes = ColumnTileEntityMap.this.cubeMap.iterator();
                    Iterator<TileEntity> curIt = !cubes.hasNext() ? null : cubes.next().getTileEntityMap().values().iterator();
                    TileEntity nextVal;

                    @Override public boolean hasNext() {
                        if (nextVal != null) {
                            return true;
                        }
                        if (curIt == null) {
                            return false;
                        }
                        while (!curIt.hasNext() && cubes.hasNext()) {
                            curIt = cubes.next().getTileEntityMap().values().iterator();
                        }
                        if (!curIt.hasNext()) {
                            return false;
                        }
                        nextVal = curIt.next();
                        return true;
                    }

                    @Override public TileEntity next() {
                        if (hasNext()) {
                            return nextVal;
                        }
                        throw new NoSuchElementException();
                    }
                };
            }

            @Override public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            @Override public <T> T[] toArray(T[] ts) {
                throw new UnsupportedOperationException();
            }

            @Override public boolean add(TileEntity tileEntity) {
                return ColumnTileEntityMap.this.put(tileEntity.getPos(), tileEntity) == null;
            }

            @Override public boolean remove(Object o) {
                if (!(o instanceof TileEntity)) {
                    return false;
                }
                TileEntity te = (TileEntity) o;
                return ColumnTileEntityMap.this.remove(te.getPos(), te);
            }

            @Override public boolean containsAll(Collection<?> collection) {
                return collection.stream().allMatch(ColumnTileEntityMap.this::containsValue);
            }

            @Override public boolean addAll(Collection<? extends TileEntity> collection) {
                throw new UnsupportedOperationException("not implemented yet");
            }

            @Override public boolean removeAll(Collection<?> collection) {
                throw new UnsupportedOperationException("not implemented yet");
            }

            @Override public boolean retainAll(Collection<?> collection) {
                throw new UnsupportedOperationException("not implemented yet");
            }

            @Override public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override public Set<Entry<BlockPos, TileEntity>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
