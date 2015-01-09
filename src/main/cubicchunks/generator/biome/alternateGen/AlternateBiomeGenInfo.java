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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cuchaz.cubicChunks.generator.biome.alternateGen;

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;

/**
 *
 * @author Barteks2x
 */
public class AlternateBiomeGenInfo
{
	public final float minVolatility;
	public final float maxVolatility;
	public final float minHeight;
	public final float maxHeight;
	public final float minTemp;
	public final float maxTemp;
	public final float minRainfall;
	public final float maxRainfall;
	public final boolean extendedHeightVolatilityChecks;
	public final CubeBiomeGenBase biome;
	public final float rarity;
	public final float size;
	public final String name;

	public static AlternateBiomeGenInfoBuilder builder()
	{
		return new AlternateBiomeGenInfoBuilder();
	}

	private AlternateBiomeGenInfo( float minVol, float maxVol, float minHeight, float maxHeight, float minTemp, float maxTemp, float minRainfall, float maxRainfall, float rarity, float size, boolean doExtHV, CubeBiomeGenBase biome, String name )
	{
		this.minVolatility = minVol;
		this.maxVolatility = maxVol;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.minTemp = minTemp;
		this.maxTemp = maxTemp;
		this.minRainfall = minRainfall;
		this.maxRainfall = maxRainfall;
		this.rarity = rarity;
		this.size = size;
		this.extendedHeightVolatilityChecks = doExtHV;
		this.biome = biome;
		this.name = name;
	}

