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
package cubicchunks.worldgen.generator.custom.structure.feature;

import static cubicchunks.util.Coords.blockCeilToCube;
import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.cubeToCenterBlock;
import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;

import com.google.common.collect.Lists;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureStrongholdPieces;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

public class CubicStrongholdGenerator extends CubicFeatureGenerator {

    private CubePos[] structureCoords;
    private double distance;
    private int spread;
    private List<Biome> allowedBiomes;
    private boolean positionsGenerated;
    private final CustomGeneratorSettings conf;

    public CubicStrongholdGenerator(CustomGeneratorSettings conf) {
        super(4);
        this.conf = conf;
        this.structureCoords = new CubePos[128];
        this.distance = 32.0D;
        this.spread = 3;
        this.allowedBiomes = Lists.<Biome>newArrayList();

        for (Biome biome : ForgeRegistries.BIOMES) {
            if (biome != null && biome.getBaseHeight() > 0.0F && !BiomeManager.strongHoldBiomesBlackList.contains(biome)) {
                this.allowedBiomes.add(biome);
            }
        }

        for (Biome biome : BiomeManager.strongHoldBiomes) {
            if (!this.allowedBiomes.contains(biome)) {
                this.allowedBiomes.add(biome);
            }
        }
    }

    public CubicStrongholdGenerator(CustomGeneratorSettings conf, Map<String, String> data) {
        this(conf);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey().equals("distance")) {
                this.distance = MathHelper.getDouble(entry.getValue(), this.distance, 1.0D);
            } else if (entry.getKey().equals("count")) {
                this.structureCoords = new CubePos[MathHelper.getInt(entry.getValue(), this.structureCoords.length, 1)];
            } else if (entry.getKey().equals("spread")) {
                this.spread = MathHelper.getInt(entry.getValue(), this.spread, 1);
            }
        }
    }

    @Override public String getStructureName() {
        return "Stronghold";
    }

    @Nullable @Override public BlockPos getClosestStrongholdPos(World worldIn, BlockPos pos, boolean findUnexplored) {
        checkPositionsGenerated();

        BlockPos.MutableBlockPos currentBlock = new BlockPos.MutableBlockPos(0, 0, 0);

        BlockPos closestPos = null;
        double minDist = Double.MAX_VALUE;

        for (CubePos cubePos : this.structureCoords) {
            currentBlock.setPos(cubePos.getXCenter(), cubePos.getYCenter(), cubePos.getZCenter());
            double currentDist = currentBlock.distanceSq(pos);

            if (closestPos == null || currentDist < minDist) {
                closestPos = new BlockPos(currentBlock);
                minDist = currentDist;
            }
        }

        assert closestPos != null;

        return closestPos;
    }

    @Override protected boolean canSpawnStructureAtCoords(int chunkX, int chunkY, int chunkZ) {
        checkPositionsGenerated();

        return Arrays.stream(this.structureCoords)
                .anyMatch(cubePos -> chunkX == cubePos.getX() && chunkY == cubePos.getY() && chunkZ == cubePos.getZ());
    }

    @Override protected StructureStart getStructureStart(int chunkX, int chunkY, int chunkZ) {
        StructureStart start;
        do {
            start = new MapGenStronghold.Start((World) this.world, this.rand, chunkX, chunkZ);
            ((ICubicStructureStart) start).initCubic((World) world, conf, chunkY);
        } while (start.getComponents().isEmpty() || ((StructureStrongholdPieces.Stairs2) start.getComponents().get(0)).strongholdPortalRoom == null);
        return start;
    }

    private void checkPositionsGenerated() {
        if (!this.positionsGenerated) {
            this.generatePositions();
            this.positionsGenerated = true;
        }
    }

    private void generatePositions() {
        this.initializeStructureData((World) this.world);
        {
            int i = 0;
            for (ICubicStructureStart start : this.structureMap) {
                if (i >= this.structureCoords.length) {
                    break;
                }
                this.structureCoords[i++] = start.getCubePos();
            }
        }

        int nextIndex = this.structureMap.getSize();
        if (nextIndex >= this.structureCoords.length) {
            return;
        }

        Random rand = new Random();
        rand.setSeed(this.world.getSeed());

        double angle = rand.nextDouble() * Math.PI * 2.0D;

        int minCubeY = blockToCube(conf.getMinHeight());
        int maxCubeY = blockCeilToCube(conf.getAverageHeight());

        int distFactor = 0;
        int ringStep = 0;
        for (int i = 0; i < this.structureCoords.length; ++i) {
            double distance = 4.0D * this.distance + this.distance * (double) distFactor * 6.0D + (rand.nextDouble() - 0.5D) * this.distance * 2.5D;
            int chunkX;
            int chunkY;
            int chunkZ;
            if (conf.alternateStrongholdsPositions) {
                double yAngle = -rand.nextDouble() * Math.PI;
                chunkX = (int) round(cos(angle) * cos(yAngle) * distance);
                chunkY = (int) round(sin(yAngle) * distance) + maxCubeY; // TODO: use configuration-based offset
                chunkZ = (int) round(sin(angle) * cos(yAngle) * distance);
            } else {
                chunkX = (int) round(cos(angle) * distance);
                chunkY = MathHelper.getInt(rand, minCubeY, maxCubeY);
                chunkZ = (int) round(sin(angle) * distance);
            }
            BlockPos blockPos = this.world.getBiomeProvider().findBiomePosition(
                    cubeToCenterBlock(chunkX), cubeToCenterBlock(chunkZ), 112, this.allowedBiomes, rand);

            if (blockPos != null) {
                chunkX = blockToCube(blockPos.getX());
                chunkZ = blockToCube(blockPos.getZ());
            }

            chunkX = Math.floorDiv(chunkX, this.spacing) * this.spacing;
            chunkY = Math.floorDiv(chunkY, this.spacing) * this.spacing;
            chunkZ = Math.floorDiv(chunkZ, this.spacing) * this.spacing;

            if (i >= nextIndex) {
                this.structureCoords[i] = new CubePos(chunkX, chunkY, chunkZ);
            }

            angle += (Math.PI * 2D) / (double) this.spread;
            ++ringStep;

            if (ringStep == this.spread) {
                ++distFactor;
                ringStep = 0;
                this.spread += 2 * this.spread / (distFactor + 1);
                this.spread = Math.min(this.spread, this.structureCoords.length - i);
                angle += rand.nextDouble() * Math.PI * 2.0D;
            }
        }
    }
}
