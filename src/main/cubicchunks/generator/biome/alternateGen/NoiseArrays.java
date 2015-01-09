/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 * Copyright (c) Tall Worlds
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
 *******************************************************************************/
package cuchaz.cubicChunks.generator.biome.alternateGen;

import java.util.ArrayList;
import java.util.List;

class NoiseArrays
{
	private final List<double[][]> arrays = new ArrayList<double[][]>( 4 );

	NoiseArrays( double[][] volatility, double[][] height, double[][] temperature, double[][] rainfall )
	{
		//add 4 null elements to set them later.
		while( arrays.size() < 4 ) arrays.add( null );
		arrays.set( Type.VOLATILITY.ordinal(), volatility );
		arrays.set( Type.HEIGHT.ordinal(), height );
		arrays.set( Type.TEMPERATURE.ordinal(), temperature );
		arrays.set( Type.RAINFALL.ordinal(), rainfall );
	}

	double[][] get( Type type )
	{
		return arrays.get( type.ordinal() );
	}

	enum Type
	{
		VOLATILITY,
		HEIGHT,
		TEMPERATURE,
		RAINFALL
	}
	
}
