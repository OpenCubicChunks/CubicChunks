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
package cuchaz.cubicChunks;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class LightIndexBlockProxy extends Block
{
	private static TreeMap<Integer,LightIndexBlockProxy> m_cache;
	
	static
	{
		m_cache = new TreeMap<Integer,LightIndexBlockProxy>();
	}
	
	public static LightIndexBlockProxy get( int opacity )
	{
		LightIndexBlockProxy proxy = m_cache.get( opacity );
		if( proxy == null )
		{
			proxy = new LightIndexBlockProxy( opacity );
			m_cache.put( opacity, proxy );
		}
		return proxy;
	}
	
	private int m_opacity;
	
	protected LightIndexBlockProxy( int opacity )
	{
		super( Material.air );
		m_opacity = opacity;
	}
	
	@Override
	public int getLightOpacity( )
	{
		return m_opacity;
	}
	
	@Override
	public int getLightValue( )
	{
		// assume lights in unloaded cubic chunks are turned off
		return 0;
	}
	
	// fail fast if anyone tries to access properties other than opacity and light
	
	@Override
	public boolean func_149730_j( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getCanBlockGrass( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean func_149710_n( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Material getMaterial( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public MapColor getMapColor( int p_149728_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setStepSound( SoundType p_149672_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setLightOpacity( int p_149713_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setLightLevel( float p_149715_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setResistance( float p_149752_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBlockNormalCube( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isNormalCube( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean renderAsNormalBlock( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getBlocksMovement( IBlockAccess p_149655_1_, int p_149655_2_, int p_149655_3_, int p_149655_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRenderType( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setHardness( float p_149711_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setBlockUnbreakable( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public float getBlockHardness( World p_149712_1_, int p_149712_2_, int p_149712_3_, int p_149712_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setTickRandomly( boolean p_149675_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getTickRandomly( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasTileEntity( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getBlockBrightness( IBlockAccess p_149677_1_, int p_149677_2_, int p_149677_3_, int p_149677_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean shouldSideBeRendered( IBlockAccess p_149646_1_, int p_149646_2_, int p_149646_3_, int p_149646_4_, int p_149646_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBlockSolid( IBlockAccess p_149747_1_, int p_149747_2_, int p_149747_3_, int p_149747_4_, int p_149747_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IIcon getIcon( IBlockAccess p_149673_1_, int p_149673_2_, int p_149673_3_, int p_149673_4_, int p_149673_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IIcon getIcon( int p_149691_1_, int p_149691_2_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool( World p_149633_1_, int p_149633_2_, int p_149633_3_, int p_149633_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings( "rawtypes" )
	public void addCollisionBoxesToList( World p_149743_1_, int p_149743_2_, int p_149743_3_, int p_149743_4_, AxisAlignedBB p_149743_5_, List p_149743_6_, Entity p_149743_7_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool( World p_149668_1_, int p_149668_2_, int p_149668_3_, int p_149668_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOpaqueCube( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canCollideCheck( int p_149678_1_, boolean p_149678_2_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCollidable( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateTick( World p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_, Random p_149674_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void randomDisplayTick( World p_149734_1_, int p_149734_2_, int p_149734_3_, int p_149734_4_, Random p_149734_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBlockDestroyedByPlayer( World p_149664_1_, int p_149664_2_, int p_149664_3_, int p_149664_4_, int p_149664_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onNeighborBlockChange( World p_149695_1_, int p_149695_2_, int p_149695_3_, int p_149695_4_, Block p_149695_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int func_149738_a( World p_149738_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBlockAdded( World p_149726_1_, int p_149726_2_, int p_149726_3_, int p_149726_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void breakBlock( World p_149749_1_, int p_149749_2_, int p_149749_3_, int p_149749_4_, Block p_149749_5_, int p_149749_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int quantityDropped( Random p_149745_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Item getItemDropped( int p_149650_1_, Random p_149650_2_, int p_149650_3_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public float getPlayerRelativeBlockHardness( EntityPlayer p_149737_1_, World p_149737_2_, int p_149737_3_, int p_149737_4_, int p_149737_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void dropBlockAsItemWithChance( World p_149690_1_, int p_149690_2_, int p_149690_3_, int p_149690_4_, int p_149690_5_, float p_149690_6_, int p_149690_7_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void dropBlockAsItem_do( World p_149642_1_, int p_149642_2_, int p_149642_3_, int p_149642_4_, ItemStack p_149642_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void dropXpOnBlockBreak( World p_149657_1_, int p_149657_2_, int p_149657_3_, int p_149657_4_, int p_149657_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int damageDropped( int p_149692_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public float getExplosionResistance( Entity p_149638_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public MovingObjectPosition collisionRayTrace( World p_149731_1_, int p_149731_2_, int p_149731_3_, int p_149731_4_, Vec3 p_149731_5_, Vec3 p_149731_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBlockDestroyedByExplosion( World p_149723_1_, int p_149723_2_, int p_149723_3_, int p_149723_4_, Explosion p_149723_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRenderBlockPass( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canReplace( World p_149705_1_, int p_149705_2_, int p_149705_3_, int p_149705_4_, int p_149705_5_, ItemStack p_149705_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canPlaceBlockOnSide( World p_149707_1_, int p_149707_2_, int p_149707_3_, int p_149707_4_, int p_149707_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canPlaceBlockAt( World p_149742_1_, int p_149742_2_, int p_149742_3_, int p_149742_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onBlockActivated( World p_149727_1_, int p_149727_2_, int p_149727_3_, int p_149727_4_, EntityPlayer p_149727_5_, int p_149727_6_, float p_149727_7_, float p_149727_8_, float p_149727_9_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onEntityWalking( World p_149724_1_, int p_149724_2_, int p_149724_3_, int p_149724_4_, Entity p_149724_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int onBlockPlaced( World p_149660_1_, int p_149660_2_, int p_149660_3_, int p_149660_4_, int p_149660_5_, float p_149660_6_, float p_149660_7_, float p_149660_8_, int p_149660_9_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBlockClicked( World p_149699_1_, int p_149699_2_, int p_149699_3_, int p_149699_4_, EntityPlayer p_149699_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void velocityToAddToEntity( World p_149640_1_, int p_149640_2_, int p_149640_3_, int p_149640_4_, Entity p_149640_5_, Vec3 p_149640_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBlockBoundsBasedOnState( IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getBlockColor( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getRenderColor( int p_149741_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int colorMultiplier( IBlockAccess p_149720_1_, int p_149720_2_, int p_149720_3_, int p_149720_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int isProvidingWeakPower( IBlockAccess p_149709_1_, int p_149709_2_, int p_149709_3_, int p_149709_4_, int p_149709_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canProvidePower( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onEntityCollidedWithBlock( World p_149670_1_, int p_149670_2_, int p_149670_3_, int p_149670_4_, Entity p_149670_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int isProvidingStrongPower( IBlockAccess p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setBlockBoundsForItemRender( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void harvestBlock( World p_149636_1_, EntityPlayer p_149636_2_, int p_149636_3_, int p_149636_4_, int p_149636_5_, int p_149636_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean canSilkHarvest( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected ItemStack createStackedBlock( int p_149644_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int quantityDroppedWithBonus( int p_149679_1_, Random p_149679_2_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canBlockStay( World p_149718_1_, int p_149718_2_, int p_149718_3_, int p_149718_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBlockPlacedBy( World p_149689_1_, int p_149689_2_, int p_149689_3_, int p_149689_4_, EntityLivingBase p_149689_5_, ItemStack p_149689_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onPostBlockPlaced( World p_149714_1_, int p_149714_2_, int p_149714_3_, int p_149714_4_, int p_149714_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Block setBlockName( String p_149663_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getLocalizedName( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getUnlocalizedName( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onBlockEventReceived( World p_149696_1_, int p_149696_2_, int p_149696_3_, int p_149696_4_, int p_149696_5_, int p_149696_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getEnableStats( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block disableStats( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMobilityFlag( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public float getAmbientOcclusionLightValue( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onFallenUpon( World p_149746_1_, int p_149746_2_, int p_149746_3_, int p_149746_4_, Entity p_149746_5_, float p_149746_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Item getItem( World p_149694_1_, int p_149694_2_, int p_149694_3_, int p_149694_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDamageValue( World p_149643_1_, int p_149643_2_, int p_149643_3_, int p_149643_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	@SuppressWarnings( "rawtypes" )
	public void getSubBlocks( Item p_149666_1_, CreativeTabs p_149666_2_, List p_149666_3_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public CreativeTabs getCreativeTabToDisplayOn( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Block setCreativeTab( CreativeTabs p_149647_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBlockHarvested( World p_149681_1_, int p_149681_2_, int p_149681_3_, int p_149681_4_, int p_149681_5_, EntityPlayer p_149681_6_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void onBlockPreDestroy( World p_149725_1_, int p_149725_2_, int p_149725_3_, int p_149725_4_, int p_149725_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void fillWithRain( World p_149639_1_, int p_149639_2_, int p_149639_3_, int p_149639_4_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isFlowerPot( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean func_149698_L( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canDropFromExplosion( Explosion p_149659_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean func_149667_c( Block p_149667_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasComparatorInputOverride( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getComparatorInputOverride( World p_149736_1_, int p_149736_2_, int p_149736_3_, int p_149736_4_, int p_149736_5_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected Block setBlockTextureName( String p_149658_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected String getTextureName( )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public IIcon func_149735_b( int p_149735_1_, int p_149735_2_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerBlockIcons( IIconRegister p_149651_1_ )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getItemIconName( )
	{
		throw new UnsupportedOperationException();
	}
}
