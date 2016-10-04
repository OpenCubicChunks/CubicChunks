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
/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package cubicchunks.launch;

import com.google.common.io.Resources;
import cubicchunks.asm.CoreModLoadingPlugin;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.net.URL;
import java.util.List;

//copied from SpongeCommon https://github.com/SpongePowered/SpongeCommon/blob/d6aad9553c33f12014f3b126d5c7475cda5eb46c/src/test/java/org/spongepowered/common/launch/TestTweaker.java
//made the class abstract to allow client/server subclasses, and added server and client inner classes
public abstract class TestTweaker implements ITweaker {

	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {

	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader loader) {
		// Register access transformer
		Launch.blackboard.put("at", new URL[]{Resources.getResource("META-INF/cubicchunks_at.cfg")});
		loader.registerTransformer("cubicchunks.launch.transformer.AccessTransformer");

		// JUnit attempts to lookup the @Test annotation so we need to make sure the classes are loaded
		// using the same class loader (the main class loader)
		loader.addClassLoaderExclusion("org.junit.");
		loader.addClassLoaderExclusion("org.hamcrest.");

		CoreModLoadingPlugin.initMixin();
		MixinEnvironment.getDefaultEnvironment().setSide(getDefaultSide());
	}

	@Override
	public String getLaunchTarget() {
		return "cubicchunks.launch.TestMain";
	}

	@Override
	public String[] getLaunchArguments() {
		return new String[0];
	}

	public abstract MixinEnvironment.Side getDefaultSide();

	public static class Client extends TestTweaker {
		@Override public MixinEnvironment.Side getDefaultSide() {
			return MixinEnvironment.Side.CLIENT;
		}
	}

	public static class Server extends TestTweaker {
		@Override public MixinEnvironment.Side getDefaultSide() {
			return MixinEnvironment.Side.SERVER;
		}
	}
}