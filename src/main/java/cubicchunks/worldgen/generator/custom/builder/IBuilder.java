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
package cubicchunks.worldgen.generator.custom.builder;

import cubicchunks.util.MathUtil;
import cubicchunks.util.cache.HashCacheDoubles;
import gnu.trove.function.TDoubleFunction;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import java.util.function.DoublePredicate;
import java.util.function.ToIntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IBuilder {

    DoublePredicate NEGATIVE = x -> x < 0;
    DoublePredicate POSITIVE = x -> x > 0;
    DoublePredicate NOT_NEGATIVE = x -> x >= 0;
    DoublePredicate NOT_POSITIVE = x -> x <= 0;

    double get(int x, int y, int z);

    default IBuilder add(IBuilder builder) {
        return (x, y, z) -> this.get(x, y, z) + builder.get(x, y, z);
    }

    default IBuilder add(double c) {
        return apply(x -> x + c);
    }

    default IBuilder sub(IBuilder builder) {
        return (x, y, z) -> this.get(x, y, z) - builder.get(x, y, z);
    }

    default IBuilder sub(double c) {
        return apply(x -> x - c);
    }

    default IBuilder mul(IBuilder builder) {
        return (x, y, z) -> this.get(x, y, z) * builder.get(x, y, z);
    }

    default IBuilder mul(double c) {
        return apply(x -> x * c);
    }

    default IBuilder div(IBuilder builder) {
        return (x, y, z) -> this.get(x, y, z) / builder.get(x, y, z);
    }

    default IBuilder div(double c) {
        return apply(x -> x / c);
    }

    default IBuilder clamp(double min, double max) {
        return apply(x -> MathHelper.clamp(x, min, max));
    }

    default IBuilder apply(TDoubleFunction func) {
        return (x, y, z) -> func.execute(this.get(x, y, z));
    }

    default IBuilder addIf(DoublePredicate predicate, IBuilder builder) {
        return (x, y, z) -> {
            double value = this.get(x, y, z);
            if (predicate.test(value)) {
                value += builder.get(x, y, z);
            }
            return value;
        };
    }

    default IBuilder addIf(DoublePredicate predicate, double c) {
        return applyIf(predicate, x -> x + c);
    }

    default IBuilder subIf(DoublePredicate predicate, IBuilder builder) {
        return (x, y, z) -> {
            double value = this.get(x, y, z);
            if (predicate.test(value)) {
                value -= builder.get(x, y, z);
            }
            return value;
        };
    }

    default IBuilder subIf(DoublePredicate predicate, double c) {
        return applyIf(predicate, x -> x - c);
    }

    default IBuilder mulIf(DoublePredicate predicate, IBuilder builder) {
        return (x, y, z) -> {
            double value = this.get(x, y, z);
            if (predicate.test(value)) {
                value *= builder.get(x, y, z);
            }
            return value;
        };
    }

    default IBuilder mulIf(DoublePredicate predicate, double c) {
        return applyIf(predicate, x -> x * c);
    }

    default IBuilder divIf(DoublePredicate predicate, IBuilder builder) {
        return (x, y, z) -> {
            double value = this.get(x, y, z);
            if (predicate.test(value)) {
                value /= builder.get(x, y, z);
            }
            return value;
        };
    }

    default IBuilder divIf(DoublePredicate predicate, double c) {
        return applyIf(predicate, x -> x / c);
    }

    default IBuilder clampIf(DoublePredicate predicate, double min, double max) {
        return apply(x ->
                predicate.test(x) ?
                        MathHelper.clamp(x, min, max) :
                        x);
    }

    default IBuilder applyIf(DoublePredicate predicate, TDoubleFunction func) {
        return (x, y, z) -> {
            double value = this.get(x, y, z);
            if (predicate.test(value)) {
                value = func.execute(value);
            }
            return value;
        };
    }

    /**
     * Combines 2 builders using current builder as selector for linear interpolation.
     * Selector 0 = low, selector 1 = high.
     * <p>
     * No clamping is done on selector value, so values exceeding range 0-1 will result in extrapolation.
     */
    default IBuilder lerp(IBuilder low, IBuilder high) {
        return (x, y, z) -> MathUtil.lerp(this.get(x, y, z), low.get(x, y, z), high.get(x, y, z));
    }

    default IBuilder cached(int cacheSize, ToIntFunction<Vec3i> hash) {
        HashCacheDoubles<Vec3i> cache = HashCacheDoubles.create(cacheSize, hash,
                v -> this.get(v.getX(), v.getY(), v.getZ()));
        return (x, y, z) -> cache.get(new Vec3i(x, y, z));
    }

    /**
     * Returns IBuilder that caches values based on x and z coordinates, ignoring Y coordinate.
     * <p>
     * This should NEVER be used if the IBuilder is intended to generate values that depend on Y coordinate
     */
    default IBuilder cached2d(int cacheSize, ToIntFunction<Vec3i> hash) {
        HashCacheDoubles<Vec3i> cache = HashCacheDoubles.create(cacheSize,
                hash,
                v -> this.get(v.getX(), v.getY(), v.getZ()));
        return (x, y, z) -> cache.get(new Vec3i(x, 0, z));
    }

    default void forEachScaled(Vec3i startUnscaled, Vec3i endUnscaled, Vec3i scale, NoiseConsumer consumer) {

        if (scale.getZ() != scale.getX()) {
            throw new UnsupportedOperationException("X and Z scale must be the same!");
        }
        final double/*[]*/[][] gradX = new double/*[scale.getX()]*/[scale.getY()][scale.getZ()];
        final double[]/*[]*/[] gradY = new double[scale.getX()]/*[scale.getY()]*/[scale.getZ()];
        final double[][]/*[]*/ gradZ = new double[scale.getX()][scale.getY()]/*[scale.getZ()]*/;
        final double[][][] vals = new double[scale.getX()][scale.getY()][scale.getZ()];

        int xScale = scale.getX();
        int yScale = scale.getY();
        int zScale = scale.getZ();

        double stepX = 1.0 / xScale;
        double stepY = 1.0 / yScale;
        double stepZ = 1.0 / zScale;

        int minX = startUnscaled.getX();
        int minY = startUnscaled.getY();
        int minZ = startUnscaled.getZ();
        int maxX = endUnscaled.getX();
        int maxY = endUnscaled.getY();
        int maxZ = endUnscaled.getZ();
        for (int sectionX = minX; sectionX < maxX; ++sectionX) {
            int x = sectionX * xScale;
            for (int sectionZ = minZ; sectionZ < maxZ; ++sectionZ) {
                int z = sectionZ * zScale;
                for (int sectionY = minY; sectionY < maxY; ++sectionY) {
                    int y = sectionY * yScale;

                    final double v000 = this.get(x + xScale * 0, y + yScale * 0, z + zScale * 0);
                    final double v001 = this.get(x + xScale * 0, y + yScale * 0, z + zScale * 1);
                    final double v010 = this.get(x + xScale * 0, y + yScale * 1, z + zScale * 0);
                    final double v011 = this.get(x + xScale * 0, y + yScale * 1, z + zScale * 1);
                    final double v100 = this.get(x + xScale * 1, y + yScale * 0, z + zScale * 0);
                    final double v101 = this.get(x + xScale * 1, y + yScale * 0, z + zScale * 1);
                    final double v110 = this.get(x + xScale * 1, y + yScale * 1, z + zScale * 0);
                    final double v111 = this.get(x + xScale * 1, y + yScale * 1, z + zScale * 1);

                    double v0y0 = v000;
                    double v0y1 = v001;
                    double v1y0 = v100;
                    double v1y1 = v101;
                    final double d_dy__0y0 = (v010 - v000) * stepY;
                    final double d_dy__0y1 = (v011 - v001) * stepY;
                    final double d_dy__1y0 = (v110 - v100) * stepY;
                    final double d_dy__1y1 = (v111 - v101) * stepY;

                    for (int yRel = 0; yRel < yScale; ++yRel) {
                        double vxy0 = v0y0;
                        double vxy1 = v0y1;
                        final double d_dx__xy0 = (v1y0 - v0y0) * stepX;
                        final double d_dx__xy1 = (v1y1 - v0y1) * stepX;

                        // gradients start
                        double v0yz = v0y0;
                        double v1yz = v1y0;

                        final double d_dz__0yz = (v0y1 - v0y0) * stepX;
                        final double d_dz__1yz = (v1y1 - v1y0) * stepX;
                        // gradients end

                        for (int xRel = 0; xRel < xScale; ++xRel) {
                            final double d_dz__xyz = (vxy1 - vxy0) * stepZ;
                            double vxyz = vxy0;

                            // gradients start
                            final double d_dx__xyz = (v1yz - v0yz) * stepZ;
                            gradX[yRel][xRel] = d_dx__xyz; // for this one x and z are swapped
                            gradZ[xRel][yRel] = d_dz__xyz;
                            // gradients end
                            for (int zRel = 0; zRel < zScale; ++zRel) {
                                // to get gradients working, consumer usage moved to later
                                vals[xRel][yRel][zRel] = vxyz;
                                vxyz += d_dz__xyz;
                            }

                            vxy0 += d_dx__xy0;
                            vxy1 += d_dx__xy1;
                            // gradients start
                            v0yz += d_dz__0yz;
                            v1yz += d_dz__1yz;
                            // gradients end
                        }

                        v0y0 += d_dy__0y0;
                        v0y1 += d_dy__0y1;
                        v1y0 += d_dy__1y0;
                        v1y1 += d_dy__1y1;

                    }
                    // gradients start
                    double v00z = v000;
                    double v01z = v010;
                    double v10z = v100;
                    double v11z = v110;

                    final double d_dz__00z = (v001 - v000) * stepZ;
                    final double d_dz__01z = (v011 - v010) * stepZ;
                    final double d_dz__10z = (v101 - v100) * stepZ;
                    final double d_dz__11z = (v111 - v110) * stepZ;

                    for (int zRel = 0; zRel < zScale; ++zRel) {

                        double vx0z = v00z;
                        double vx1z = v01z;

                        final double d_dx__x0z = (v10z - v00z) * stepX;
                        final double d_dx__x1z = (v11z - v01z) * stepX;

                        for (int xRel = 0; xRel < xScale; ++xRel) {

                            double d_dy__xyz = (vx1z - vx0z) * stepY;

                            gradY[xRel][zRel] = d_dy__xyz;

                            vx0z += d_dx__x0z;
                            vx1z += d_dx__x1z;
                        }
                        v00z += d_dz__00z;
                        v01z += d_dz__01z;
                        v10z += d_dz__10z;
                        v11z += d_dz__11z;
                    }

                    for (int xRel = 0; xRel < xScale; ++xRel) {
                        for (int zRel = 0; zRel < zScale; ++zRel) {
                            for (int yRel = 0; yRel < yScale; ++yRel) {
                                double vxyz = vals[xRel][yRel][zRel];
                                double d_dx__xyz = gradX[yRel][zRel];
                                double d_dy__xyz = gradY[xRel][zRel];
                                double d_dz__xyz = gradZ[xRel][yRel];
                                consumer.accept(x + xRel, y + yRel, z + zRel, d_dx__xyz, d_dy__xyz, d_dz__xyz, vxyz);
                            }
                        }
                    }
                    // gradients end
                }
            }
        }
    }
}
