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

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.provider.CubicWorldProvider;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.IColumnGenerator;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.vanilla.VanillaCompatibilityGenerator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class VanillaCubicProvider extends CubicWorldProvider {

	private WorldProvider wp;

	private VanillaCompatibilityGenerator compatGen;

	public VanillaCubicProvider(ICubicWorld world, WorldProvider provider, IChunkGenerator reUse) {
		this.wp = provider;
		this.worldObj = (World) world;

		if(!(worldObj.getWorldType() instanceof ICubicWorldType)){
			compatGen = new VanillaCompatibilityGenerator(
							reUse == null ? wp.createChunkGenerator() : reUse,
							world);
		}
	}

	public VanillaCubicProvider(ICubicWorld world, WorldProvider provider) {
		this(world, provider, null);
	}

	@Override
	public IColumnGenerator createColumnGenerator() {
		if (compatGen == null) {
			return ((ICubicWorldType)worldObj.getWorldType()).createColumnGenerator(getCubicWorld());
		}
		return compatGen;
	}

	@Override
	public ICubeGenerator createCubeGenerator() {
		if (compatGen == null) {
			return ((ICubicWorldType)worldObj.getWorldType()).createCubeGenerator(getCubicWorld());
		}
		return compatGen;
	}

	@SuppressWarnings("deprecation")
	public IChunkGenerator createChunkGenerator() {
		return wp.createChunkGenerator(); // Just in case a mod wants it (I cant think of why any would need to do this)
	}

	public boolean canCoordinateBeSpawn(int x, int z) {
		return wp.canCoordinateBeSpawn(x, z);
	}

	public float calculateCelestialAngle(long worldTime, float partialTicks) {
		return wp.calculateCelestialAngle(worldTime, partialTicks);
	}

	public int getMoonPhase(long worldTime) {
		return wp.getMoonPhase(worldTime);
	}

	public boolean isSurfaceWorld() {
		return wp.isSurfaceWorld();
	}

	@Nullable
	@SideOnly(Side.CLIENT)
	public float[] calcSunriseSunsetColors(float celestialAngle, float partialTicks) {
		return wp.calcSunriseSunsetColors(celestialAngle, partialTicks);
	}

	@SideOnly(Side.CLIENT)
	public Vec3d getFogColor(float p_76562_1_, float p_76562_2_) {
		return wp.getFogColor(p_76562_1_, p_76562_2_);
	}

	public boolean canRespawnHere() {
		return wp.canRespawnHere();
	}

	@SideOnly(Side.CLIENT)
	public float getCloudHeight() {
		return wp.getCloudHeight();
	}

	@SideOnly(Side.CLIENT)
	public boolean isSkyColored() {
		return wp.isSkyColored();
	}

	public BlockPos getSpawnCoordinate() {
		return wp.getSpawnCoordinate();
	}

	public int getAverageGroundLevel() {
		return wp.getAverageGroundLevel();
	}

	@SideOnly(Side.CLIENT)
	public boolean doesXZShowFog(int x, int z) {
		return wp.doesXZShowFog(x, z);
	}

	public BiomeProvider getBiomeProvider() {
		return wp.getBiomeProvider();
	}

	public boolean doesWaterVaporize() {
		return wp.doesWaterVaporize();
	}

	public boolean getHasNoSky() {
		return wp.getHasNoSky();
	}

	public float[] getLightBrightnessTable() {
		return wp.getLightBrightnessTable();
	}

	public WorldBorder createWorldBorder() {
		return wp.createWorldBorder();
	}

	public void setDimension(int dim) {
		wp.setDimension(dim);
	}

	public int getDimension() {
		return wp.getDimension();
	}

	public String getSaveFolder() {
		return wp.getSaveFolder();
	}

	public String getWelcomeMessage() {
		return wp.getWelcomeMessage();
	}

	public String getDepartMessage() {
		return wp.getDepartMessage();
	}

	public double getMovementFactor() {
		return wp.getMovementFactor();
	}

	@SideOnly(Side.CLIENT)
	public net.minecraftforge.client.IRenderHandler getSkyRenderer() {
		return wp.getSkyRenderer();
	}

	@SideOnly(Side.CLIENT)
	public void setSkyRenderer(net.minecraftforge.client.IRenderHandler skyRenderer) {
		wp.setSkyRenderer(skyRenderer);
	}

	@SideOnly(Side.CLIENT)
	public net.minecraftforge.client.IRenderHandler getCloudRenderer() {
		return wp.getCloudRenderer();
	}

	@SideOnly(Side.CLIENT)
	public void setCloudRenderer(net.minecraftforge.client.IRenderHandler renderer) {
		setCloudRenderer(renderer);
	}

	@SideOnly(Side.CLIENT)
	public net.minecraftforge.client.IRenderHandler getWeatherRenderer() {
		return wp.getWeatherRenderer();
	}

	@SideOnly(Side.CLIENT)
	public void setWeatherRenderer(net.minecraftforge.client.IRenderHandler renderer) {
		wp.setWeatherRenderer(renderer);
	}

	//public BlockPos getRandomizedSpawnPoint() {
	//	return wp.getRandomizedSpawnPoint();
	//}

	public boolean shouldMapSpin(String entity, double x, double y, double z) {
		return wp.shouldMapSpin(entity, x, y, z);
	}

	public int getRespawnDimension(net.minecraft.entity.player.EntityPlayerMP player) {
		return wp.getRespawnDimension(player);
	}

	public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities() {
		return wp.initCapabilities();
	}

	public Biome getBiomeForCoords(BlockPos pos) {
		return wp.getBiomeForCoords(pos);
	}

	public boolean isDaytime() {
		return wp.isDaytime();
	}

	public float getSunBrightnessFactor(float par1) {
		return wp.getSunBrightnessFactor(par1);
	}

	public float getCurrentMoonPhaseFactor() {
		return wp.getCurrentMoonPhaseFactor();
	}

	@SideOnly(Side.CLIENT)
	public Vec3d getSkyColor(net.minecraft.entity.Entity cameraEntity, float partialTicks) {
		return wp.getSkyColor(cameraEntity, partialTicks);
	}

	@SideOnly(Side.CLIENT)
	public Vec3d getCloudColor(float partialTicks) {
		return wp.getCloudColor(partialTicks);
	}

	@SideOnly(Side.CLIENT)
	public float getSunBrightness(float par1) {
		return wp.getSunBrightness(par1);
	}

	@SideOnly(Side.CLIENT)
	public float getStarBrightness(float par1) {
		return wp.getStarBrightness(par1);
	}

	public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
		wp.setAllowedSpawnTypes(allowHostile, allowPeaceful);
	}

	public void calculateInitialWeather() {
		wp.calculateInitialWeather();
	}

	public void updateWeather() {
		wp.updateWeather();
	}

	public boolean canBlockFreeze(BlockPos pos, boolean byWater) {
		return wp.canBlockFreeze(pos, byWater);
	}

	public boolean canSnowAt(BlockPos pos, boolean checkLight) {
		return wp.canSnowAt(pos, checkLight);
	}

	public void setWorldTime(long time) {
		wp.setWorldTime(time);
	}

	public long getSeed() {
		return wp.getSeed();
	}

	public long getWorldTime() {
		return wp.getWorldTime();
	}

	public BlockPos getSpawnPoint() {
		return wp.getSpawnPoint();
	}

	public void setSpawnPoint(BlockPos pos) {
		wp.setSpawnPoint(pos);
	}

	public boolean canMineBlock(net.minecraft.entity.player.EntityPlayer player, BlockPos pos) {
		return wp.canMineBlock(player, pos);
	}

	public boolean isBlockHighHumidity(BlockPos pos) {
		return wp.isBlockHighHumidity(pos);
	}

	public double getHorizon() {
		return wp.getHorizon();
	}

	public void resetRainAndThunder() {
		wp.resetRainAndThunder();
	}

	public boolean canDoLightning(net.minecraft.world.chunk.Chunk chunk) {
		return wp.canDoLightning(chunk);
	}

	public boolean canDoRainSnowIce(net.minecraft.world.chunk.Chunk chunk) {
		return wp.canDoRainSnowIce(chunk);
	}

	public void onPlayerAdded(EntityPlayerMP player) {
		wp.onPlayerAdded(player);
	}

	public void onPlayerRemoved(EntityPlayerMP player) {
		wp.onPlayerRemoved(player);
	}

	public DimensionType getDimensionType(){
		return wp.getDimensionType();
	}

	public void onWorldSave() {
		wp.onWorldSave();
	}

	public void onWorldUpdateEntities() {
		wp.onWorldUpdateEntities();
	}

	@SuppressWarnings("deprecation")
	public boolean canDropChunk(int x, int z) {
		return wp.canDropChunk(x, z);
	}
}
