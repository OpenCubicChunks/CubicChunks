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

import com.google.common.collect.AbstractIterator;

import gnu.trove.function.TDoubleFunction;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import java.util.Iterator;
import java.util.function.DoublePredicate;
import java.util.function.ToIntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.util.HashCacheDoubles;
import cubicchunks.util.MathUtil;
import mcp.MethodsReturnNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IBuilder {

	DoublePredicate NEGATIVE = x -> x < 0;
	DoublePredicate POSITIVE = x -> x > 0;
	DoublePredicate NOT_NEGATIVE = x -> x >= 0;
	DoublePredicate NOT_POSITIVE = x -> x <= 0;

	double get(int x, int y, int z);

	default Iterator<IEntry> iterator(Vec3i start, Vec3i end) {

		return new AbstractIterator<IEntry>() {
			Iterator<BlockPos> posIt = BlockPos.getAllInBox(new BlockPos(start), new BlockPos(end)).iterator();

			@Override protected IEntry computeNext() {
				BlockPos p = posIt.hasNext() ? posIt.next() : null;
				if (p == null) {
					endOfData();
					return null;
				}
				return new ImmutbleEntry(p.getX(), p.getY(), p.getZ(), get(p.getX(), p.getY(), p.getZ()));
			}
		};
	}

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
		return (x, y, z) -> this.get(x, y, z)*builder.get(x, y, z);
	}

	default IBuilder mul(double c) {
		return apply(x -> x*c);
	}

	default IBuilder div(IBuilder builder) {
		return (x, y, z) -> this.get(x, y, z)/builder.get(x, y, z);
	}

	default IBuilder div(double c) {
		return apply(x -> x/c);
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
		return applyIf(predicate, x -> x*c);
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
		return applyIf(predicate, x -> x/c);
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

	default Iterator<IExtendedEntry> scaledIterator(Vec3i start, Vec3i end, Vec3i scale) {
		return new ScalingLerpIBuilderIterator(IBuilder.this, start, end, scale);
	}

	interface IEntry {
		int getX();

		int getY();

		int getZ();

		double getValue();
	}

	interface IExtendedEntry extends IEntry {
		double getXGradient();

		double getYGradient();

		double getZGradient();
	}

	class ImmutbleEntry implements IEntry {

		private final int x;
		private final int y;
		private final int z;
		private final double value;

		public ImmutbleEntry(int x, int y, int z, double value) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.value = value;
		}

		@Override public int getX() {
			return x;
		}

		@Override public int getY() {
			return y;
		}

		@Override public int getZ() {
			return z;
		}

		@Override public double getValue() {
			return value;
		}
	}

	class ImmutbleExtendedEntry extends ImmutbleEntry implements IExtendedEntry {

		private final double gradX;
		private final double gradY;
		private final double gradZ;

		public ImmutbleExtendedEntry(int x, int y, int z, double value, double gradX, double gradY, double gradZ) {
			super(x, y, z, value);
			this.gradX = gradX;
			this.gradY = gradY;
			this.gradZ = gradZ;
		}

		@Override public double getXGradient() {
			return gradX;
		}

		@Override public double getYGradient() {
			return gradY;
		}

		@Override public double getZGradient() {
			return gradZ;
		}
	}

}
