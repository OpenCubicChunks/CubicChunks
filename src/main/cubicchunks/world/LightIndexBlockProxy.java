/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
package cubicchunks.world;

import java.util.TreeMap;

import net.minecraft.block.BlockAir;

@Deprecated
public class LightIndexBlockProxy extends BlockAir {
	
	private static TreeMap<Integer,LightIndexBlockProxy> cache;
	
	static {
		cache = new TreeMap<Integer,LightIndexBlockProxy>();
	}
	
	public static LightIndexBlockProxy get(int opacity) {
		LightIndexBlockProxy proxy = cache.get(opacity);
		if (proxy == null) {
			proxy = new LightIndexBlockProxy(opacity);
			cache.put(opacity, proxy);
		}
		return proxy;
	}
	
	private int opacity;
	
	protected LightIndexBlockProxy(int opacity) {
		this.opacity = opacity;
	}
	
	@Override
	public int getOpacity() {
		return this.opacity;
	}
	
	@Override
	public int getBrightness() {
		// assume lights in unloaded cubes are turned off
		return 0;
	}
	
	@Override
	public boolean isOpaque() {
		return this.opacity == 255;
	}
}