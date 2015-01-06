/*******************************************************************************
 * Copyright (c) 2014 Bartosz Skrzypczak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bartosz Skrzypczak - initial implementation and adaptation from vanilla.
 ******************************************************************************/
package main.java.cubicchunks.generator.features;

import java.util.Random;

import main.java.cubicchunks.world.Cube;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import static net.minecraft.world.EnumSkyBlock.Block;
import net.minecraft.world.World;

public class CubicRavineGen extends CubicMapGenBase
{
	//I'm not sure what it is
	private float[] array1 = new float[ 1024 ];

	protected void generateRavine( long seed, Cube cube, int xOrigin, int yOrigin, int zOrigin, double x, double y, double z, float size_base, float curve, float angle, int numTry, int tries, double yModSinMultiplier )
	{
		Random rand = new Random( seed );
		double xOCenter = xOrigin * 16 + 8;
		double yOCenter = yOrigin * 16 + 8;
		double zOCenter = zOrigin * 16 + 8;
		float f3 = 0.0F;
		float f4 = 0.0F;

		if ( tries <= 0 )
		{
			int radius = m_range * 16 - 16;
			tries = radius - rand.nextInt( radius / 4 );
		}

		boolean kAltered = false;

		if ( numTry == -1 )
		{
			numTry = tries / 2;
			kAltered = true;
		}

		float temp1 = 1.0F;

		for ( int i = 0; i < 256; ++i )
		{
			if ( i == 0 || rand.nextInt( 3 ) == 0 )
			{
				temp1 = 1.0F + rand.nextFloat() * rand.nextFloat()/* * 1.0F*/;//1.X, lower = higher probability
			}

			this.array1[i] = temp1 * temp1;
		}

		for ( ; numTry < tries; ++numTry )
		{
			double modSin = 1.5D + ( double ) ( MathHelper.sin( ( float ) numTry * ( float ) Math.PI / ( float ) tries ) * size_base * 1.0F );
			double yModSin = modSin * yModSinMultiplier;
			modSin *= ( double ) rand.nextFloat() * 0.25D + 0.75D;//* value between 0.75 and 1
			yModSin *= ( double ) rand.nextFloat() * 0.25D + 0.75D;
			float cosAngle = MathHelper.cos( angle );
			float sinAngle = MathHelper.sin( angle );
			x += ( double ) ( MathHelper.cos( curve ) * cosAngle );
			y += ( double ) sinAngle;
			z += ( double ) ( MathHelper.sin( curve ) * cosAngle );
			angle *= 0.7F;
			angle += f4 * 0.05F;
			curve += f3 * 0.05F;
			f4 *= 0.8F;
			f3 *= 0.5F;
			f4 += ( rand.nextFloat() - rand.nextFloat() ) * rand.nextFloat() * 2.0F;
			f3 += ( rand.nextFloat() - rand.nextFloat() ) * rand.nextFloat() * 4.0F;

			if ( !( kAltered || rand.nextInt( 4 ) != 0 ) )
			{
				continue;
			}
			double xDist = x - xOCenter;
			//double yDist = y - yOCenter;
			double zDist = z - zOCenter;
			double triesLeft = ( double ) ( tries - numTry );
			double fDist = ( double ) ( size_base + 2.0F + 16.0F );

			if ( xDist * xDist + zDist * zDist - triesLeft * triesLeft > fDist * fDist )
			{
				return;
			}

			if ( !( x >= xOCenter - 16.0D - modSin * 2.0D
				&& y >= yOCenter - 16.0D - yModSin * 2.0D
				&& z >= zOCenter - 16.0D - modSin * 2.0D
				&& x <= xOCenter + 16.0D + modSin * 2.0D
				&& y <= yOCenter + 16.0D + yModSin * 2.0D
				&& z <= zOCenter + 16.0D + modSin * 2.0D ) )
			{
				continue;
			}
			int xDist1 = MathHelper.floor_double( x - modSin ) - xOrigin * 16 - 1;
			int xDist2 = MathHelper.floor_double( x + modSin ) - xOrigin * 16 + 1;
			int yDist1 = MathHelper.floor_double( y - yModSin ) - yOrigin * 16 - 1;
			int yDist2 = MathHelper.floor_double( y + yModSin ) - yOrigin * 16 + 1;
			int zDist1 = MathHelper.floor_double( z - modSin ) - zOrigin * 16 - 1;
			int zDist2 = MathHelper.floor_double( z + modSin ) - zOrigin * 16 + 1;

			if ( xDist1 < 0 )
			{
				xDist1 = 0;
			}

			if ( xDist2 > 16 )
			{
				xDist2 = 16;
			}

			if ( yDist1 < 0 )//1
			{
				yDist1 = 0;//1
			}

			if ( yDist2 > 16 )//120
			{
				yDist2 = 16;//120
			}

			if ( zDist1 < 0 )
			{
				zDist1 = 0;
			}

			if ( zDist2 > 16 )
			{
				zDist2 = 16;
			}

			boolean hitwater = false;
			//int x1;
			//int temp;

			for ( int x1 = xDist1; !hitwater && x1 < xDist2; ++x1 )
			{
				for ( int z1 = zDist1; !hitwater && z1 < zDist2; ++z1 )
				{
					for ( int y1 = yDist2 - 1; !hitwater && y1 >= yDist1; --y1 )
					{
						//temp = ( x1 * 16 + z1 ) * 16 /*128*/ + y1;
						Block block = cube.getBlock( x1, y1, z1 );

						if ( y1 < 0 || y1 >= 16 )//128
						{
							continue;
						}
						if ( block == Blocks.flowing_water || block == Blocks.water )
						{
							hitwater = true;
						}

						if ( y1 != yDist1 - 1
							&& x1 != xDist1
							&& x1 != xDist2 - 1
							&& z1 != zDist1
							&& z1 != zDist2 - 1 )
						{
							y1 = yDist1;
						}

					}
				}
			}

			if ( hitwater )
			{
				continue;
			}
			for ( int x1 = xDist1; x1 < xDist2; ++x1 )
			{
				double distX = ( ( double ) ( x1 + xOrigin * 16 ) + 0.5D - x ) / modSin;

				for ( int z1 = zDist1; z1 < zDist2; ++z1 )
				{
					double distZ = ( ( double ) ( z1 + zOrigin * 16 ) + 0.5D - z ) / modSin;
					//int aIndex = ( x1 * 16 + z1 ) * 16 /*128*/ + yDist2;
					boolean grass = false;

					if ( distX * distX + distZ * distZ >= 1.0D )
					{
						continue;
					}
					for ( int y1 = yDist1; y1 < yDist2; ++y1 )
					{
						double distY = ( ( double ) y1 + yOrigin * 16 + 0.5D - y ) / yModSin;

						if ( ( distX * distX + distZ * distZ ) * ( double ) this.array1[( y1 + yOrigin * 16 ) & 0xFF] + distY * distY / 6.0D >= 1.0D )
						{
							continue;
						}
						//byte blockID = abyte[aIndex];
						Block block = cube.getBlock( x1, y1, z1 );
						if ( block == Blocks.grass )
						{
							grass = true;
						}

						if ( block != Blocks.stone && block != Blocks.dirt && block != Blocks.grass )
						{
							continue;
						}
						if ( y1 + yOrigin * 16 < /*10*/ 0 ) //used to place lava at the bottom of ravines if it was deep enough
						{
							cube.setBlockForGeneration( x1, y1, z1, /*Blocks.flowing_lava*/ Blocks.air );//BUG: crash when it's lava
						} 
						else
						{
							cube.setBlockForGeneration( x1, y1, z1, Blocks.air );

							/*if ( grass && blocks.getBlock( x1, y1 - 1, z1) == Blocks.dirt )
							 {
							 blocks.setBlock( x1, y1 - 1, z1, m_world.getBiomeGenForCoords( x1 + xOrigin * 16, z1 + zOrigin * 16 ).topBlock);//not needed yet
							 }*/
						}
					}

				}
			}

			if ( kAltered )
			{
				break;
			}
		}
	}

	@Override
	protected void generate( World world, Cube cube, int chunkX, int chunkY, int chunkZ, int xOrigin, int yOrigin, int zOrigin )
	{
		if ( m_rand.nextInt( 16 ) != 0 )
		{
			return;
		}
		if ( chunkY <= 4 && m_rand.nextInt( 50 ) == 0 )
		{
			double x = chunkX * 16 + m_rand.nextInt( 16 );
			double y = chunkY * 16 + m_rand.nextInt( 16 );
			double z = chunkZ * 16 + m_rand.nextInt( 16 );
			byte numGen = 1;

			for ( int i = 0; i < numGen; ++i )
			{
				float curve = m_rand.nextFloat() * ( float ) Math.PI * 2.0F;
				float angle = ( m_rand.nextFloat() - 0.5F ) * 2.0F / 8.0F;
				float f = ( m_rand.nextFloat() * 2.0F + m_rand.nextFloat() ) * 2.0F;
				this.generateRavine( m_rand.nextLong(), cube, xOrigin, yOrigin, zOrigin, x, y, z, f, curve, angle, 0, 0, 3.0D );
			}
		}
	}
}
