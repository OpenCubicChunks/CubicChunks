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
package cubicchunks.entity;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;

public class CubicEntityTrackerEntry extends EntityTrackerEntry {

    public CubicEntityTrackerEntry(Entity entityIn, int rangeIn, int maxRangeIn, int updateFrequencyIn,
            boolean sendVelocityUpdatesIn) {
        super(entityIn, rangeIn, maxRangeIn, updateFrequencyIn, sendVelocityUpdatesIn);
    }

    @Override
    public boolean isVisibleTo(EntityPlayerMP playerMP) {
        double dx = playerMP.posX - (double) this.encodedPosX / 4096.0D;
        double dz = playerMP.posZ - (double) this.encodedPosZ / 4096.0D;
        double dy = playerMP.posY - (double) this.encodedPosY / 4096.0D;
        int range = Math.min(this.range, this.maxRange);

        return dx >= -range && dx <= range &&
                dz >= -range && dz <= range &&
                dy >= -range && dy <= range &&
                this.trackedEntity.isSpectatedByPlayer(playerMP);
    }

    @Override
    protected boolean isPlayerWatchingThisChunk(EntityPlayerMP player) {
        return ((ICubicWorldServer) player.getServerWorld()).getPlayerCubeMap()
                .isPlayerWatchingCube(player, this.trackedEntity.chunkCoordX, this.trackedEntity.chunkCoordY, this.trackedEntity.chunkCoordZ);
    }

    // Workaround to fix invisible entities bug
    public void sendSpawnPacket(EntityPlayerMP playerMP) {
        this.trackingPlayers.remove(playerMP);
        this.updatePlayerEntity(playerMP);
    }
}
