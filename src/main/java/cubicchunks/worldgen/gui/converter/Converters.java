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
package cubicchunks.worldgen.gui.converter;

import com.google.common.base.Converter;
import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

public class Converters {

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		protected Converter<Float, Float> currentConverter = Converter.identity();

		public ExponentialBuilder exponential() {
			return new ExponentialBuilder(self());
		}

		public InfinityBuilder withInfinity() {
			return new InfinityBuilder(self());
		}

		public Builder inverse() {
			return composeWithSelf(new InverseConverter());
		}

		public Builder reverse() {
			return composeWithSelf(new ReverseConverter());
		}

		public Builder scale(float scale) {
			return composeWithSelf(new ScaleConverter(scale));
		}

		public Builder offset(float offset) {
			return composeWithSelf(new OffsetConverter(offset));
		}

		public Builder linearScale(float min, float max) {
			return scale(max - min).offset(min);
		}

		public RoundingBuilder rounding() {
			return new RoundingBuilder(self());
		}

		public Converter<Float, Float> build() {
			return self().currentConverter;
		}

		/**
		 * This special method allows to do something like this:
		 * <p>
		 * builder().someConverter().setSomethingInSomeConverter(a).otherConverter().yetAnotherConverter().build();
		 * <p>
		 * someConverter() returns SomeConverterBuilder, which extends the normal Builder. When otherConverter is
		 * called, which won't be implemented in SomeConverterBuilder and the implementation in Builder calls self()
		 * instead of accessing this(). self() in SomeConverterBuilder can then finish any work in progress building and
		 * return the Builder it wrapped.
		 */
		protected Builder self() {
			return this;
		}

		protected final Builder composeWithSelf(Converter<Float, Float> conv) {
			Builder self = self();
			self.currentConverter = compose(conv, self.currentConverter);
			return self;
		}
	}

	public static class ExponentialBuilder extends Builder {
		private Builder baseSelf;

		boolean hasZero = false;
		float minExpPos = Float.NaN;
		float maxExpPos = Float.NaN;

		float minExpNeg = Float.NaN;
		float maxExpNeg = Float.NaN;
		float baseVal;

		public ExponentialBuilder(Builder baseSelf) {
			this.baseSelf = baseSelf;
		}

		public ExponentialBuilder withZero() {
			ExponentialBuilder self = exponentialSelf();
			self.hasZero = true;
			return self;
		}

		public ExponentialBuilder withPositiveExponentRange(float min, float max) {
			ExponentialBuilder self = exponentialSelf();
			self.minExpPos = min;
			self.maxExpPos = max;
			return self;
		}

		public ExponentialBuilder withNegativeExponentRange(float min, float max) {
			ExponentialBuilder self = exponentialSelf();
			self.minExpNeg = min;
			self.maxExpNeg = max;
			return self;
		}

		public ExponentialBuilder withBaseValue(float baseVal) {
			ExponentialBuilder self = exponentialSelf();
			self.baseVal = baseVal;
			return self;
		}

		protected ExponentialBuilder exponentialSelf() {
			return this;
		}

		@Override
		protected Builder self() {
			baseSelf.currentConverter = compose(new ExponentialConverter(this), baseSelf.currentConverter);
			return baseSelf;
		}
	}

	public static class InfinityBuilder extends Builder {
		private Builder baseSelf;

		private float negInf = 0, posInf = 1;

		public InfinityBuilder(Builder baseSelf) {
			this.baseSelf = baseSelf;
		}

		public InfinityBuilder negativeAt(float negInf) {
			InfinityBuilder self = infinitySelf();
			self.negInf = negInf;
			return self;
		}

		public InfinityBuilder positiveAt(float posInf) {
			InfinityBuilder self = infinitySelf();
			self.posInf = posInf;
			return self;
		}

		protected InfinityBuilder infinitySelf() {
			return this;
		}

		@Override
		protected Builder self() {
			baseSelf.currentConverter = compose(new ConverterWithInfinity(negInf, posInf), baseSelf.currentConverter);
			return baseSelf;
		}
	}

	public static class RoundingBuilder extends Builder {
		private Builder baseSelf;

		float maxExp = Float.NaN;
		Set<RoundingConverter.RoundingEntry> roundingData = new HashSet<>();
		DoubleUnaryOperator snapRadius;

		public RoundingBuilder(Builder baseSelf) {
			this.baseSelf = baseSelf;
		}

		public RoundingBuilder withMaxExp(float max) {
			RoundingBuilder self = roundingSelf();
			self.maxExp = max;
			return self;
		}

		public RoundingBuilder withRoundingRadius(DoubleUnaryOperator op) {
			Preconditions.checkNotNull(op);
			RoundingBuilder self = roundingSelf();
			self.snapRadius = op;
			return self;
		}

		public RoundingBuilder withBase(float baseVal, float multiplier) {
			RoundingBuilder self = roundingSelf();
			self.roundingData.add(new RoundingConverter.RoundingEntry(baseVal, multiplier));
			return self;
		}

		protected RoundingBuilder roundingSelf() {
			return this;
		}

		@Override
		protected Builder self() {
			baseSelf.currentConverter = compose(new RoundingConverter(this), baseSelf.currentConverter);
			return baseSelf;
		}
	}

	public static <IN, X, OUT> Converter<IN, OUT> compose(Converter<X, OUT> f, Converter<IN, X> g) {
		return Converter.from(
			x -> f.convert(g.convert(x)),
			x -> g.reverse().convert(f.reverse().convert(x))
		);
	}
}
