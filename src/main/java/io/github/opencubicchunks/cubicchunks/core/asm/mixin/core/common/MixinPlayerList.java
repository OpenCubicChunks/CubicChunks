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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.core.entity.ICubicEntityTracker;
import io.github.opencubicchunks.cubicchunks.core.server.ICubicPlayerList;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(PlayerList.class)
public abstract class MixinPlayerList implements ICubicPlayerList {

    @Shadow private int viewDistance;

    @Shadow @Final private MinecraftServer server;
    protected int verticalViewDistance = -1;

    @Redirect(method = "playerLoggedOut",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;markDirty()V", ordinal = 0),
            require = 1)
    private void setChunkModifiedOnPlayerLoggedOut(Chunk chunkIn, EntityPlayerMP playerIn) {
        ICubicWorldInternal world = (ICubicWorldInternal) playerIn.getServerWorld();
        if (world.isCubicWorld()) {
            world.getCubeFromCubeCoords(playerIn.chunkCoordX, playerIn.chunkCoordY, playerIn.chunkCoordZ).markDirty();
        } else {
            ((World) world).getChunk(playerIn.chunkCoordX, playerIn.chunkCoordZ).markDirty();
        }
    }

    @Override public int getVerticalViewDistance() {
        return verticalViewDistance < 0 ? viewDistance : verticalViewDistance;
    }

    @Override public int getRawVerticalViewDistance() {
        return verticalViewDistance;
    }

    @Override public void setVerticalViewDistance(int dist) {
        this.verticalViewDistance = dist;

        if (this.server.worlds != null) {
            for (WorldServer worldserver : this.server.worlds) {
                if (worldserver != null && ((ICubicWorld) worldserver).isCubicWorld()) {
                    ((PlayerCubeMap) worldserver.getPlayerChunkMap()).setPlayerViewDistance(viewDistance, dist);
                    ((ICubicEntityTracker) worldserver.getEntityTracker()).setVertViewDistance(dist);
                }
            }
        }
    }

    @Inject(method = "recreatePlayerEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkProviderServer;provideChunk(II)Lnet/minecraft/world/chunk/Chunk;"))
    private void createPlayerChunk(EntityPlayerMP playerIn, int dimension, boolean conqueredEnd, CallbackInfoReturnable<EntityPlayerMP> cir) {
        if (!((ICubicWorld) playerIn.world).isCubicWorld()) {
            return;
        }
        for (int dCubeY = -8; dCubeY <= 8; dCubeY++) {
            ((ICubicWorld) playerIn.world).getCubeFromBlockCoords(playerIn.getPosition().up(Coords.cubeToMinBlock(dCubeY)));
        }
    }

    @ModifyConstant(method = "recreatePlayerEntity",
            constant = @Constant(doubleValue = 256))
    private double getMaxHeight(double _256, EntityPlayerMP playerIn, int dimension, boolean conqueredEnd) {
        // +/- 8 chunks around the original position are loaded because of an inject above
        if (!playerIn.world.isBlockLoaded(new BlockPos(playerIn))) {
            return Double.NEGATIVE_INFINITY;
        }
        return ((ICubicWorld) playerIn.world).getMaxHeight();
    }
}
