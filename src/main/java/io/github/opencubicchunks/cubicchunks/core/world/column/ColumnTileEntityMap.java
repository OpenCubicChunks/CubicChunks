/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.world.column;

import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.Map.Entry;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ColumnTileEntityMap implements Map<BlockPos, TileEntity> {

    private final IColumn column;

    public ColumnTileEntityMap(IColumn column) {
        this.column = column;
    }

    @Override public int size() {
        return column.getLoadedCubes().stream()
                .map(ICube::getTileEntityMap)
                .map(Map::size)
                .reduce(Integer::sum).orElse(0);
    }

    @Override public boolean isEmpty() {
        return  column.getLoadedCubes().stream()
                .map(ICube::getTileEntityMap)
                .allMatch(Map::isEmpty);
    }

    @Override public boolean containsKey(Object o) {
        if (!(o instanceof BlockPos)) {
            return false;
        }
        BlockPos pos = (BlockPos) o;
        int y = Coords.blockToCube(pos.getY());
        ICube cube = column.getCube(y); // see comment in get() for why getCube instead of getLoadedCube is used
        return cube.getTileEntityMap().containsKey(o);
    }

    @Override public boolean containsValue(Object o) {
        if (!(o instanceof TileEntity)) {
            return false;
        }
        BlockPos pos = ((TileEntity) o).getPos();
        int y = Coords.blockToCube(pos.getY());
        ICube cube = column.getLoadedCube(y);
        assert cube != null : "Cube is null but tile entity in it exists!";
        return cube.getTileEntityMap().containsValue(o);
    }

    @Nullable
    @Override public TileEntity get(Object o) {
        if (!(o instanceof BlockPos)) {
            return null;
        }
        BlockPos pos = (BlockPos) o;
        int y = Coords.blockToCube(pos.getY());
        // when something other than CHECK is passed into Chunk.getTileEntity, then if the current TE is null
        // it will try to create a new one. To do that it will get a block which will load the cube
        // with the already existing TE. But the "create new TE" code will continue not knowing the TE just got loaded
        // and will replace the newly loaded one
        // so load the cube here to avoid problems. Other places use getCube() for consistency
        ICube cube = column.getCube(y);
        return cube.getTileEntityMap().get(o);
    }

    @Override public TileEntity put(BlockPos blockPos, TileEntity tileEntity) {
        int y = Coords.blockToCube(blockPos.getY());
        ICube cube = column.getCube(y);
        return cube.getTileEntityMap().put(blockPos, tileEntity);
    }

    @Nullable
    @Override public TileEntity remove(Object o) {
        if (!(o instanceof BlockPos)) {
            return null;
        }
        BlockPos pos = (BlockPos) o;
        int y = Coords.blockToCube(pos.getY());
        ICube cube = column.getLoadedCube(y);
        return cube == null ? null : cube.getTileEntityMap().remove(pos);
    }

    @Override public void putAll(Map<? extends BlockPos, ? extends TileEntity> map) {
        map.forEach(this::put);
    }

    @Override public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override public Set<BlockPos> keySet() {
        return new AbstractSet<BlockPos>() {
            @Override public int size() {
                return ColumnTileEntityMap.this.size();
            }

            @Override public boolean isEmpty() {
                return ColumnTileEntityMap.this.isEmpty();
            }

            @Override public boolean contains(Object o) {
                return ColumnTileEntityMap.this.containsKey(o);
            }

            @Nonnull @Override public Iterator<BlockPos> iterator() {
                return new Iterator<BlockPos>() {
                    Iterator<? extends ICube> cubes = column.getLoadedCubes().iterator();
                    Iterator<BlockPos> curIt = !cubes.hasNext() ? null : cubes.next().getTileEntityMap().keySet().iterator();
                    BlockPos nextVal;

                    @Override public boolean hasNext() {
                        if (nextVal != null) {
                            return true;
                        }
                        if (curIt == null) {
                            return false;
                        }
                        while (!curIt.hasNext() && cubes.hasNext()) {
                            curIt = cubes.next().getTileEntityMap().keySet().iterator();
                        }
                        if (!curIt.hasNext()) {
                            return false;
                        }
                        nextVal = curIt.next();
                        return true;
                    }

                    @Override public BlockPos next() {
                        if (hasNext()) {
                            BlockPos next = nextVal;
                            nextVal = null;
                            return next;
                        }
                        throw new NoSuchElementException();
                    }
                };
            }

            @Override public boolean remove(Object o) {
                return ColumnTileEntityMap.this.remove(o) != null;
            }

            @Override public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override public Collection<TileEntity> values() {
        return new AbstractCollection<TileEntity>() {

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
                    Iterator<? extends ICube> cubes = column.getLoadedCubes().iterator();
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
                            TileEntity next = nextVal;
                            nextVal = null;
                            return next;
                        }
                        throw new NoSuchElementException();
                    }
                };
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

            @Override public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override public Set<Entry<BlockPos, TileEntity>> entrySet() {
        return new AbstractSet<Entry<BlockPos, TileEntity>>() {
            @Override public int size() {
                return ColumnTileEntityMap.this.size();
            }

            @Override public boolean isEmpty() {
                return ColumnTileEntityMap.this.isEmpty();
            }

            @Override public boolean contains(Object o) {
                return ColumnTileEntityMap.this.containsKey(o);
            }

            @Nonnull @Override public Iterator<Entry<BlockPos, TileEntity>> iterator() {
                return new Iterator<Entry<BlockPos, TileEntity>>() {
                    Iterator<? extends ICube> cubes = column.getLoadedCubes().iterator();
                    Iterator<Entry<BlockPos, TileEntity>> curIt = !cubes.hasNext() ? null : cubes.next().getTileEntityMap().entrySet().iterator();
                    Entry<BlockPos, TileEntity> nextVal;

                    @Override public boolean hasNext() {
                        if (nextVal != null) {
                            return true;
                        }
                        if (curIt == null) {
                            return false;
                        }
                        while (!curIt.hasNext() && cubes.hasNext()) {
                            curIt = cubes.next().getTileEntityMap().entrySet().iterator();
                        }
                        if (!curIt.hasNext()) {
                            return false;
                        }
                        nextVal = curIt.next();
                        return true;
                    }

                    @Override public Entry<BlockPos, TileEntity> next() {
                        if (hasNext()) {
                            Entry<BlockPos, TileEntity> e = nextVal;
                            nextVal = null;
                            return e;
                        }
                        throw new NoSuchElementException();
                    }
                };
            }

            @Override public boolean remove(Object o) {
                return ColumnTileEntityMap.this.remove(o) != null;
            }

            @Override public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
