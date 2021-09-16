package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.structure;

import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureFeature.class)
public abstract class MixinStructureFeature {

    @Shadow protected abstract boolean linearSeparation();

    @Inject(at = @At("HEAD"), method = "getNearestGeneratedFeature", cancellable = true)
    private void getNearestStructure3D(LevelReader level, StructureFeatureManager manager, BlockPos blockPos, int searchRadius, boolean skipExistingChunks, long seed,
                                       StructureFeatureConfiguration structureFeatureConfiguration, CallbackInfoReturnable<BlockPos> cir) {

        if (((CubicLevelHeightAccessor) level).generates2DChunks()) {
            return;
        }

        int spacing = structureFeatureConfiguration.spacing();
        int ySpacing = CubeAccess.DIAMETER_IN_SECTIONS;

        int mainSectionX = SectionPos.blockToSectionCoord(blockPos.getX());
        int mainSectionY = SectionPos.blockToSectionCoord(blockPos.getY());
        int mainSectionZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        WorldgenRandom worldgenRandom = new WorldgenRandom();

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

                        CubePos pos = this.getPotentialFeatureChunk(structureFeatureConfiguration, seed, worldgenRandom, xPos, yPos, zPos);
                        CubeAccess cube = null;
                        for (int sectionX = 0; sectionX < CubeAccess.DIAMETER_IN_SECTIONS; sectionX++) {
                            for (int sectionZ = 0; sectionZ < CubeAccess.DIAMETER_IN_SECTIONS; sectionZ++) {
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
                                    cube = ((CubicLevelAccessor) level).getCube(pos, ChunkStatus.STRUCTURE_STARTS);
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

    @Redirect(method = "loadStaticStart", at = @At(value = "NEW", target = "net/minecraft/world/level/ChunkPos"))
    private static ChunkPos loadStaticCubeStart(int x, int z, ServerLevel world, CompoundTag nbt, long worldSeed) {
        ChunkPos original = new ChunkPos(x, z);
        if (!((CubicLevelHeightAccessor) world).isCubic()) {
            return original;
        } else {
            return nbt.contains("ChunkY") ? new ImposterChunkPos(x, nbt.getInt("ChunkY"), z) : original;
        }
    }

    public final CubePos getPotentialFeatureChunk(StructureFeatureConfiguration config, long seed, WorldgenRandom rand, int sectionX, int sectionY, int sectionZ) {
        int spacing = config.spacing();
        int separation = config.separation();
        int gridX = Math.floorDiv(sectionX, spacing);
        int gridZ = Math.floorDiv(sectionZ, spacing);
        rand.setLargeFeatureWithSalt(seed, gridX, gridZ, config.salt());
        int dx;
        int dz;
        // TODO: 3d structure placement?
        if (this.linearSeparation()) {
            dx = rand.nextInt(spacing - separation);
            dz = rand.nextInt(spacing - separation);
        } else {
            dx = (rand.nextInt(spacing - separation) + rand.nextInt(spacing - separation)) / 2;
            dz = (rand.nextInt(spacing - separation) + rand.nextInt(spacing - separation)) / 2;
        }
        return CubePos.of(Coords.sectionToCube(gridX * spacing + dx), sectionY, Coords.sectionToCube(gridZ * spacing + dz));
    }
}
