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

import cubicchunks.world.ICubicWorld;
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

import javax.annotation.Nullable;

public class VanillaCubicProvider extends CubicWorldProvider {

	private static final WorldType HEURISTIC_WORLDTYPE = new WorldType("no-op") {
		@Override public IChunkGenerator getChunkGenerator(World world, String options) {
			return null; // now thats something we can detect >:D
		}
	};

	private WorldProvider provider;

	private ICubeGenerator cubeGen;

	public VanillaCubicProvider(ICubicWorld world, WorldProvider provider, @Nullable IChunkGenerator reUse) {
		this.provider = provider;
		this.worldObj = (World) world;

		boolean useProvider = false;

		if(worldObj.getWorldType() instanceof ICubicWorldType) { // Who do we trust!??!?! D:

			// nasty hack heuristic to see if provider asks its WorldType for a chunk generator
			//ReflectionUtil.setFieldValueSrg(wp, "field_76577_b", HEURISTIC_WORLDTYPE);
			provider.terrainType = HEURISTIC_WORLDTYPE;

			IChunkGenerator pro_or_null = provider.createChunkGenerator();

			// clean up
			//ReflectionUtil.setFieldValueSrg(provider, "field_76577_b", worldObj.getWorldType());
			provider.terrainType = worldObj.getWorldType();

			if(pro_or_null != null) { // It will be null if it tries to get one form WorldType

				// It was from a vanilla WorldProvider... use it
				cubeGen = new VanillaCompatibilityGenerator(
							reUse == null ? pro_or_null : reUse,
							world);
			}else{

				// It was from WorldType, try to use cubic generator
				cubeGen   = ((ICubicWorldType)worldObj.getWorldType()).createCubeGenerator(getCubicWorld());
				if(cubeGen == null) {
					useProvider = true;
				}
			}
		}else{
			useProvider = true;
		}

		if(useProvider) {
			cubeGen = new VanillaCompatibilityGenerator(
							reUse == null ? provider.createChunkGenerator() : reUse,
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
		return provider.createChunkGenerator(); // Just in case a mod wants it (I cant think of why any would need to do this)
	}

	@Override public boolean canCoordinateBeSpawn(int x, int z) {
		return provider.canCoordinateBeSpawn(x, z);
	}

	@Override public float calculateCelestialAngle(long worldTime, float partialTicks) {
		return provider.calculateCelestialAngle(worldTime, partialTicks);
	}

	@Override public int getMoonPhase(long worldTime) {
		return provider.getMoonPhase(worldTime);
	}

	@Override public boolean isSurfaceWorld() {
		return provider.isSurfaceWorld();
	}

	@Nullable
	@SideOnly(Side.CLIENT)
	@Override public float[] calcSunriseSunsetColors(float celestialAngle, float partialTicks) {
		return provider.calcSunriseSunsetColors(celestialAngle, partialTicks);
	}

	@SideOnly(Side.CLIENT)
	@Override public Vec3d getFogColor(float p_76562_1_, float p_76562_2_) {
		return provider.getFogColor(p_76562_1_, p_76562_2_);
	}

	@Override public boolean canRespawnHere() {
		return provider.canRespawnHere();
	}

	@SideOnly(Side.CLIENT)
	@Override public float getCloudHeight() {
		return provider.getCloudHeight();
	}

	@SideOnly(Side.CLIENT)
	@Override public boolean isSkyColored() {
		return provider.isSkyColored();
	}

	@Override public BlockPos getSpawnCoordinate() {
		return provider.getSpawnCoordinate();
	}

	@Override public int getAverageGroundLevel() {
		return provider.getAverageGroundLevel();
	}

	@SideOnly(Side.CLIENT)
	@Override public boolean doesXZShowFog(int x, int z) {
		return provider.doesXZShowFog(x, z);
	}

	@Override public BiomeProvider getBiomeProvider() {
		return provider.getBiomeProvider();
	}

	@Override public boolean doesWaterVaporize() {
		return provider.doesWaterVaporize();
	}

	@Override public boolean getHasNoSky() {
		return provider.getHasNoSky();
	}

	@Override public float[] getLightBrightnessTable() {
		return provider.getLightBrightnessTable();
	}

	@Override public WorldBorder createWorldBorder() {
		return provider.createWorldBorder();
	}

	@Override public void setDimension(int dim) {
		provider.setDimension(dim);
	}

	@Override public int getDimension() {
		return provider.getDimension();
	}

	@Override public String getSaveFolder() {
		return provider.getSaveFolder();
	}

	@Override public String getWelcomeMessage() {
		return provider.getWelcomeMessage();
	}

	@Override public String getDepartMessage() {
		return provider.getDepartMessage();
	}

	@Override public double getMovementFactor() {
		return provider.getMovementFactor();
	}

	@SideOnly(Side.CLIENT)
	@Override public net.minecraftforge.client.IRenderHandler getSkyRenderer() {
		return provider.getSkyRenderer();
	}

	@SideOnly(Side.CLIENT)
	@Override public void setSkyRenderer(net.minecraftforge.client.IRenderHandler skyRenderer) {
		provider.setSkyRenderer(skyRenderer);
	}

	@SideOnly(Side.CLIENT)
	@Override public net.minecraftforge.client.IRenderHandler getCloudRenderer() {
		return provider.getCloudRenderer();
	}

	@SideOnly(Side.CLIENT)
	@Override public void setCloudRenderer(net.minecraftforge.client.IRenderHandler renderer) {
		setCloudRenderer(renderer);
	}

	@SideOnly(Side.CLIENT)
	@Override public net.minecraftforge.client.IRenderHandler getWeatherRenderer() {
		return provider.getWeatherRenderer();
	}

	@SideOnly(Side.CLIENT)
	@Override public void setWeatherRenderer(net.minecraftforge.client.IRenderHandler renderer) {
		provider.setWeatherRenderer(renderer);
	}

	//public BlockPos getRandomizedSpawnPoint() {
	//	return wp.getRandomizedSpawnPoint();
	//}

	@Override public boolean shouldMapSpin(String entity, double x, double y, double z) {
		return provider.shouldMapSpin(entity, x, y, z);
	}

	@Override public int getRespawnDimension(net.minecraft.entity.player.EntityPlayerMP player) {
		return provider.getRespawnDimension(player);
	}

	@Override public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities() {
		return provider.initCapabilities();
	}

	@Override public Biome getBiomeForCoords(BlockPos pos) {
		return provider.getBiomeForCoords(pos);
	}

	@Override public boolean isDaytime() {
		return provider.isDaytime();
	}

	@Override public float getSunBrightnessFactor(float par1) {
		return provider.getSunBrightnessFactor(par1);
	}

	@Override public float getCurrentMoonPhaseFactor() {
		return provider.getCurrentMoonPhaseFactor();
	}

	@SideOnly(Side.CLIENT)
	@Override public Vec3d getSkyColor(net.minecraft.entity.Entity cameraEntity, float partialTicks) {
		return provider.getSkyColor(cameraEntity, partialTicks);
	}

	@SideOnly(Side.CLIENT)
	@Override public Vec3d getCloudColor(float partialTicks) {
		return provider.getCloudColor(partialTicks);
	}

	@SideOnly(Side.CLIENT)
	@Override public float getSunBrightness(float par1) {
		return provider.getSunBrightness(par1);
	}

	@SideOnly(Side.CLIENT)
	@Override public float getStarBrightness(float par1) {
		return provider.getStarBrightness(par1);
	}

	@Override public void setAllowedSpawnTypes(boolean allowHostile, boolean allowPeaceful) {
		provider.setAllowedSpawnTypes(allowHostile, allowPeaceful);
	}

	@Override public void calculateInitialWeather() {
		provider.calculateInitialWeather();
	}

	@Override public void updateWeather() {
		provider.updateWeather();
	}

	@Override public boolean canBlockFreeze(BlockPos pos, boolean byWater) {
		return provider.canBlockFreeze(pos, byWater);
	}

	@Override public boolean canSnowAt(BlockPos pos, boolean checkLight) {
		return provider.canSnowAt(pos, checkLight);
	}

	@Override public void setWorldTime(long time) {
		provider.setWorldTime(time);
	}

	@Override public long getSeed() {
		return provider.getSeed();
	}

	@Override public long getWorldTime() {
		return provider.getWorldTime();
	}

	@Override public BlockPos getSpawnPoint() {
		return provider.getSpawnPoint();
	}

	@Override public void setSpawnPoint(BlockPos pos) {
		provider.setSpawnPoint(pos);
	}

	@Override public boolean canMineBlock(net.minecraft.entity.player.EntityPlayer player, BlockPos pos) {
		return provider.canMineBlock(player, pos);
	}

	@Override public boolean isBlockHighHumidity(BlockPos pos) {
		return provider.isBlockHighHumidity(pos);
	}

	@Override public double getHorizon() {
		return provider.getHorizon();
	}

	@Override public void resetRainAndThunder() {
		provider.resetRainAndThunder();
	}

	@Override public boolean canDoLightning(net.minecraft.world.chunk.Chunk chunk) {
		return provider.canDoLightning(chunk);
	}

	@Override public boolean canDoRainSnowIce(net.minecraft.world.chunk.Chunk chunk) {
		return provider.canDoRainSnowIce(chunk);
	}

	@Override public void onPlayerAdded(EntityPlayerMP player) {
		provider.onPlayerAdded(player);
	}

	@Override public void onPlayerRemoved(EntityPlayerMP player) {
		provider.onPlayerRemoved(player);
	}

	@Override public DimensionType getDimensionType() {
		return provider.getDimensionType();
	}

	@Override public void onWorldSave() {
		provider.onWorldSave();
	}

	@Override public void onWorldUpdateEntities() {
		provider.onWorldUpdateEntities();
	}

	@SuppressWarnings("deprecation")
	@Override public boolean canDropChunk(int x, int z) {
		return provider.canDropChunk(x, z);
	}
}
