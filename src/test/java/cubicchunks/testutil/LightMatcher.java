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

import static cubicchunks.util.MathUtil.max;
import static net.minecraft.world.EnumSkyBlock.BLOCK;
import static net.minecraft.world.EnumSkyBlock.SKY;

import cubicchunks.lighting.LightBlockAccess;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class LightMatcher extends TypeSafeDiagnosingMatcher<LightBlockAccess> {

    @Nonnull private final BlockPos start;
    @Nonnull private final BlockPos end;

    LightMatcher(StructureBoundingBox box) {
        this.start = new BlockPos(box.minX, box.minY, box.minZ);
        this.end = new BlockPos(box.maxX, box.maxY, box.maxZ);
    }

    /**
     * Subclasses should implement this. The item will already have been checked
     * for the specific type and will never be null.
     */
    @Override protected boolean matchesSafely(LightBlockAccess access, Description mismatchDescription) {
        return StreamSupport.stream(BlockPos.getAllInBox(start, end).spliterator(), false).allMatch(pos -> {
            if (access.getLightFor(SKY, pos) != getExpected(access, pos, SKY)) {
                addMismatchDescription(mismatchDescription, access, pos, SKY);
                return false;
            }
            if (access.getLightFor(BLOCK, pos) != getExpected(access, pos, BLOCK)) {
                addMismatchDescription(mismatchDescription, access, pos, BLOCK);
                return false;
            }
            return true;
        });
    }

    private void addMismatchDescription(Description desc, LightBlockAccess access, BlockPos pos, EnumSkyBlock type) {
        desc.appendText("light value for type " + type + " at " + pos + " didn't match expected value ").
                appendValue(getExpected(access, pos, type)).appendText(", found ").appendValue(access.getLightFor(type, pos)).
                appendText(" with neighbors" +
                        " D=" + access.getLightFor(type, pos.down()) +
                        " U=" + access.getLightFor(type, pos.up()) +
                        " W=" + access.getLightFor(type, pos.west()) +
                        " E=" + access.getLightFor(type, pos.east()) +
                        " N=" + access.getLightFor(type, pos.north()) +
                        " S=" + access.getLightFor(type, pos.south()) +
                        " emitted=" + access.getEmittedLight(pos, type) +
                        " opacity=" + access.getBlockLightOpacity(pos));
    }

    private int getExpected(LightBlockAccess access, BlockPos pos, EnumSkyBlock type) {
        return max(access.getEmittedLight(pos, type), access.getLightFromNeighbors(type, pos));
    }

    /**
     * Generates a description of the object.  The description may be part of a
     * a description of a larger object of which this is just a component, so it
     * should be worded appropriately.
     *
     * @param description The description to be built or appended to.
     */
    @Override public void describeTo(Description description) {
        description.
                appendText("for each BlockPos between ").
                appendValue(start).
                appendText(" and ").
                appendValue(end).
                appendText(" lightValue == max(emittedLight, maxNeighborLightValue - max(1, opacity))");
    }
}
