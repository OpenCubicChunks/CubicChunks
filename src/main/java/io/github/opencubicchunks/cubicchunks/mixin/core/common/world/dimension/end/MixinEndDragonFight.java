package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.dimension.end;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.server.IServerChunkProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndDragonFight.class)
public class MixinEndDragonFight {

    @Shadow @Final private ServerLevel level;

    @Shadow private BlockPos portalLocation;

    @Shadow @Final private BlockPattern exitPortalPattern;

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerChunkCache;addRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V"))
    private <T> void addCubeRegionTickets(ServerChunkCache serverChunkCache, TicketType<T> ticketType, ChunkPos pos, int radius, T argument) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            serverChunkCache.addRegionTicket(ticketType, pos, radius, argument);
            return;
        }

        serverChunkCache.addRegionTicket(ticketType, pos, radius, argument); // TODO: Load order?
        ((IServerChunkProvider) serverChunkCache).addCubeRegionTicket(CCTicketType.CCDRAGON, CubePos.of(0, 0, 0), Coords.sectionToCube(radius), CubePos.of(0, 0, 0));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerChunkCache;removeRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;)V"))
    private <T> void removeCubeRegionTickets(ServerChunkCache serverChunkCache, TicketType<T> ticketType, ChunkPos pos, int radius, T argument) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            serverChunkCache.removeRegionTicket(ticketType, pos, radius, argument);
            return;
        }

        serverChunkCache.removeRegionTicket(ticketType, pos, radius, argument); // TODO: Load order?
        ((IServerChunkProvider) serverChunkCache).removeCubeRegionTicket(CCTicketType.CCDRAGON, CubePos.of(0, 0, 0), Coords.sectionToCube(radius), CubePos.of(0, 0, 0));
    }

    @Inject(method = "isArenaLoaded", at = @At("HEAD"), cancellable = true)
    private void isArenaLoaded3D(CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        for (int x = -8; x <= 8; ++x) {
            for (int y = -8; y <= 8; ++y) {
                for (int z = 8; z <= 8; ++z) {
                    ChunkAccess chunkAccess = ((ICubicWorld) this.level).getCube(x, y, z, ChunkStatus.FULL, true);
                    if (!(chunkAccess instanceof BigCube)) {
                        cir.setReturnValue(false);
                        return;
                    }

                    ChunkHolder.FullChunkStatus fullChunkStatus = ((BigCube) chunkAccess).getFullStatus();
                    if (!fullChunkStatus.isOrAfter(ChunkHolder.FullChunkStatus.BORDER)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "hasActiveExitPortal", at = @At("HEAD"), cancellable = true)
    private void hasActiveExitPortal3D(CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        for (int x = -8; x <= 8; ++x) {
            for (int y = -8; y <= 8; ++y) {
                for (int z = -8; z <= 8; ++z) {
                    BigCube bigCube = (BigCube) ((ICubicWorld) this.level).getCube(x, y, z);

                    for (BlockEntity blockEntity : bigCube.getCubeBlockEntities().values()) {
                        if (blockEntity instanceof TheEndPortalBlockEntity) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "findExitPortal", at = @At("HEAD"), cancellable = true)
    private void findExitPortal3D(CallbackInfoReturnable<BlockPattern.BlockPatternMatch> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        for (int x = -8; x <= 8; ++x) {
            for (int y = -8; y <= 8; ++y) {
                for (int z = -8; z <= 8; ++z) {
                    BigCube bigCube = (BigCube) ((ICubicWorld) this.level).getCube(x, y, z);

                    for (BlockEntity blockEntity : bigCube.getCubeBlockEntities().values()) {
                        if (blockEntity instanceof TheEndPortalBlockEntity) {
                            BlockPattern.BlockPatternMatch blockPatternMatch = this.exitPortalPattern.find(this.level, blockEntity.getBlockPos());
                            if (blockPatternMatch != null) {
                                BlockPos blockPos = blockPatternMatch.getBlock(3, 3, 3).getPos();
                                if (this.portalLocation == null && blockPos.getX() == 0 && blockPos.getZ() == 0) {
                                    this.portalLocation = blockPos;
                                }

                                cir.setReturnValue(blockPatternMatch);
                            }
                        }
                    }
                }
            }
        }
        BlockPos endPodiumLocation = EndPodiumFeature.END_PODIUM_LOCATION;

        int motionBlockingY =
            Math.min(((ICubicWorld) this.level).getCube(Coords.blockToCube(endPodiumLocation.getX()), 8, Coords.blockToCube(endPodiumLocation.getZ())).getCubePos().maxCubeY(),
                this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.END_PODIUM_LOCATION).getY());

        for (int ySearch = motionBlockingY;
             ySearch >= ((ICubicWorld) this.level).getCube(Coords.blockToCube(endPodiumLocation.getX()), -8, Coords.blockToCube(endPodiumLocation.getZ())).getCubePos().minCubeY();
             --ySearch) {

            BlockPattern.BlockPatternMatch exitPortalPattern = this.exitPortalPattern.find(this.level, new BlockPos(endPodiumLocation.getX(), ySearch, endPodiumLocation.getZ()));

            if (exitPortalPattern != null) {
                if (this.portalLocation == null) {
                    this.portalLocation = exitPortalPattern.getBlock(3, 3, 3).getPos();
                }

                cir.setReturnValue(exitPortalPattern);
            }
        }
        cir.setReturnValue(null);
    }
}
