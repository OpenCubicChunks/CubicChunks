package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import java.util.Optional;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRandom;
import io.github.opencubicchunks.cubicchunks.world.ICubicStructureStart;
import io.github.opencubicchunks.cubicchunks.world.gen.structure.CubicStructureConfiguration;
import io.github.opencubicchunks.cubicchunks.world.gen.structure.ICubicStructureFeature;
import io.github.opencubicchunks.cubicchunks.world.gen.structure.ICubicStructureFeatureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StrongholdFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureFeature.class)
public abstract class MixinStructureFeature<C extends FeatureConfiguration> implements ICubicStructureFeature<C> {


    @Shadow protected abstract boolean linearSeparation();

    @Shadow protected abstract StructureStart<C> createStart(int chunkX, int chunkZ, BoundingBox boundingBox, int referenceCount, long worldSeed);

    @Shadow protected abstract boolean isFeatureChunk(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long worldSeed, WorldgenRandom random, int chunkX, int chunkZ,
                                                      Biome biome, ChunkPos chunkPos, C config, LevelHeightAccessor levelHeightAccessor);

    @Shadow public abstract ChunkPos getPotentialFeatureChunk(StructureFeatureConfiguration config, long worldSeed, WorldgenRandom placementRandom, int chunkX, int chunkY);

    @Inject(at = @At("HEAD"), method = "getNearestGeneratedFeature", cancellable = true)
    private void getNearestStructure3D(LevelReader level, StructureFeatureManager manager, BlockPos blockPos, int searchRadius, boolean skipExistingChunks, long seed,
                                       StructureFeatureConfiguration structureFeatureConfiguration, CallbackInfoReturnable<BlockPos> cir) {


        if (((CubicLevelHeightAccessor) level).generates2DChunks()) {
            return;
        }

        int spacing = structureFeatureConfiguration.spacing();
        int ySpacing = IBigCube.DIAMETER_IN_SECTIONS;

        int mainSectionX = SectionPos.blockToSectionCoord(blockPos.getX());
        int mainSectionY = SectionPos.blockToSectionCoord(blockPos.getY());
        int mainSectionZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        WorldgenRandom worldgenRandom = new CubeWorldGenRandom();

        for (int radius = 0; radius <= 100; ++radius) {
            for (int dx = -radius; dx <= radius; ++dx) {
                boolean isEdgeX = dx == -radius || dx == radius;

                for (int dz = -radius; dz <= radius; ++dz) {
                    boolean isEdgeZ = dz == -radius || dz == radius;

                    int yRadius = Math.min(radius, 4); //TODO: Optimize Y radius search.
                    for (int dy = -yRadius; dy <= yRadius; dy++) {
                        boolean isEdgeY = dy == -radius || dy == radius;

                        if (!isEdgeX && !isEdgeZ && !isEdgeY) {
                            continue;
                        }
                        int xPos = mainSectionX + spacing * dx;
                        int yPos = mainSectionY + ySpacing * dy;
                        int zPos = mainSectionZ + spacing * dz;

                        // TODO: make it use section pos directly
                        CubePos pos = CubePos.from(this.getPotentialFeatureCube(structureFeatureConfiguration, seed, worldgenRandom, xPos, yPos, zPos));
                        IBigCube cube = null;
                        for (int sectionX = 0; sectionX < IBigCube.DIAMETER_IN_SECTIONS; sectionX++) {
                            for (int sectionZ = 0; sectionZ < IBigCube.DIAMETER_IN_SECTIONS; sectionZ++) {
                                SectionPos sp = SectionPos.of(
                                    Coords.cubeToSection(pos.getX(), sectionX),
                                    Coords.cubeToSection(pos.getY(), 0),
                                    Coords.cubeToSection(pos.getZ(), sectionZ)
                                );
                                boolean validBiome =
                                    level.getBiomeManager().getPrimaryBiomeAtChunk(new ChunkPos(sp.x(), sp.z())).getGenerationSettings().isValidStart((StructureFeature<?>) (Object) this);
                                if (!validBiome) {
                                    continue;
                                }
                                if (cube == null) {
                                    cube = ((ICubicWorld) level).getCube(pos, ChunkStatus.STRUCTURE_STARTS);
                                }
                                StructureStart<?> structureStart = manager.getStartForFeature(sp, (StructureFeature<?>) (Object) this, cube);
                                if (structureStart == null || !structureStart.isValid()) {
                                    continue;
                                }
                                if (skipExistingChunks && structureStart.canBeReferenced()) {
                                    structureStart.addReference();
                                    cir.setReturnValue(structureStart.getLocatePos());
                                    return;
                                }

                                if (!skipExistingChunks) {
                                    cir.setReturnValue(structureStart.getLocatePos());
                                    return;
                                }
                            }
                        }
                        if (radius == 0) {
                            break;
                        }
                    }
                    if (radius == 0) {
                        break;
                    }
                }

                if (radius == 0) {
                    break;
                }
            }
        }

        cir.setReturnValue(null);
    }