	public AlternateBiomeGenInfo( AlternateBiomeGenInfo i )
	{
		this( i.minVolatility, i.maxVolatility, i.minHeight, i.maxHeight, i.minTemp, i.maxTemp, i.minRainfall, i.maxRainfall, i.rarity, i.size, i.extendedHeightVolatilityChecks, i.biome, i.name );
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static class AlternateBiomeGenInfoBuilder
	{
		private Float minH, minV, minT, minR;
		private Float maxH, maxV, maxT, maxR;
		private Float size, rarity;
		private Boolean extHV;
		private String name;
		private CubeBiomeGenBase biome;

		private AlternateBiomeGenInfoBuilder()
		{
		}

		public AlternateBiomeGenInfoBuilder setH( float min, float max )
		{
			minH = min;
			maxH = max;
			return this;
		}

		public AlternateBiomeGenInfoBuilder setV( float min, float max )
		{
			minV = min;
			maxV = max;
			return this;
		}

		public AlternateBiomeGenInfoBuilder setT( float min, float max )
		{
			minT = min;
			maxT = max;
			return this;
		}

		public AlternateBiomeGenInfoBuilder setR( float min, float max )
		{
			minR = min;
			maxR = max;
			return this;
		}

		public AlternateBiomeGenInfoBuilder setSizeRarity( float size, float rarity )
		{
			this.size = size;
			this.rarity = rarity;
			return this;
		}

		public AlternateBiomeGenInfoBuilder setExtHV( boolean val )
		{
			extHV = val;
			return this;
		}

		public AlternateBiomeGenInfoBuilder setName( String name )
		{
			this.name = name;
			return this;
		}

		public AlternateBiomeGenInfoBuilder setBiome( CubeBiomeGenBase color )
		{
			this.biome = color;
			return this;
		}

		public AlternateBiomeGenInfo build()
		{
			checkNotNull( minH, "minH" );
			//values are set in pairs
			assert maxH != null;

			checkNotNull( minV, "minV" );
			assert maxV != null;

			checkNotNull( minT, "minT" );
			assert maxT != null;

			checkNotNull( minR, "minR" );
			assert maxR != null;

			checkNotNull( name, "biomeName" );
			checkNotNull( biome, "biome" );

			if( size == null )
			{
				size = 1.0F;
			}

			if( rarity == null )
			{
				rarity = 0.0F;
			}

			if( extHV == null )
			{
				extHV = true;
			}

			if( minV >= maxV )
			{
				throw new IllegalArgumentException( String.format( "minVol >= maxVol, biome: %s", name ) );
			}
			if( minH >= maxH )
			{
				throw new IllegalArgumentException( String.format( "minHeight >= maxHeight, biome: %s", name ) );
			}
			if( minT >= maxT )
			{
				throw new IllegalArgumentException( String.format( "minTemp >= maxTemp, biome: %s", name ) );
			}
			if( minR >= maxR )
			{
				throw new IllegalArgumentException( String.format( "minRainfall >= maxRainfall, biome: %s", name ) );
			}
			if( minV >= Math.max( Math.abs( maxH ), Math.abs( minH ) ) )
			{
				throw new IllegalArgumentException( String.format( "incorrect voaltility! Biome: %s", name ) );
			}

			return new AlternateBiomeGenInfo( minV, maxV, minH, maxH, minT, maxT, minR, maxR, rarity, size, extHV, biome, name );
		}

		//this should build muatted biome taking normal biome as parameter (should choose biome with id += 128)
		//in biome preview code there is no CubeBiomeGenBase class
		public AlternateBiomeGenInfo buildMutated()
		{
			checkNotNull( minH, "minH" );
			//values are set in pairs
			assert maxH != null;

			checkNotNull( minV, "minV" );
			assert maxV != null;

			checkNotNull( minT, "minT" );
			assert maxT != null;

			checkNotNull( minR, "minR" );
			assert maxR != null;

			checkNotNull( name, "biomeName" );
			checkNotNull( biome, "biome" );

			if( size == null )
			{
				size = 1.0F;
			}

			if( rarity == null )
			{
				rarity = 0.0F;
			}

			if( extHV == null )
			{
				extHV = true;
			}

			if( minV >= maxV )
			{
				throw new IllegalArgumentException( String.format( "minVol >= maxVol, biome: %s", name ) );
			}
			if( minH >= maxH )
			{
				throw new IllegalArgumentException( String.format( "minHeight >= maxHeight, biome: %s", name ) );
			}
			if( minT >= maxT )
			{
				throw new IllegalArgumentException( String.format( "minTemp >= maxTemp, biome: %s", name ) );
			}
			if( minR >= maxR )
			{
				throw new IllegalArgumentException( String.format( "minRainfall >= maxRainfall, biome: %s", name ) );
			}
			if( minV >= Math.max( Math.abs( maxH ), Math.abs( minH ) ) )
			{
				throw new IllegalArgumentException( String.format( "incorrect voaltility! Biome: %s", name ) );
			}

			return new AlternateBiomeGenInfo( minV, maxV, minH, maxH, minT, maxT, minR, maxR, rarity, size, extHV, (CubeBiomeGenBase)CubeBiomeGenBase.getBiomeGenArray()[biome.biomeID + 128], name );
		}

		public AlternateBiomeGenInfoBuilder setBaseInfo( AlternateBiomeGenInfo i )
		{
			this.minH = i.minHeight;
			this.maxH = i.maxHeight;

			this.minV = i.minVolatility;
			this.maxV = i.maxVolatility;

			this.minT = i.minTemp;
			this.maxT = i.maxTemp;

			this.minR = i.minRainfall;
			this.maxR = i.maxRainfall;

			this.size = i.size;
			this.rarity = i.rarity;

			this.biome = i.biome;
			this.name = i.name;

			return this;
		}

		private void checkNotNull( Object obj, String fieldName )
		{
			if( obj == null )
			{
				throw new BiomeGenInfoUnfinishedBuildingException( fieldName );
			}
		}

		private class BiomeGenInfoUnfinishedBuildingException extends RuntimeException
		{
			private static final long serialVersionUID = 1L;

			public BiomeGenInfoUnfinishedBuildingException( String fieldName )
			{
				super( String.format( "BiomeGenInfo building exception: value not set (%s)", fieldName ) );
			}
		}
	}
}
