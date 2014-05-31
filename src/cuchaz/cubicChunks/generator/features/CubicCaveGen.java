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
package cuchaz.cubicChunks.generator.features;

import java.util.Random;

import cuchaz.cubicChunks.world.Cube;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
/*
 * Modified Minecraft cave generation code. Based on Robinton's cave generation implementation.
 */

public class CubicCaveGen extends CubicMapGenBase
{
	protected void generate( World world, Cube cube, int x, int y, int z, int xOrigin, int yOrigin, int zOrigin )
	{
		if ( m_rand.nextInt( 16 ) != 0 )
		{
			return;
		}
		int tries = this.m_rand.nextInt( this.m_rand.nextInt( this.m_rand.nextInt( 15 ) + 1 ) + 1 );

		if ( this.m_rand.nextInt( 7 ) != 0 )
		{
			tries = 0;
		}

		for ( int n1 = 0; n1 < tries; ++n1 )
		{
			double x1 = x * 16 + this.m_rand.nextInt( 16 );
			double y1 = y * 16 + this.m_rand.nextInt( 16 );
			double z1 = z * 16 + this.m_rand.nextInt( 16 );
			int numTries = 1;

			if ( this.m_rand.nextInt( 4 ) == 0 )
			{
				this.generateLargeCaveNode( cube, this.m_rand.nextLong(), xOrigin, yOrigin, zOrigin, x1, y1, z1 );
				numTries += this.m_rand.nextInt( 4 );
			}

			for ( int n2 = 0; n2 < numTries; ++n2 )
			{
				float curve = this.m_rand.nextFloat() * ( float ) Math.PI * 2.0F;
				float angle = ( this.m_rand.nextFloat() - 0.5F ) * 2.0F / 8.0F;
				float f2 = this.m_rand.nextFloat() * 2.0F + this.m_rand.nextFloat();

				if ( m_rand.nextInt( 10 ) == 0 )
				{
					f2 *= m_rand.nextFloat() * m_rand.nextFloat() * 3.0F + 1.0F;
				}

				this.generateCaveNode( cube, this.m_rand.nextLong(), xOrigin, yOrigin, zOrigin, x1, y1, z1, f2/* * 2.0F*/, curve, angle, 0, 0, /*0.5D*/ 1.0D );
			}
		}
	}

	/**
	 * Generates a larger initial cave node than usual. Called 25% of the time.
	 */
	protected void generateLargeCaveNode( Cube cube, long seed, int xOrigin, int yOrigin, int zOrigin,  double x, double y, double z )
	{
		this.generateCaveNode( cube, seed, xOrigin, yOrigin, zOrigin, x, y, z, 1.0F + this.m_rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D );
	}

