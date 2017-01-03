package cubichunks.entity;

import cubicchunks.world.cube.Cube;
import net.minecraft.entity.player.EntityPlayerMP;

public interface ICubicEntityTracker {

	public void sendLeashedEntitiesInCube(EntityPlayerMP player, Cube cubeIn);
}
