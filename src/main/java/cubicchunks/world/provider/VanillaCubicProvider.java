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

import javax.annotation.Nullable;

import cubicchunks.util.ReflectionUtil;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.provider.CubicWorldProvider;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.vanilla.VanillaCompatibilityGenerator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class VanillaCubicProvider extends CubicWorldProvider {

	private static final WorldType HEURISTIC_WORLDTYPE = new WorldType("no-op"){
		@Override public IChunkGenerator getChunkGenerator(World world, String options){
			return null; // now thats something we can detect >:D
		}
	};

	private WorldProvider wp;

	private ICubeGenerator cubeGen;

	public VanillaCubicProvider(ICubicWorld world, WorldProvider provider, @Nullable IChunkGenerator reUse) {
		this.wp = provider;
		this.worldObj = (World) world;

		boolean flag = false;

		if(worldObj.getWorldType() instanceof ICubicWorldType){ // Who do we trust!??!?! D:

			// nasty hack heuristic to see if provider asks its WorldType for a chunk generator
			ReflectionUtil.setFieldValueSrg(wp, "field_76577_b", HEURISTIC_WORLDTYPE);
			
			IChunkGenerator pro_or_null = wp.createChunkGenerator();
			
			// clean up
			ReflectionUtil.setFieldValueSrg(provider, "field_76577_b", worldObj.getWorldType()); 

			if(pro_or_null != null){ // It will be null if it tries to get one form WorldType

				// It was from a vanilla WorldProvider... use it
				cubeGen = new VanillaCompatibilityGenerator(
							reUse == null ? pro_or_null : reUse,
							world);
			}else{
				
				// It was from WorldType, try to use cubic generator
				cubeGen   = ((ICubicWorldType)worldObj.getWorldType()).createCubeGenerator(getCubicWorld());
				if(cubeGen == null){
					flag = true;
				}
			}
		}else{
			flag = true;
		}

		if(flag){
			cubeGen = new VanillaCompatibilityGenerator(
							reUse == null ? wp.createChunkGenerator() : reUse,
							world);
		}
	}

	public VanillaCubicProvider(ICubicWorld world, WorldProvider provider) {
		this(world, provider, null);
	}

	@Override
	public ICubeGenerator createCubeGenerator() {
		return cubeGen;
	}

	@SuppressWarnings("deprecation")
	@Override public IChunkGenerator createChunkGenerator() {
		return wp.createChunkGenerator(); // Just in case a mod wants it (I cant think of why any would need to do this)
	}

	@Override public boolean canCoordinateBeSpawn(int x, int z) {
		return wp.canCoordinateBeSpawn(x, z);
	}

	@Override public float calculateCelestialAngle(long worldTime, float partialTicks) {
		return wp.calculateCelestialAngle(worldTime, partialTicks);
	}

	@Override public int getMoonPhase(long worldTime) {
		return wp.getMoonPhase(worldTime);
	}

	@Override public boolean isSurfaceWorld() {
		return wp.isSurfaceWorld();
	}

	@Nullable
	@SideOnly(Side.CLIENT)
	@Override public float[] calcSunriseSunsetColors(float celestialAngle, float partialTicks) {
		return wp.calcSunriseSunsetColors(celestialAngle, partialTicks);
	}

	@SideOnly(Side.CLIENT)
	@Override public Vec3d getFogColor(float p_76562_1_, float p_76562_2_) {
		return wp.getFogColor(p_76562_1_, p_76562_2_);
	}

	@Override public boolean canRespawnHere() {
		return wp.canRespawnHere();
	}

	@SideOnly(Side.CLIENT)
	@Override public float getCloudHeight() {
		return wp.getCloudHeight();
	}

	@SideOnly(Side.CLIENT)
	@Override public boolean isSkyColored() {
		return wp.isSkyColored();
	}

	@Override public BlockPos getSpawnCoordinate() {
		return wp.getSpawnCoordinate();
	}

	@Override public int getAverageGroundLevel() {
		return wp.getAverageGroundLevel();
	}

	@SideOnly(Side.CLIENT)
	@Override public boolean doesXZShowFog(int x, int z) {
		return wp.doesXZShowFog(x, z);
	}

	@Override public BiomeProvider getBiomeProvider() {
		return wp.getBiomeProvider();
	}

	@Override public boolean doesWaterVaporize() {
		return wp.doesWaterVaporize();
	}

	@Override public boolean getHasNoSky() {
		return wp.getHasNoSky();
	}

	@Override public float[] getLightBrightnessTable() {
		return wp.getLightBrightnessTable();
	}

	@Override public WorldBorder createWorldBorder() {
		return wp.createWorldBorder();
	}

	@Override public void setDimension(int dim) {
		wp.setDimension(dim);
	}

	@Override public int getDimension() {
		return wp.getDimension();
	}

	@Override public String getSaveFolder() {
		return wp.getSaveFolder();
	}

	@Override public String getWelcomeMessage() {
		return wp.getWelcomeMessage();
	}

	@Override public String getDepartMessage() {
		return wp.getDepartMessage();
	}

	@Override public double getMovementFactor() {
		return wp.getMovementFactor();
	}

	@SideOnly(Side.CLIENT)
	@Override public net.minecraftforge.client.IRenderHandler getSkyRenderer() {
		return wp.getSkyRenderer();
	}

	@SideOnly(Side.CLIENT)
	@Override public void setSkyRenderer(net.minecraftforge.client.IRenderHandler skyRenderer) {
		wp.setSkyRenderer(skyRenderer);
	}

	@SideOnly(Side.CLIENT)
	@Override public net.minecraftforge.client.IRenderHandler getCloudRenderer() {
		return wp.getCloudRenderer();
	}

	@SideOnly(Side.CLIENT)
	@Override public void setCloudRenderer(net.minecraftforge.client.IRenderHandler renderer) {
		setCloudRenderer(renderer);
	}

	@SideOnly(Side.CLIENT)
	@Override public net.minecraftforge.client.IRenderHandler getWeatherRenderer() {
		return wp.getWeatherRenderer();
	}

	@SideOnly(Side.CLIENT)
	@Override public void setWeatherRenderer(net.minecraftforge.client.IRenderHandler renderer) {
		wp.setWeatherRenderer(renderer);
	}

	//public BlockPos getRandomizedSpawnPoint() {
	//	return wp.getRandomizedSpawnPoint();
	//}

	@Override public boolean shouldMapSpin(String entity, double x, double y, double z) {
		return wp.shouldMapSpin(entity, x, y, z);
	}

	@Override public int getRespawnDimension(net.minecraft.entity.player.EntityPlayerMP player) {
		return wp.getRespawnDimension(player);
	}

	@Override public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities() {
		return wp.initCapabilities();
	}

	@Override public Biome getBiomeForCoords(BlockPos pos) {
		return wp.getBiomeForCoords(pos);
	}

	@Override public boolean isDaytime() {
		return wp.isDaytime();
	}

	@Override public float getSunBrightnessFactor(float par1) {
		return wp.getSunBrightnessFactor(par1);
	}

	@Override public float getCurrentMoonPhaseFactor() {
		return wp.getCurrentMoonPhaseFactor();
	}

	@SideOnly(Side.CLIENT)
	@Override public Vec3d getSkyColor(net.minecraft.entity.Entity cameraEntity, float partialTicks) {
		return wp.getSkyColor(cameraEntity, partialTicks);
	}

	@SideOnly(Side.CLIENT)
	@Override public Vec3d getCloudColor(float partialTicks) {
		return wp.getCloudColor(partialTicks);
	}

	@SideOnly(Side.CLIENT)
	@Override public float getSunBrightness(float par1) {
		return wp.getSunBrightness(par1);
	}

	@SideOnly(Side.CLIENT)
	@Override public float getStarBrightness(float par1) {
		return wp.getStarBrightness(par1);
	}

	@Override public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
		wp.setAllowedSpawnTypes(allowHostile, allowPeaceful);
	}

	@Override public void calculateInitialWeather() {
		wp.calculateInitialWeather();
	}

	@Override public void updateWeather() {
		wp.updateWeather();
	}

	@Override public boolean canBlockFreeze(BlockPos pos, boolean byWater) {
		return wp.canBlockFreeze(pos, byWater);
	}

	@Override public boolean canSnowAt(BlockPos pos, boolean checkLight) {
		return wp.canSnowAt(pos, checkLight);
	}

	@Override public void setWorldTime(long time) {
		wp.setWorldTime(time);
	}

	@Override public long getSeed() {
		return wp.getSeed();
	}

	@Override public long getWorldTime() {
		return wp.getWorldTime();
	}

	@Override public BlockPos getSpawnPoint() {
		return wp.getSpawnPoint();
	}

	@Override public void setSpawnPoint(BlockPos pos) {
		wp.setSpawnPoint(pos);
	}

	@Override public boolean canMineBlock(net.minecraft.entity.player.EntityPlayer player, BlockPos pos) {
		return wp.canMineBlock(player, pos);
	}

	@Override public boolean isBlockHighHumidity(BlockPos pos) {
		return wp.isBlockHighHumidity(pos);
	}

	@Override public double getHorizon() {
		return wp.getHorizon();
	}

	@Override public void resetRainAndThunder() {
		wp.resetRainAndThunder();
	}

	@Override public boolean canDoLightning(net.minecraft.world.chunk.Chunk chunk) {
		return wp.canDoLightning(chunk);
	}

	@Override public boolean canDoRainSnowIce(net.minecraft.world.chunk.Chunk chunk) {
		return wp.canDoRainSnowIce(chunk);
	}

	@Override public void onPlayerAdded(EntityPlayerMP player) {
		wp.onPlayerAdded(player);
	}

	@Override public void onPlayerRemoved(EntityPlayerMP player) {
		wp.onPlayerRemoved(player);
	}

	@Override public DimensionType getDimensionType(){
		return wp.getDimensionType();
	}

	@Override public void onWorldSave() {
		wp.onWorldSave();
	}

	@Override public void onWorldUpdateEntities() {
		wp.onWorldUpdateEntities();
	}

	@SuppressWarnings("deprecation")
	@Override public boolean canDropChunk(int x, int z) {
		return wp.canDropChunk(x, z);
	}
}