	/**
	 * Generates a node in the current cave system recursion tree.
	 */
	protected void generateCaveNode(Cube cube, long randLong, int xOrigin, int yOrigin, int zOrigin, double x, double y, double z, float f, float curve, float angle, int n, int tries, double d3 )
	{
		double xOCenter = xOrigin * 16D + 8D;
		double yOCenter = yOrigin * 16D + 8D;
		double zOCenter = zOrigin * 16D + 8D;
		float f3 = 0.0F;
		float f4 = 0.0F;
		Random random = new Random( randLong );

		if ( tries <= 0 )
		{
			int radius = this.m_range * 16 - 16;
			tries = radius - random.nextInt( radius / 4 );
		}

		boolean kAltered = false;

		if ( n == -1 )
		{
			n = tries / 2;
			kAltered = true;
		}

		int r1 = random.nextInt( tries / 2 ) + tries / 4;

		for ( boolean r2 = random.nextInt( 6 ) == 0; n < tries; ++n )
		{
			double modSin = 1.5D + ( double ) ( MathHelper.sin( ( float ) n * ( float ) Math.PI / ( float ) tries ) * f * 1.0F );
			double yModSin = modSin * d3;
			float angleCos = MathHelper.cos( angle );
			float angleSin = MathHelper.sin( angle );
			x += ( double ) ( MathHelper.cos( curve ) * angleCos );
			y += ( double ) angleSin;
			z += ( double ) ( MathHelper.sin( curve ) * angleCos );

			if ( r2 )
			{
				angle *= 0.92F;
			} else
			{
				angle *= 0.7F;
			}

			angle += f4 * 0.1F;
			curve += f3 * 0.1F;
			f4 *= 0.9F;
			f3 *= 0.75F;
			f4 += ( random.nextFloat() - random.nextFloat() ) * random.nextFloat() * 2.0F;
			f3 += ( random.nextFloat() - random.nextFloat() ) * random.nextFloat() * 4.0F;

			if ( !kAltered && n == r1 && f > 1.0F )
			{
				this.generateCaveNode( cube, random.nextLong(), xOrigin, yOrigin, zOrigin,
						x, y, z, 
						random.nextFloat() * 0.5F + 0.5F, 
						curve - ( ( float ) Math.PI / 2F ), 
						angle / 3.0F, 
						n, tries, 1.0D );
				this.generateCaveNode( cube, random.nextLong(), xOrigin, yOrigin, zOrigin, 
						x, y, z, 
						random.nextFloat() * 0.5F + 0.5F, 
						curve + ( ( float ) Math.PI / 2F ), 
						angle / 3.0F, 
						n, tries, 1.0D );
				return;
			}

			if ( kAltered || random.nextInt( 4 ) != 0 )
			{
				double xDist = x - xOCenter;
				double yDist = y - yOCenter;
				double zDist = z - zOCenter;
				double triesLeft = ( double ) ( tries - n );
				double fDist = ( double ) ( f + 2.0F + 16.0F );

				//Use yDist?
				if ( xDist * xDist + yDist * yDist + zDist * zDist - triesLeft * triesLeft > fDist * fDist )
				{
					return;
				}

				//Check y coords?
				if ( x >= xOCenter - 16.0D - modSin * 2.0D
					&& y >= yOCenter - 16.0D - yModSin * 2.0D
					&& z >= zOCenter - 16.0D - modSin * 2.0D
					&& x <= xOCenter + 16.0D + modSin * 2.0D
					&& y <= yOCenter + 16.0D + yModSin * 2.0D
					&& z <= zOCenter + 16.0D + modSin * 2.0D )
				{
					int xDist1 = MathHelper.floor_double( x - modSin ) - xOrigin * 16 - 1;
					int xDist2 = MathHelper.floor_double( x + modSin ) - xOrigin * 16 + 1;
					int yDist1 = MathHelper.floor_double( y - yModSin ) - yOrigin * 16 - 1;
					int yDist2 = MathHelper.floor_double( y + yModSin ) - yOrigin * 16 + 1;
					int zDist1 = MathHelper.floor_double( z - modSin ) - zOrigin * 16 - 1;
					int zDist2 = MathHelper.floor_double( z + modSin ) - zOrigin * 16 + 1;

					//Probably this causes some glitches
					if ( xDist1 < 0 )
					{
						xDist1 = 0;
					}

					if ( xDist2 > 16 )
					{
						xDist2 = 16;
					}

					if ( yDist1 < 0 )//orig: 1
					{
						yDist1 = 0;//orig: 1
					}

					if ( yDist2 > 16 )//orig: 120
					{
						yDist2 = 16;//orig: 120
					}

					if ( zDist1 < 0 )
					{
						zDist1 = 0;
					}

					if ( zDist2 > 16 )
					{
						zDist2 = 16;
					}

					boolean hitLava = false;
					int x2;
					//int temp;

					for ( x2 = xDist1; !hitLava && x2 < xDist2; ++x2 )
					{
						for ( int z2 = zDist1; !hitLava && z2 < zDist2; ++z2 )
						{
							for ( int y2 = yDist2 - 1; !hitLava && y2 >= yDist1; --y2 )
							{
								//temp = ( x2 * 16 + z2 ) * 16/*128*/ + y2;
								Block block = cube.getBlock( x2, y2, z2 );

								if ( y2 >= 0 && y2 < 16/*128*/ )
								{
									if ( block == Blocks.flowing_lava || block == Blocks.lava )
									{
										hitLava = true;
									}

									if ( y2 != yDist1 - 1 && x2 != xDist1 && x2 != xDist2 - 1 && z2 != zDist1 && z2 != zDist2 - 1 )
									{
										y2 = yDist1;//WTF!?
									}
								}
							}
						}
					}
					if ( !hitLava )
					{
						//Will it work?
						for ( x2 = xDist1; x2 < xDist2; ++x2 )
						{
							//xDistance from what to what?
							double distX = ( ( double ) ( x2 + xOrigin * 16 ) + 0.5D - x ) / modSin;

							for ( int z2 = zDist1; z2 < zDist2; ++z2 )
							{
								double distZ = ( ( double ) ( z2 + zOrigin * 16 ) + 0.5D - z ) / modSin;
								//int aIndex = ( x2 * 16 + z2 ) * 16/*128*/ + yDist2;

								//for ( int y2 = yDist2 - 1; y2 >= yDist1; --y2 )//what?
								for ( int y2 = yDist1; y2 < yDist2; ++y2 )
								{
									double distY = ( ( double ) ( y2 + yOrigin * 16 ) + 0.5D - y ) / yModSin;

									if ( distY > -0.7D && distX * distX + distY * distY + distZ * distZ < 1.0D )
									{
										//No lava generation, infinite depth.
										//Lava will be generatede differently (or not generated)
										Block block = cube.getBlock( x2, y2, z2 );

										if ( block == Blocks.stone || block == Blocks.dirt || block == Blocks.grass )
										{
											cube.setBlockForGeneration( x2, y2, z2, Blocks.air );
											//abyte[aIndex] = 0;
										}
									}
									//what?
									//--aIndex;
								}
							}
						}

						if ( kAltered )
						{
							break;
						}
					}
				}
			}
		}
	}
}
