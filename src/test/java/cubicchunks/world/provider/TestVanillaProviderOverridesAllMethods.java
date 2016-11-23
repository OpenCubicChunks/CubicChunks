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
package cubicchunks.world.provider;

import com.google.common.collect.Sets;

import net.minecraft.world.WorldProvider;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import mcp.MethodsReturnNonnullByDefault;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestVanillaProviderOverridesAllMethods {
	@Test public void test() {
		Set<Method> implemented = Sets.newHashSet(VanillaCubicProvider.class.getDeclaredMethods());
		Set<Method> toImplement = Sets.newHashSet(WorldProvider.class.getDeclaredMethods());

		Set<Method> unimplemented = new HashSet<>();
		scanning:
		for (Method toCheck : toImplement) {
			if (!isOverridable(toCheck)) {
				continue;
			}
			for (Method toCompare : implemented) {
				if (areEqual(toCheck, toCompare)) {
					continue scanning;
				}
			}
			unimplemented.add(toCheck);
		}
		assertThat(unimplemented, is(empty()));
	}

	private boolean areEqual(Method toCheck, Method toCompare) {
		if (!toCheck.getName().equals(toCompare.getName())) {
			return false;
		}
		Class<?>[] args1 = toCheck.getParameterTypes();
		Class<?>[] args2 = toCompare.getParameterTypes();
		if (args1.length != args2.length) {
			return false;
		}
		for (int i = 0; i < args1.length; i++) {
			if (args1[i] != args2[i]) {
				return false;
			}
		}
		return true;
	}

	private boolean isOverridable(Method toCheck) {
		int mod = toCheck.getModifiers();
		return !Modifier.isFinal(mod) &&
			!Modifier.isStatic(mod) &&
			!Modifier.isPrivate(mod);
	}
}
