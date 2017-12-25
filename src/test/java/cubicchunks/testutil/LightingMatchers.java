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
package cubicchunks.testutil;

import com.google.common.collect.Lists;
import cubicchunks.lighting.ILightBlockAccess;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LightingMatchers {

    public static Matcher<ILightBlockAccess> hasCorrectLight(StructureBoundingBox range) {
        return new LightMatcher(range);
    }

    public static Matcher<ILightBlockAccess> hasLightValue(EnumSkyBlock type, int light, PosSection blocks) {
        return new TypeSafeDiagnosingMatcher<ILightBlockAccess>() {
            @Override protected boolean matchesSafely(ILightBlockAccess item, Description mismatchDescription) {
                for (BlockPos p : blocks) {
                    if (item.getLightFor(type, p) != light) {
                        mismatchDescription.appendText(type.toString().toLowerCase() + "light value at ").appendValue(p).appendText(" is ")
                                .appendValue(item.getLightFor(type, p));
                        return false;
                    }
                }
                return true;
            }

            @Override public void describeTo(Description description) {
                description.appendText(type.toString().toLowerCase() + "light value ").appendValue(light).appendText(" for ")
                        .appendValue(blocks);
            }
        };
    }

    public static Matcher<ILightBlockAccess> hasBlockLightValue(int light, PosSection blocks) {
        return hasLightValue(EnumSkyBlock.BLOCK, light, blocks);
    }

    public static Matcher<ILightBlockAccess> hasSkyLightValue(int light, PosSection blocks) {
        return hasLightValue(EnumSkyBlock.SKY, light, blocks);
    }

    public static StructureBoundingBox range(BlockPos start, BlockPos end) {
        return new StructureBoundingBox(start, end);
    }

    public static StructureBoundingBox range(int radius) {
        return range(pos(-radius, -radius, -radius), pos(radius, radius, radius));
    }

    public static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z) {
            @Override public String toString() {
                return "[" + getX() + ", " + getY() + ", " + getZ() + "]";
            }
        };
    }

    public static BlockPos[] posRange(BlockPos start, BlockPos end) {
        ArrayList<BlockPos> p = Lists.newArrayList(BlockPos.getAllInBox(start, end));
        return p.toArray(new BlockPos[p.size()]);
    }

    /**
     * Creates new BlockPos with special toString method
     */
    public static BlockPos pos(BlockPos pos) {
        return new BlockPos(pos) {
            @Override public String toString() {
                return "[" + getX() + ", " + getY() + ", " + getZ() + "]";
            }
        };
    }

    public interface PosSection extends Iterable<BlockPos> {

        boolean hasPos(BlockPos pos);

        Set<BlockPos> toSet();

        default PosSection except(PosSection exclude) {
            return new ExcludedSection(this, exclude);
        }

        /**
         * Identity function for decorating other PosSections.
         */
        static PosSection in(PosSection pos) {
            return pos;
        }

        static PosSection block(int x, int y, int z) {
            return block(pos(x, y, z));
        }

        static PosSection block(BlockPos p) {
            return new BlockPosSection(pos(p));
        }

        static PosSection section(BlockPos start, BlockPos end) {
            return new SimplePosSection(start, end);
        }

        static PosSection allOf(PosSection... sections) {
            return new MultiPosSection(sections);
        }

        // NOTE: nesting these is NOT a good idea
        static PosSection neighborsOf(int x, int y, int z) {
            return neighborsOf(pos(x, y, z));
        }

        static PosSection neighborsOf(BlockPos pos) {
            return neighborsOf(block(pos));
        }

        static PosSection neighborsOf(PosSection section) {
            return new NeighborsPosSection(section);
        }
    }

    public static class BlockPosSection implements PosSection {

        private final BlockPos pos;

        public BlockPosSection(BlockPos at) {
            this.pos = at;
        }

        public boolean hasPos(BlockPos pos) {
            return this.pos.equals(pos);
        }

        @Override public Iterator<BlockPos> iterator() {
            return this.toSet().iterator();
        }

        public Set<BlockPos> toSet() {
            return Collections.singleton(pos);
        }

        @Override public String toString() {
            return "block" + pos;
        }
    }

    public static class NeighborsPosSection implements PosSection {
        private final PosSection section;

        public NeighborsPosSection(PosSection section) {
            this.section = section;
        }

        public boolean hasPos(BlockPos pos) {
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (this.section.hasPos(pos.offset(facing))) {
                    return true;
                }
            }
            return false;
        }

        @Override public Iterator<BlockPos> iterator() {
            return this.toSet().iterator();
        }

        public Set<BlockPos> toSet() {
            Set<BlockPos> set = section.toSet();
            Set<BlockPos> newSet = new HashSet<>();
            for (BlockPos pos : set) {
                for (EnumFacing facing : EnumFacing.VALUES) {
                    newSet.add(pos.offset(facing));
                }
            }
            return newSet;
        }

        @Override public String toString() {
            return "neighborsOf(" + section + ")";
        }
    }

    public static class SimplePosSection implements PosSection {

        private final BlockPos start;
        private final BlockPos end;

        public SimplePosSection(BlockPos from, BlockPos to) {
            this.start = from;
            this.end = to;
        }

        public boolean hasPos(BlockPos pos) {
            return new StructureBoundingBox(start, end).isVecInside(pos);
        }

        @Override public Iterator<BlockPos> iterator() {
            return this.toSet().iterator();

        }

        public Set<BlockPos> toSet() {
            Set<BlockPos> set = new HashSet<>();
            BlockPos.getAllInBox(start, end).forEach(p -> set.add(pos(p.getX(), p.getY(), p.getZ())));
            return set;
        }

        @Override public String toString() {
            return "section(from " + start + " to " + end + ")";
        }
    }

    public static class MultiPosSection implements PosSection {

        private final List<PosSection> included;

        public MultiPosSection(PosSection... sections) {
            this.included = Arrays.asList(sections);
        }

        public boolean hasPos(BlockPos pos) {
            return included.stream().anyMatch(section -> section.hasPos(pos));
        }

        @Override public Iterator<BlockPos> iterator() {
            return this.toSet().iterator();

        }

        public Set<BlockPos> toSet() {
            Set<BlockPos> set = new HashSet<>();
            included.forEach(s -> set.addAll(s.toSet()));
            return set;
        }

        @Override public String toString() {
            return "allOf(" + StringUtils.join(included, ", ") + ")";
        }
    }

    public static class ExcludedSection implements PosSection {

        private final PosSection included;
        private final PosSection excluded;

        public ExcludedSection(PosSection included, PosSection excluded) {
            this.included = included;
            this.excluded = excluded;
        }

        public boolean hasPos(BlockPos pos) {
            return !excluded.hasPos(pos) && included.hasPos(pos);
        }

        @Override public Iterator<BlockPos> iterator() {
            return this.toSet().iterator();

        }

        public Set<BlockPos> toSet() {
            Set<BlockPos> set = included.toSet();
            set.removeAll(excluded.toSet());
            return set;
        }

        @Override public String toString() {
            return included.toString() + ".except(" + excluded.toString() + ")";
        }
    }
}