    public final SectionPos getPotentialFeatureCube(StructureFeatureConfiguration config, long seed, WorldgenRandom rand, int sectionX, int sectionY, int sectionZ) {
        Optional<CubicStructureConfiguration> verticalSettingsOptional = ((ICubicStructureFeatureConfiguration) config).getVerticalSettings();

        if (!verticalSettingsOptional.isPresent()) {
            return SectionPos.of(this.getPotentialFeatureChunk(config, seed, rand, sectionX, sectionZ), 0);
        }

        CubicStructureConfiguration verticalSettings = verticalSettingsOptional.get();


        int spacing = config.spacing();
        int ySpacing = verticalSettings.getYSpacing();
        int separation = config.separation();
        int ySeparation = verticalSettings.getYSeparation();
        int gridX = Math.floorDiv(sectionX, spacing);
        int gridY = Math.floorDiv(sectionY, ySpacing);
        int gridZ = Math.floorDiv(sectionZ, spacing);
        ((CubeWorldGenRandom) rand).setLargeFeatureWithSalt(seed, gridX, gridY, gridZ, config.salt());
        int dx;
        int dy;
        int dz;

        if (this.linearSeparation()) {
            dx = rand.nextInt(spacing - separation);
            dy = rand.nextInt(ySpacing - ySeparation);
            dz = rand.nextInt(spacing - separation);
        } else {
            dx = (rand.nextInt(spacing - separation) + rand.nextInt(spacing - separation)) / 2;
            dy = (rand.nextInt(ySpacing - ySeparation) + rand.nextInt(ySpacing - ySeparation)) / 2;
            dz = (rand.nextInt(spacing - separation) + rand.nextInt(spacing - separation)) / 2;
        }

        return SectionPos.of(gridX * spacing + dx, gridY * ySpacing + dy, gridZ * spacing + dz);
    }

    @Override
    public boolean isFeatureSection(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long worldSeed, WorldgenRandom random, int sectionX, int sectionY, int sectionZ,
                                    Biome biome, SectionPos chunkPos, C config, LevelHeightAccessor levelHeightAccessor) {
        return isFeatureChunk(chunkGenerator, biomeSource, worldSeed + sectionY, random, sectionX, sectionZ, biome, chunkPos.chunk(), config, levelHeightAccessor);
    }

    @Override
    public StructureStart<?> generateCC(RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, StructureManager structureManager, long worldSeed,
                                        SectionPos sectionPos, Biome biome, int referenceCount, WorldgenRandom worldgenRandom, StructureFeatureConfiguration structureFeatureConfiguration,
                                        C featureConfiguration, LevelHeightAccessor levelHeightAccessor) {
        SectionPos outSection = this.getPotentialFeatureCube(structureFeatureConfiguration, worldSeed, worldgenRandom, sectionPos.x(), sectionPos.y(), sectionPos.z());

        if (sectionPos.x() == outSection.x() && sectionPos.y() == outSection.y() && sectionPos.z() == outSection.z() &&
            this.isFeatureSection(chunkGenerator, biomeSource, worldSeed, worldgenRandom, sectionPos.x(), sectionPos.y(), sectionPos.z(), biome,
                outSection, featureConfiguration, levelHeightAccessor)) {

            StructureStart<C> structureStart = this.createStart(sectionPos.x(), sectionPos.z(), BoundingBox.getUnknownBox(), referenceCount, worldSeed);
            ((ICubicStructureStart) structureStart).init3dPlacement(sectionPos.y());

            structureStart.generatePieces(registryAccess, chunkGenerator, structureManager, sectionPos.x(), sectionPos.z(), biome, featureConfiguration, levelHeightAccessor);
            movePieces(structureStart, sectionPos.y());
            if (structureStart.isValid()) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    private void movePieces(StructureStart<?> start, int sectionY) {
        if (start instanceof StrongholdFeature.StrongholdStart) { // TODO: configurable?
            return;
        }
        BoundingBox boundingBox = start.getBoundingBox();
        int currentY = (boundingBox.y0 + boundingBox.y1) >> 1;
        int targetY = Coords.sectionToMinBlock(sectionY);
        int dy = targetY - currentY;

        boundingBox.move(0, dy, 0);
        for (StructurePiece piece : start.getPieces()) {
            piece.getBoundingBox().move(0, dy, 0);
        }
    }

}
