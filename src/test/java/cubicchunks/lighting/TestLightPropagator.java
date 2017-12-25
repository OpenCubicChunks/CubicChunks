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
package cubicchunks.lighting;

import static cubicchunks.testutil.LightingMatchers.PosSection.allOf;
import static cubicchunks.testutil.LightingMatchers.PosSection.block;
import static cubicchunks.testutil.LightingMatchers.PosSection.neighborsOf;
import static cubicchunks.testutil.LightingMatchers.PosSection.in;
import static cubicchunks.testutil.LightingMatchers.PosSection.section;
import static cubicchunks.testutil.LightingMatchers.hasBlockLightValue;
import static cubicchunks.testutil.LightingMatchers.hasCorrectLight;
import static cubicchunks.testutil.LightingMatchers.hasSkyLightValue;
import static cubicchunks.testutil.LightingMatchers.pos;
import static cubicchunks.testutil.LightingMatchers.posRange;
import static cubicchunks.testutil.LightingMatchers.range;
import static cubicchunks.testutil.TestLightBlockAccessImpl.lightAccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import cubicchunks.testutil.LightingMatchers;
import cubicchunks.testutil.TestLightBlockAccessImpl;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import org.junit.Test;

import java.util.ArrayList;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestLightPropagator {
    // TODO: test callbacks

    // Manually verified tests

    @Test
    public void testEmitBlockLight2() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toSet = pos(0, 0, 0);

        access.setBlockLightSource(toSet, 2);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toSet, toSet), access, EnumSkyBlock.BLOCK, p -> {
        });

        verifyEqual(center.add(-1, -1, -1), access, EnumSkyBlock.BLOCK, new int[/*x*/][/*y*/][/*z*/]{
                {
                        {0, 0, 0},
                        {0, 1, 0},
                        {0, 0, 0}
                },
                {
                        {0, 1, 0},
                        {1, 2, 1},
                        {0, 1, 0}
                },
                {
                        {0, 0, 0},
                        {0, 1, 0},
                        {0, 0, 0}
                }
        });
    }

    @Test
    public void testEmitBlockLight3() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toSet = pos(0, 0, 0);

        access.setBlockLightSource(toSet, 3);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toSet, toSet), access, EnumSkyBlock.BLOCK, p -> {
        });

        verifyEqual(center.add(-2, -2, -2), access, EnumSkyBlock.BLOCK, new int[/*x*/][/*y*/][/*z*/]{
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 1, 2, 1, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 1, 0, 0},
                        {0, 1, 2, 1, 0},
                        {1, 2, 3, 2, 1},
                        {0, 1, 2, 1, 0},
                        {0, 0, 1, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 1, 2, 1, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                }
        });
    }

    // test for source strength 3 and hope it generalizes well
    @Test
    public void testUnspread3() {
        BlockPos center = pos(0, 0, 0);
        BlockPos toSet = pos(0, 0, 0);

        LightingMatchers.PosSection l3 = block(toSet);
        LightingMatchers.PosSection l2 = neighborsOf(l3).except(l3);
        LightingMatchers.PosSection l1 = neighborsOf(l2).except(allOf(l3, l2));
        int size = 5;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                make();

        l3.toSet().forEach(pos -> access.setLightFor(EnumSkyBlock.BLOCK, pos, 3));
        l2.toSet().forEach(pos -> access.setLightFor(EnumSkyBlock.BLOCK, pos, 2));
        l1.toSet().forEach(pos -> access.setLightFor(EnumSkyBlock.BLOCK, pos, 1));

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toSet, toSet), access, EnumSkyBlock.BLOCK, p -> {
        });

        assertThat(access, hasBlockLightValue(0, in(section(pos(-size, -size, -size), pos(size, size, size)))));
    }

    @Test
    public void testEmitBlockLight15InOpaque() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toSet = pos(0, 0, 0);

        for (EnumFacing direction : EnumFacing.values()) {
            access.setOpacity(toSet.offset(direction), 255);
        }
        access.setBlockLightSource(toSet, 15);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toSet, toSet), access, EnumSkyBlock.BLOCK, p -> {
        });

        verifyEqual(center.add(-2, -2, -2), access, EnumSkyBlock.BLOCK, new int[/*x*/][/*y*/][/*z*/]{
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 15, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                }
        });
    }

    @Test
    public void testEmitBlockLight15ConstrainedInBox() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                withOpaque(posRange(pos(-2, -2, -2), pos(2, 2, 2))).
                withTransparent(posRange(pos(-1, -1, -1), pos(1, 1, 1))).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toSet = pos(0, 0, 0);

        access.setBlockLightSource(toSet, 15);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toSet, toSet), access, EnumSkyBlock.BLOCK, p -> {
        });

        verifyEqual(center.add(-2, -2, -2), access, EnumSkyBlock.BLOCK, new int[/*x*/][/*y*/][/*z*/]{
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 12, 13, 12, 0},
                        {0, 13, 14, 13, 0},
                        {0, 12, 13, 12, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 13, 14, 13, 0},
                        {0, 14, 15, 14, 0},
                        {0, 13, 14, 13, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 12, 13, 12, 0},
                        {0, 13, 14, 13, 0},
                        {0, 12, 13, 12, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                }
        });
    }

    @Test
    public void testEmitBlockLight15MultipleSourcesConstrainedInBox() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                withOpaque(posRange(pos(-2, -2, -2), pos(2, 2, 2))).
                withTransparent(posRange(pos(-1, -1, -1), pos(1, 1, 1))).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toSet1 = pos(0, 0, 0);
        BlockPos toSet2 = pos(0, 1, 0);

        access.setBlockLightSource(toSet1, 15);
        access.setBlockLightSource(toSet2, 15);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toSet1, toSet2), access, EnumSkyBlock.BLOCK, p -> {
        });

        verifyEqual(center.add(-2, -2, -2), access, EnumSkyBlock.BLOCK, new int[/*x*/][/*y*/][/*z*/]{
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 13, 14, 13, 0},
                        {0, 14, 15, 14, 0},
                        {0, 13, 14, 13, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 13, 14, 13, 0},
                        {0, 14, 15, 14, 0},
                        {0, 13, 14, 13, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 12, 13, 12, 0},
                        {0, 13, 14, 13, 0},
                        {0, 12, 13, 12, 0},
                        {0, 0, 0, 0, 0}
                },
                {
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0}
                }
        });
    }

    // Automatically verified tests
    @Test
    public void testNoChanges() {
        int size = 20;
        ILightBlockAccess access = lightAccess(size).currentHeightsForInitSkyLight().make();
        BlockPos center = pos(0, 0, 0);
        Iterable<BlockPos> coordsToUpdate = BlockPos.getAllInBox(center, center);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testEmittingLightUpdate() {
        int size = 20;
        ILightBlockAccess access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withFullBlockLight(pos(0, 0, 0)).
                make();
        BlockPos center = pos(0, 0, 0);
        Iterable<BlockPos> coordsToUpdate = BlockPos.getAllInBox(center, center);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testEmittingLightUpdateSetAndRemove() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withFullBlockLight(pos(0, 0, 0)).
                make();
        BlockPos center = pos(0, 0, 0);
        Iterable<BlockPos> coordsToUpdate = BlockPos.getAllInBox(center, center);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);

        access.setBlockLightSource(pos(0, 0, 0), 0);
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testUpdateInLitAreaHasNoEffect() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withFullBlockLight(pos(0, 0, 0)).
                make();
        BlockPos center = pos(0, 0, 0);
        Iterable<BlockPos> coordsToUpdate = BlockPos.getAllInBox(center, center);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);

        coordsToUpdate = BlockPos.getAllInBox(pos(1, 2, 3), pos(1, 2, 3));
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testEmittingLightUpdateMultipleBlocks() {
        int size = 32;
        ILightBlockAccess access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withFullBlockLight(
                        pos(0, 0, 0),
                        pos(15, 15, 15),
                        pos(4, 5, 6)
                ).make();
        BlockPos center = pos(0, 0, 0);
        Iterable<BlockPos> coordsToUpdate = Lists.newArrayList(
                pos(0, 0, 0),
                pos(15, 15, 15),
                pos(4, 5, 6)
        );

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testSunlightShaftOpen() {
        int size = 20;
        ILightBlockAccess access = lightAccess(size).
                withOpaque(posRange(pos(-30, -10, -30), pos(30, -10, 30))).
                withOpaque(posRange(pos(-30, 10, -30), pos(30, 10, 30))).
                currentHeightsForInitSkyLight().
                withTransparent(pos(0, 10, 0)).
                make();
        BlockPos center = pos(0, 0, 0);
        Iterable<BlockPos> coordsToUpdate = BlockPos.getAllInBox(pos(0, -9, 0), pos(0, 10, 0));

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.SKY, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testSunlightShaftOpenClose() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                withOpaque(posRange(pos(-30, -10, -30), pos(30, -10, 30))).
                withOpaque(posRange(pos(-30, 10, -30), pos(30, 10, 30))).
                currentHeightsForInitSkyLight().
                withTransparent(pos(0, 10, 0)).
                make();
        BlockPos center = pos(0, 0, 0);
        Iterable<BlockPos> coordsToUpdate = BlockPos.getAllInBox(pos(0, -9, 0), pos(0, 10, 0));

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.SKY, p -> {
        });

        verify(access, size);

        access.setOpacity(pos(0, 10, 0), 255);
        propagator.propagateLight(center, coordsToUpdate, access, EnumSkyBlock.SKY, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testSetBlockAboveSurfaceAndRelightColumn() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                withOpaque(posRange(pos(-30, -1, -30), pos(30, -1, 30))).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos testBottom = pos(0, 0, 0);
        BlockPos testTop = pos(0, 1, 0);

        LightPropagator propagator = new LightPropagator();

        access.setOpacity(testTop, 255);
        propagator.propagateLight(center, BlockPos.getAllInBox(testBottom, testTop), access, EnumSkyBlock.SKY, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testSetBlockOnSurface() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                withOpaque(posRange(pos(-30, 0, -30), pos(30, 0, 30))).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toSet = pos(0, 1, 0);

        LightPropagator propagator = new LightPropagator();

        access.setOpacity(toSet, 255);
        propagator.propagateLight(center, BlockPos.getAllInBox(toSet, toSet), access, EnumSkyBlock.SKY, p -> {
        });

        verify(access, size);
    }

    @Test(timeout = 10000)
    public void testWorksAfterCrash() {
        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toUpdate = pos(0, 0, 0);

        access.setOpacity(toUpdate, 255);

        LightPropagator propagator = new LightPropagator();

        try {
            propagator.propagateLight(center.add(500, 500, 500), BlockPos.getAllInBox(toUpdate, toUpdate), access, EnumSkyBlock.SKY, p -> {
            });
            fail();
        } catch (ReportedException ex) {
            //expected
        }
        propagator.propagateLight(center, BlockPos.getAllInBox(toUpdate, toUpdate), access, EnumSkyBlock.SKY, p -> {
        });
        //success if no crash
    }


    /*
     * Tests for spreading into existing unspread light sources
     */

    @Test
    public void testEmitFullLightAroundSources() {

        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withFullBlockLight(posRange(pos(-1, -1, -1), pos(1, 1, 1))).
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toUpdate = pos(0, 0, 0);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toUpdate, toUpdate), access, EnumSkyBlock.BLOCK, p -> {
        });

        assertThat(access, hasBlockLightValue(15, in(block(0, 0, 0))));
        assertThat(access, hasBlockLightValue(0,
                in(section(pos(-10, -10, -10), pos(10, 10, 10)))
                        .except(block(0, 0, 0))
        ));
    }

    @Test
    public void testEmitLightOnSourcesEdge() {

        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withBlockLight(2, posRange(pos(-1, -1, -1), pos(1, 1, 1))). // use value 2 because it's manual test
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toUpdate = pos(1, 0, 0);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toUpdate, toUpdate), access, EnumSkyBlock.BLOCK, p -> {
        });

        LightingMatchers.PosSection untouchedSection = section(pos(-1, -1, -1), pos(1, 1, 1))
                .except(block(1, 0, 0));

        assertThat(access, hasBlockLightValue(2, in(block(1, 0, 0))));
        assertThat(access, hasBlockLightValue(1, in(LightingMatchers.PosSection.neighborsOf(1, 0, 0).except(untouchedSection))));
        assertThat(access, hasBlockLightValue(0, in(untouchedSection)));
    }

    @Test
    public void testEmitFullLightInLowerSourceArea() {

        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withBlockLight(8, posRange(pos(-1, -1, -1), pos(1, 1, 1))).
                withFullBlockLight(pos(0, 0, 0)).
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toUpdate = pos(0, 0, 0);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toUpdate, toUpdate), access, EnumSkyBlock.BLOCK, p -> {
        });

        verify(access, size);
    }

    @Test
    public void testEmitFullLightSpreadMatchingSourceArea() {

        int size = 20;
        TestLightBlockAccessImpl access = lightAccess(size).
                currentHeightsForInitSkyLight().
                withBlockLight(14, posRange(pos(-1, -1, -1), pos(1, 1, 1))).
                withFullBlockLight(pos(0, 0, 0)).
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toUpdate = pos(0, 0, 0);

        LightPropagator propagator = new LightPropagator();
        propagator.propagateLight(center, BlockPos.getAllInBox(toUpdate, toUpdate), access, EnumSkyBlock.BLOCK, p -> {
        });

        assertThat(access, hasBlockLightValue(15, in(block(0, 0, 0))));
        assertThat(access, hasBlockLightValue(0,
                in(section(pos(-10, -10, -10), pos(10, 10, 10)))
                        .except(block(0, 0, 0))
        ));
    }

    @Test(timeout = 15000)
    public void testUnlightSolidBlockInHugeLitArea() {
        int size = 21;
        TestLightBlockAccessImpl access = lightAccess(size, size, size, size).
                withOpaque(posRange(pos(-size, -size, -size), pos(size, -size, size))).
                currentHeightsForInitSkyLight().
                make();
        BlockPos center = pos(0, 0, 0);
        BlockPos toUpdate = pos(0, 0, 0);

        access.setOpacity(toUpdate, 255);

        LightPropagator propagator = new LightPropagator();

        propagator.propagateLight(center, BlockPos.getAllInBox(toUpdate, toUpdate), access, EnumSkyBlock.SKY, p -> {
        });
        assertThat(access, hasSkyLightValue(0, in(block(toUpdate))));
        assertThat(access, hasSkyLightValue(15, in(section(
                pos(-20, -20, -20),
                pos(20, 20, 20)
        ).except(block(toUpdate)))));
    }

    // utils

    /**
     * Data format:
     * <pre>
     * new int[][][] {
     *     //x/z slice at max Y
     *     {
     *          {0, 1, 2, 3, 4},//rows with increasing X
     *          {0, 1, 2, 3, 4},//Z increases as you go down
     *          {0, 1, 2, 3, 4},
     *          {0, 1, 2, 3, 4},
     *          {0, 1, 2, 3, 4}
     *     },
     *     //x/z slice at min Y
     *     {
     *          {0, 1, 2, 3, 4},//X rows
     *          {0, 1, 2, 3, 4},
     *          {0, 1, 2, 3, 4},
     *          {0, 1, 2, 3, 4},
     *          {0, 1, 2, 3, 4}
     *     }
     * }
     * </pre>
     */
    private void verifyEqual(BlockPos start, ILightBlockAccess access, EnumSkyBlock type, int[/*max-y*/][/*z*/][/*x*/] data) {
        BlockPos end = start.add(data[0][0].length - 1, data.length - 1, data[0].length - 1);
        BlockPos.getAllInBox(start, end).forEach(p -> {
            Vec3i diff = p.subtract(start);
            //subtract from length so that as we go down we decrease y (go down)
            int expected = data[data.length - diff.getY() - 1][diff.getZ()][diff.getX()];
            int got = access.getLightFor(type, p);
            assertEquals("Wrong light value at " + p + " arrayPos=" + (data.length - diff.getY() - 1) + ", " +
                            diff.getZ() + ", " + diff.getX() + ", diff=" + diff,
                    expected, got);
        });
    }

    private void verify(ILightBlockAccess lightAccess, int radius) {
        assertThat(lightAccess, hasCorrectLight(range(radius)));
    }
}
