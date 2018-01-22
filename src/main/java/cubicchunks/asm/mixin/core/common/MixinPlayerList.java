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
package cubicchunks.asm.mixin.core.common;

import static cubicchunks.asm.JvmNames.CHUNK_SET_CHUNK_MODIFIED;

import cubicchunks.server.ICubicPlayerList;
import cubicchunks.server.PlayerCubeMap;
import cubicchunks.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(PlayerList.class)
public abstract class MixinPlayerList implements ICubicPlayerList {

    @Shadow private int viewDistance;

    @Shadow @Final private MinecraftServer mcServer;
    protected int verticalViewDistance = -1;

    @Redirect(method = "playerLoggedOut",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;markDirty()V", ordinal = 0),
            require = 1)
    private void setChunkModifiedOnPlayerLoggedOut(Chunk chunkIn, EntityPlayerMP playerIn) {
        WorldServer worldserver = playerIn.getServerWorld();
        if (((ICubicWorld) worldserver).isCubicWorld()) {
            ((ICubicWorld) worldserver).getCubeFromCubeCoords(playerIn.chunkCoordX, playerIn.chunkCoordY, playerIn.chunkCoordZ).markDirty();
        } else {
            worldserver.getChunkFromChunkCoords(playerIn.chunkCoordX, playerIn.chunkCoordZ).markDirty();
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

        if (this.mcServer.worlds != null) {
            for (WorldServer worldserver : this.mcServer.worlds) {
                if (worldserver != null && ((ICubicWorld) worldserver).isCubicWorld()) {
                    ((PlayerCubeMap) worldserver.getPlayerChunkMap()).setPlayerViewDistance(viewDistance, dist);
                    // TODO: entity tracker vertical view distance
                    // worldserver.getEntityTracker().setViewDistance(dist);
                }
            }
        }
    }
}
