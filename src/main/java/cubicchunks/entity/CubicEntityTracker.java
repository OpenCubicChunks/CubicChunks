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

import com.google.common.collect.Lists;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityAttach;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.util.ReportedException;
import net.minecraft.world.WorldServer;

import java.util.List;

public class CubicEntityTracker extends EntityTracker {

    public CubicEntityTracker(ICubicWorldServer worldServer) {
        super((WorldServer) worldServer);
    }

    public void sendLeashedEntitiesInCube(EntityPlayerMP player, Cube cubeIn) {
        List<Entity> list = Lists.<Entity>newArrayList();
        List<Entity> list1 = Lists.<Entity>newArrayList();

        for (EntityTrackerEntry entitytrackerentry : this.entries) {

            Entity entity = entitytrackerentry.getTrackedEntity();
            if (entity != player &&
                    entity.chunkCoordX == cubeIn.getX() &&
                    entity.chunkCoordZ == cubeIn.getZ() &&
                    entity.chunkCoordY == cubeIn.getY()) {

                entitytrackerentry.updatePlayerEntity(player);
                if (entity instanceof EntityLiving && ((EntityLiving) entity).getLeashedToEntity() != null) {
                    list.add(entity);
                }

                if (!entity.getPassengers().isEmpty()) {
                    list1.add(entity);
                }
            }
        }

        if (!list.isEmpty()) {
            for (Entity entity1 : list) {
                player.connection.sendPacket(new SPacketEntityAttach(entity1, ((EntityLiving) entity1).getLeashedToEntity()));
            }
        }

        if (!list1.isEmpty()) {
            for (Entity entity2 : list1) {
                player.connection.sendPacket(new SPacketSetPassengers(entity2));
            }
        }
    }

    @Override
    public void track(Entity entityIn, int trackingRange, final int updateFrequency, boolean sendVelocityUpdates) {
        try {
            if (this.trackedEntityHashTable.containsItem(entityIn.getEntityId())) {
                throw new IllegalStateException("Entity is already tracked!");
            }
            EntityTrackerEntry entitytrackerentry =
                    new CubicEntityTrackerEntry(entityIn, trackingRange, this.maxTrackingDistanceThreshold, updateFrequency, sendVelocityUpdates);
            this.entries.add(entitytrackerentry);
            this.trackedEntityHashTable.addKey(entityIn.getEntityId(), entitytrackerentry);
            entitytrackerentry.updatePlayerEntities(this.world.playerEntities);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Adding entity to track");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity To Track");
            crashreportcategory.addCrashSection("Tracking range", trackingRange + " blocks");
            crashreportcategory.addDetail("Update interval", new ICrashReportDetail<String>() {
                public String call() throws Exception {
                    String s = "Once per " + updateFrequency + " ticks";
                    if (updateFrequency == Integer.MAX_VALUE) {
                        s = "Maximum (" + s + ")";
                    }
                    return s;
                }
            });
            entityIn.addEntityCrashInfo(crashreportcategory);
            ((EntityTrackerEntry) this.trackedEntityHashTable.lookup(entityIn.getEntityId())).getTrackedEntity()
                    .addEntityCrashInfo(crashreport.makeCategory("Entity That Is Already Tracked"));
            try {
                throw new ReportedException(crashreport);
            } catch (ReportedException reportedexception) {
                LOGGER.error((String) "\"Silently\" catching entity tracking error.", (Throwable) reportedexception);
            }
        }
    }
}
