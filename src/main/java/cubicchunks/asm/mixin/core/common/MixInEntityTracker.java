package cubicchunks.asm.mixin.core.common;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cubicchunks.world.cube.Cube;
import cubichunks.entity.ICubicEntityTracker;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityAttach;
import net.minecraft.network.play.server.SPacketSetPassengers;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(EntityTracker.class)
public abstract class MixInEntityTracker implements ICubicEntityTracker {

	@Shadow @Final @Mutable public Set<EntityTrackerEntry> entries = Sets.<EntityTrackerEntry>newHashSet();

	public void sendLeashedEntitiesInCube(EntityPlayerMP player, Cube cubeIn)
	{
        List<Entity> list = Lists.<Entity>newArrayList();
        List<Entity> list1 = Lists.<Entity>newArrayList();

        for (EntityTrackerEntry entitytrackerentry : this.entries)
        {
            Entity entity = entitytrackerentry.getTrackedEntity();

            if (entity != player && 
            		entity.chunkCoordX == cubeIn.getX() && 
            		entity.chunkCoordZ == cubeIn.getZ() &&
            		entity.chunkCoordY == cubeIn.getY())
            {
                entitytrackerentry.updatePlayerEntity(player);

                if (entity instanceof EntityLiving && ((EntityLiving)entity).getLeashedToEntity() != null)
                {
                    list.add(entity);
                }

                if (!entity.getPassengers().isEmpty())
                {
                    list1.add(entity);
                }
            }
        }

        if (!list.isEmpty())
        {
            for (Entity entity1 : list)
            {
                player.connection.sendPacket(new SPacketEntityAttach(entity1, ((EntityLiving)entity1).getLeashedToEntity()));
            }
        }

        if (!list1.isEmpty())
        {
            for (Entity entity2 : list1)
            {
                player.connection.sendPacket(new SPacketSetPassengers(entity2));
            }
        }
	}

}
