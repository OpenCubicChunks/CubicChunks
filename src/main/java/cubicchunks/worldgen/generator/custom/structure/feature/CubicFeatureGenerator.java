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

import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.cubeToCenterBlock;

import cubicchunks.util.CubePos;
import cubicchunks.util.XYZAddressable;
import cubicchunks.util.XYZMap;
import cubicchunks.world.ICubicWorld;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.custom.structure.CubicStructureGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CubicFeatureGenerator extends CubicStructureGenerator {

    private CubicFeatureData structureData;

    /**
     * Used to store a list of all structures that have been recursively generated. Used so that during recursive
     * generation, the structure generator can avoid generating structures that intersect ones that have already been
     * placed.
     */
    protected XYZMap<StructureStartWrapped> structureMap = new XYZMap<>(0.5f, 1024);

    public abstract String getStructureName();

    @Override
    protected synchronized void generate(ICubicWorld world, ICubePrimer cube, int structureX, int structureY, int structureZ,
            CubePos generatedCubePos) {
        this.initializeStructureData((World) world);

        if (!this.structureMap.contains(structureX, structureY, structureZ)) {
            this.rand.nextInt();
            try {
                if (this.canSpawnStructureAtCoords(structureX, structureY, structureZ)) {
                    StructureStartWrapped start = this.getStructureStart(structureX, structureY, structureZ);
                    this.structureMap.put(start);
                    if (start.getStart().isSizeableStructure()) {
                        this.setStructureStart(structureX, structureY, structureZ, start);
                    }
                }
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.makeCrashReport(throwable, "Exception preparing structure feature");
                CrashReportCategory category = report.makeCategory("Feature being prepared");
                category.addDetail("Is feature chunk", () -> this.canSpawnStructureAtCoords(structureX, structureY, structureZ) ? "True" : "False");
                category.addCrashSection("Chunk location", String.format("%d,%d,%d", structureX, structureY, structureZ));
                category.addDetail("Structure type", () -> this.getClass().getCanonicalName());
                throw new ReportedException(report);
            }
        }
    }

    public synchronized boolean generateStructure(World world, Random rand, CubePos cubePos) {
        this.initializeStructureData(world);
        int centerX = cubeToCenterBlock(cubePos.getX());
        int centerY = cubeToCenterBlock(cubePos.getY());
        int centerZ = cubeToCenterBlock(cubePos.getZ());
        boolean generated = false;
        for (StructureStartWrapped wrapped : this.structureMap) {
            StructureStart structStart = wrapped.getStart();
            // TODO: cubic chunks version of isValidForPostProcess and notifyPostProcess (mixin)
            if (structStart.isSizeableStructure() && structStart.isValidForPostProcess(cubePos.chunkPos())
                    && structStart.getBoundingBox().intersectsWith(
                    new StructureBoundingBox(centerX, centerY, centerZ, centerX + 15, centerY + 15, centerZ + 15))) {
                structStart.generateStructure(world, rand,
                        new StructureBoundingBox(centerX, centerY, centerZ, centerX + 15, centerY + 15, centerZ + 15));
                structStart.notifyPostProcessAt(cubePos.chunkPos());
                generated = true;
                this.setStructureStart(structStart.getChunkPosX(), wrapped.getY(), structStart.getChunkPosZ(), wrapped);
            }
        }

        return generated;
    }

    public boolean isInsideStructure(BlockPos pos) {
        this.initializeStructureData((World) this.world);
        return this.getStructureAt(pos) != null;
    }

    @Nullable
    protected StructureStartWrapped getStructureAt(BlockPos pos) {
        for (StructureStartWrapped startWrapped : this.structureMap) {
            StructureStart start = startWrapped.getStart();

            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                for (StructureComponent component : start.getComponents()) {
                    if (component.getBoundingBox().isVecInside(pos)) {
                        return startWrapped;
                    }
                }
            }
        }
        return null;
    }

    public boolean isPositionInStructure(World world, BlockPos pos) {
        this.initializeStructureData(world);
        for (StructureStartWrapped wrapped : this.structureMap) {
            if (wrapped.getStart().isSizeableStructure() && wrapped.getStart().getBoundingBox().isVecInside(pos)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public abstract BlockPos getClosestStrongholdPos(World worldIn, BlockPos pos, boolean findUnexplored);

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

                if (nbtbase.getId() == 10) {
                    NBTTagCompound tag = (NBTTagCompound) nbtbase;

                    if (tag.hasKey("ChunkX") && tag.hasKey("ChunkY") && tag.hasKey("ChunkZ")) {
                        int cubeY = tag.getInteger("ChunkY");
                        StructureStart structurestart = MapGenStructureIO.getStructureStart(tag, world);
                        if (structurestart != null) {
                            this.structureMap.put(new StructureStartWrapped(structurestart, cubeY));
                        }
                    }
                }
            }
        }
    }

    private void setStructureStart(int chunkX, int chunkY, int chunkZ, StructureStartWrapped start) {
        this.structureData.writeInstance(start.writeStructureComponentsToNBT(chunkX, chunkY, chunkZ), chunkX, chunkY, chunkZ);
        this.structureData.markDirty();
    }

    protected abstract boolean canSpawnStructureAtCoords(int chunkX, int chunkY, int chunkZ);

    protected abstract StructureStartWrapped getStructureStart(int chunkX, int chunkY, int chunkZ);

    public class StructureStartWrapped implements XYZAddressable {

        private final StructureStart start;
        private final int y;

        public StructureStartWrapped(StructureStart start, int y) {

            this.start = start;
            this.y = y;
        }

        @Override public int getX() {
            return start.getChunkPosX();
        }

        @Override public int getY() {
            return y;
        }

        @Override public int getZ() {
            return start.getChunkPosZ();
        }

        public StructureStart getStart() {
            return start;
        }

        public NBTTagCompound writeStructureComponentsToNBT(int chunkX, int chunkY, int chunkZ) {
            NBTTagCompound tag = start.writeStructureComponentsToNBT(chunkX, chunkZ);
            tag.setInteger("ChunkY", chunkY);
            return tag;
        }
    }
}
