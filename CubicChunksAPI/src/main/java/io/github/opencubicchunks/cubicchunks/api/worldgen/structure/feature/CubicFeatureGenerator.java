/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToCenterBlock;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.structure.ICubicStructureGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.util.Constants;

import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * CubicChunks specific version of {@link net.minecraft.world.gen.structure.MapGenStructure}
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CubicFeatureGenerator implements ICubicFeatureGenerator {

    protected final int spacingBitCount;
    protected final int spacingBitCountY;

    private CubicFeatureData structureData;

    /**
     * Used to store a list of all structures that have been recursively generated. Used so that during recursive
     * generation, the structure generator can avoid generating structures that intersect ones that have already been
     * placed.
     */
    protected XYZMap<ICubicFeatureStart> structureMap = new XYZMap<>(0.5f, 1024);

    protected CubicFeatureGenerator(int spacingBitCount, int spacingBitCountY) {
        this.spacingBitCount = spacingBitCount;
        this.spacingBitCountY = spacingBitCountY;
    }

    @Override public void generate(World world, @Nullable CubePrimer cube, CubePos cubePos) {
        this.generate(world, cube, cubePos, this::generateFeature, 8, 8, spacingBitCount, spacingBitCountY);
    }

    protected synchronized void generateFeature(World world, Random rand, @Nullable CubePrimer cube, int structureX, int structureY, int structureZ,
            CubePos generatedCubePos) {
        this.initializeStructureData(world);

        if (!this.structureMap.contains(structureX, structureY, structureZ)) {
            rand.nextInt();
            try {
                if (this.canSpawnStructureAtCoords(world, rand, structureX, structureY, structureZ)) {
                    StructureStart start = this.getStructureStart(world, rand, structureX, structureY, structureZ);
                    this.structureMap.put((ICubicFeatureStart) start);
                    if (start.isSizeableStructure()) {
                        this.setStructureStart(structureX, structureY, structureZ, start);
                    }
                }
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.makeCrashReport(throwable, "Exception preparing structure feature");
                CrashReportCategory category = report.makeCategory("Feature being prepared");
                category.addDetail("Is feature chunk", () ->
                        this.canSpawnStructureAtCoords(world, rand, structureX, structureY, structureZ) ? "True" : "False");
                category.addCrashSection("Chunk location", String.format("%d,%d,%d", structureX, structureY, structureZ));
                category.addDetail("Structure type", () -> this.getClass().getCanonicalName());
                throw new ReportedException(report);
            }
        }
    }

    @Override public synchronized boolean generateStructure(World world, Random rand, CubePos cubePos) {
        this.initializeStructureData(world);
        int centerX = cubeToCenterBlock(cubePos.getX());
        int centerY = cubeToCenterBlock(cubePos.getY());
        int centerZ = cubeToCenterBlock(cubePos.getZ());
        boolean generated = false;
        for (ICubicFeatureStart cubicStructureStart : this.structureMap) {
            StructureStart structStart = (StructureStart) cubicStructureStart;
            // TODO: cubic chunks version of isValidForPostProcess and notifyPostProcess (mixin)
            if (structStart.isSizeableStructure() && structStart.isValidForPostProcess(cubePos.chunkPos())
                    && structStart.getBoundingBox().intersectsWith(
                    new StructureBoundingBox(centerX, centerY, centerZ, centerX + ICube.SIZE - 1, centerY + ICube.SIZE - 1, centerZ + ICube.SIZE - 1))) {
                structStart.generateStructure(world, rand,
                        new StructureBoundingBox(centerX, centerY, centerZ, centerX + ICube.SIZE - 1, centerY + ICube.SIZE - 1, centerZ + ICube.SIZE - 1));
                structStart.notifyPostProcessAt(cubePos.chunkPos());
                generated = true;
                this.setStructureStart(structStart.getChunkPosX(), cubicStructureStart.getChunkPosY(), structStart.getChunkPosZ(), structStart);
            }
        }

        return generated;
    }

    @Override public boolean isInsideStructure(World world, BlockPos pos) {
        this.initializeStructureData(world);
        return this.getStructureAt(pos) != null;
    }

    @Nullable
    protected StructureStart getStructureAt(BlockPos pos) {
        for (ICubicFeatureStart cubicStructureStart : this.structureMap) {
            StructureStart start = (StructureStart) cubicStructureStart;

            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                for (StructureComponent component : start.getComponents()) {
                    if (component.getBoundingBox().isVecInside(pos)) {
                        return start;
                    }
                }
            }
        }
        return null;
    }

    @Override public boolean isPositionInStructure(World world, BlockPos pos) {
        this.initializeStructureData(world);
        for (ICubicFeatureStart cubicStart : this.structureMap) {
            StructureStart start = (StructureStart) cubicStart;
            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                return true;
            }
        }
        return false;
    }

    protected void initializeStructureData(World world) {
        if (this.structureData != null) {
            return;
        }
        this.structureData = (CubicFeatureData) world.getPerWorldStorage().getOrLoadData(CubicFeatureData.class, this.getStructureName());

        if (this.structureData == null) {
            this.structureData = new CubicFeatureData(this.getStructureName());
            world.getPerWorldStorage().setData(this.getStructureName(), this.structureData);
        } else {
            NBTTagCompound nbttagcompound = this.structureData.getTagCompound();

            for (String s : nbttagcompound.getKeySet()) {
                NBTBase nbtbase = nbttagcompound.getTag(s);

                if (nbtbase.getId() == Constants.NBT.TAG_COMPOUND) {
                    NBTTagCompound tag = (NBTTagCompound) nbtbase;

                    if (tag.hasKey("ChunkX") && tag.hasKey("ChunkY") && tag.hasKey("ChunkZ")) {
                        StructureStart structurestart = MapGenStructureIO.getStructureStart(tag, world);

                        if (structurestart != null) {
                            this.structureMap.put((ICubicFeatureStart) structurestart);
                        }
                    }
                }
            }
        }
    }

    private void setStructureStart(int chunkX, int chunkY, int chunkZ, StructureStart start) {
        this.structureData.writeInstance(start.writeStructureComponentsToNBT(chunkX, chunkZ), chunkX, chunkY, chunkZ);
        this.structureData.markDirty();
    }

    protected abstract boolean canSpawnStructureAtCoords(World world, Random rand, int chunkX, int chunkY, int chunkZ);

    protected abstract StructureStart getStructureStart(World world, Random rand, int chunkX, int chunkY, int chunkZ);
}