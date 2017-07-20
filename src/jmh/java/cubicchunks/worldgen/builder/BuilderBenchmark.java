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
package cubicchunks.worldgen.builder;

import com.google.common.collect.AbstractIterator;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Iterator;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@State(Scope.Thread)
public class BuilderBenchmark {

    private final ScalingForEachVanillaNoGradientNoCoords vanillaBaselineNoCoords = new ScalingForEachVanillaNoGradientNoCoords();
    private final ScalingForEachVanillaNoGradient vanillaBaseline = new ScalingForEachVanillaNoGradient();
    private final ScalingForEachVanillaWithGradientNoOpts vanillaWithGradientsNoOpts = new ScalingForEachVanillaWithGradientNoOpts();
    private final ScalingVanillaNoEntryObject vanillaNoEntryObject = new ScalingVanillaNoEntryObject();

    private IBuilder builder;

    @Setup
    public void setup() {
        this.builder = (x, y, z) -> y;
    }

    @Benchmark
    public void scaledCurrentBig(Blackhole bh) {
        builder.forEachScaled(new Vec3i(0, 0, 0), new Vec3i(1, 1, 1), new Vec3i(16, 16, 16),
                (x, y, z, dx, dy, dz, v) -> bh.consume(x + y + z + dx + dy + dz + v));
    }

    @Benchmark
    public void scaledCurrent(Blackhole bh) {
        builder.forEachScaled(new Vec3i(0, 0, 0), new Vec3i(4, 2, 4), new Vec3i(4, 8, 4),
                (x, y, z, dx, dy, dz, v) -> bh.consume(x + y + z + dx + dy + dz + v));
    }

    @Benchmark
    public void scaledPrevious(Blackhole bh) {
        new OldScalingIterator(builder, new Vec3i(0, 0, 0), new Vec3i(16, 16, 16), new Vec3i(4, 8, 4))
                .forEachRemaining(e -> bh.consume(e));
    }

    @Benchmark
    public void scaledCurrentSmall(Blackhole bh) {
        builder.forEachScaled(new Vec3i(0, 0, 0), new Vec3i(8, 8, 8), new Vec3i(2, 2, 2),
                (x, y, z, dx, dy, dz, v) -> bh.consume(x + y + z + dx + dy + dz + v));
    }

    @Benchmark
    public void unscaled(Blackhole bh) {
        iterator(builder, new Vec3i(0, 0, 0), new Vec3i(16, 16, 16))
                .forEachRemaining(v -> bh.consume(v));
    }

    private Iterator<OldScalingIterator.IEntry> iterator(IBuilder builder, Vec3i start, Vec3i end) {
        return new AbstractIterator<OldScalingIterator.IEntry>() {
            Iterator<BlockPos> posIt = BlockPos.getAllInBox(new BlockPos(start), new BlockPos(end)).iterator();

            @Override protected OldScalingIterator.IEntry computeNext() {
                BlockPos p = posIt.hasNext() ? posIt.next() : null;
                if (p == null) {
                    endOfData();
                    return null;
                }
                return new OldScalingIterator.ImmutbleEntry(p.getX(), p.getY(), p.getZ(), builder.get(p.getX(), p.getY(), p.getZ()));
            }
        };
    }

    @Benchmark
    public void vanillaBaselineNoNoEntryObject(Blackhole bh) {
        vanillaNoEntryObject.forEach(v -> bh.consume(v));
    }

    @Benchmark
    public void vanillaBaselineNoCoords(Blackhole bh) {
        vanillaBaselineNoCoords.forEach(v -> bh.consume(v));
    }

    @Benchmark
    public void vanillaBaseline(Blackhole bh) {
        vanillaBaseline.forEach(v -> bh.consume(v));
    }

    @Benchmark
    public void vanillaBaselineGradientsNoOpts(Blackhole bh) {
        vanillaWithGradientsNoOpts.forEach(v -> bh.consume(v));
    }

    @Benchmark
    public void baselineLoopOverhead(Blackhole bh) {
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                for (int k = 0; k < 16; k++) {
                    bh.consume(builder.get(i, j, k));
                }
            }
        }
    }
}
