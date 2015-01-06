/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package main.java.cubicchunks.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

public class BlankColumn extends Column
{
	public BlankColumn( World world, int cubeX, int cubeZ )
	{
		super( world, cubeX, cubeZ );
	}
	
	// column overrides
	
	@Override
	public Cube getOrCreateCube( int cubeY, boolean isModified )
	{
		//hacky correction for world gen crash
		//needs more work. Client isn't loading the column (0,0) from the server
		if (this.getX() == 0 && this.getZ() == 0){
			return super.getOrCreateCube(cubeY, isModified);
		}
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Cube removeCube( int cubeY )
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void markSaved( )
	{
	}
	
	
	// chunk overrides
	
	@Override
	public int getHeightValue( int localX, int localZ )
	{
		return 0;
	}
	
	@Override
	public int getPrecipitationHeight( int localX, int localZ )
	{
		return 0;
	}
	
	@Override
	public void generateHeightMap( )
	{
	}
	
	@Override
	public void generateSkylightMap( )
	{
	}
	
	@Override
	public Block func_150810_a( int p_150810_1_, int p_150810_2_, int p_150810_3_ )
	{
		return Blocks.air;
	}
	
	@Override
	public int func_150808_b( int p_150808_1_, int p_150808_2_, int p_150808_3_ )
	{
		return 255;
	}
	
	@Override
	public boolean func_150807_a( int p_150807_1_, int p_150807_2_, int p_150807_3_, Block p_150807_4_, int p_150807_5_ )
	{
		return true;
	}
	
	@Override
	public int getBlockMetadata( int par1, int par2, int par3 )
	{
		return 0;
	}
	
	@Override
	public boolean setBlockMetadata( int par1, int par2, int par3, int par4 )
	{
		return false;
	}
	
	@Override
	public int getSavedLightValue( EnumSkyBlock par1EnumSkyBlock, int par2, int par3, int par4 )
	{
		return 0;
	}
	
	@Override
	public void setLightValue( EnumSkyBlock par1EnumSkyBlock, int par2, int par3, int par4, int par5 )
	{
	}
	
	@Override
	public int getBlockLightValue( int par1, int par2, int par3, int par4 )
	{
		return 0;
	}
	
	@Override
	public boolean canBlockSeeTheSky( int par1, int par2, int par3 )
	{
		return false;
	}

	@Override
	public TileEntity func_150806_e( int p_150806_1_, int p_150806_2_, int p_150806_3_ )
	{
		return null;
	}

	@Override
	public void addTileEntity( TileEntity p_150813_1_ )
	{
	}

	@Override
	public void func_150812_a( int p_150812_1_, int p_150812_2_, int p_150812_3_, TileEntity p_150812_4_ )
	{
	}

	@Override
	public void removeTileEntity( int p_150805_1_, int p_150805_2_, int p_150805_3_ )
	{
	}

	@Override
	public void onChunkLoad( )
	{
	}

	@Override
	public void onChunkUnload( )
	{
	}

	@Override
	public void setChunkModified( )
	{
	}
	
	@Override
	public boolean needsSaving( boolean par1 )
	{
		return false;
	}

	@Override
	public boolean isEmpty( )
	{
		return true;
	}

	@Override
	public boolean getAreLevelsEmpty( int par1, int par2 )
	{
		return true;
	}
}
