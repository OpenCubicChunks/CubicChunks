package cubicchunks.network;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.chat.IChatComponent;
import net.minecraft.main.Minecraft;
import net.minecraft.network.INetHandler;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.util.BlockPos;
import net.minecraft.util.TaskQueue;
import net.minecraft.world.DimensionOverworld;
import net.minecraft.world.WorldClient;
import cubicchunks.TallWorldsMod;
import cubicchunks.client.ClientCubeCache;
import cubicchunks.client.WorldClientContext;
import cubicchunks.util.AddressTools;
import cubicchunks.world.column.BlankColumn;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;

public class ClientHandler implements INetHandler {
	
	private static ClientHandler m_instance;
	
	public static ClientHandler getInstance() {
		if (m_instance == null) {
			m_instance = new ClientHandler();
		}
		return m_instance;
	}
	
	@Override
	public void onDisconnect(IChatComponent chat) {
		// nothing to do
	}
	
	public void handle(final PacketBulkCubeData packet) {
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		packet.startDecoding();
		
		// load columns first
		for (long columnAddress : packet.columnAddresses) {
			Column column = cubeCache.loadChunk(AddressTools.getX(columnAddress), AddressTools.getZ(columnAddress));
			packet.decodeNextColumn(column);
		}
		
		// then cubes
		int outOfOrderCubes = 0;
		for (long cubeAddress : packet.cubeAddresses) {
			int cubeX = AddressTools.getX(cubeAddress);
			int cubeY = AddressTools.getY(cubeAddress);
			int cubeZ = AddressTools.getZ(cubeAddress);
			Column column = cubeCache.getColumn(cubeX, cubeZ);
			if (column instanceof BlankColumn) {
				outOfOrderCubes++;
				
				// still need to read the cube though
				packet.decodeNextCube(new Cube(worldClient, column, 0, 0, 0, false));
				
				continue;
			}
			Cube cube = column.getOrCreateCube(cubeY, false);
			packet.decodeNextCube(cube);
			cube.markForRenderUpdate();
		}
		if (outOfOrderCubes > 0) {
			TallWorldsMod.log.error("Ignored {}/{} cubes that arrived before their columns", outOfOrderCubes, packet.cubeAddresses.length);
		}
		
		packet.finishDecoding();
		
		// update column metadata
		for (long columnAddress : packet.columnAddresses) {
			Column column = cubeCache.getColumn(AddressTools.getX(columnAddress), AddressTools.getZ(columnAddress));
			
			// update lighting flags
			if (! (worldClient.dimension instanceof DimensionOverworld)) {
				column.resetRelightChecks();
			}
			column.terrainPopulated = true;
			
			// update tile entities in each chunk
			for (Cube cube : column.getCubes()) {
				for (BlockEntity blockEntity : cube.getBlockEntities()) {
					blockEntity.updateContainingBlockInfo();
				}
			}
		}
	}
	
	public void handle(final PacketUnloadCubes packet) {
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		for (long cubeAddress : packet.cubeAddresses) {
			cubeCache.unloadCube(
				AddressTools.getX(cubeAddress),
				AddressTools.getY(cubeAddress),
				AddressTools.getZ(cubeAddress)
			);
		}
	}
	
	public void handle(final PacketCubeChange packet) {
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		// get the cube
		int cubeX = AddressTools.getX(packet.cubeAddress);
		int cubeY = AddressTools.getY(packet.cubeAddress);
		int cubeZ = AddressTools.getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			TallWorldsMod.log.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}
		
		// apply the update
		packet.decodeCube(cube);
		cube.markForRenderUpdate();
		for (BlockEntity blockEntity : cube.getBlockEntities()) {
			blockEntity.updateContainingBlockInfo();
		}
	}
	
	public void handle(final PacketCubeBlockChange packet) {
		
		// TODO: build better network system in M3L
		TaskQueue taskQueue = Minecraft.getMinecraft();
		if (!taskQueue.isInMainThread()) {
			taskQueue.runTask(new Runnable() {
				@Override
				public void run() {
					handle(packet);
				}
			});
			throw ThreadQuickExitException.INSTANCE;
		}
		
		WorldClient worldClient = Minecraft.getMinecraft().levelClient;
		WorldClientContext context = WorldClientContext.get(worldClient);
		ClientCubeCache cubeCache = context.getCubeCache();
		
		// get the cube
		int cubeX = AddressTools.getX(packet.cubeAddress);
		int cubeY = AddressTools.getY(packet.cubeAddress);
		int cubeZ = AddressTools.getZ(packet.cubeAddress);
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		if (cube instanceof BlankCube) {
			TallWorldsMod.log.error("Ignored update to blank cube ({},{},{})", cubeX, cubeY, cubeZ);
			return;
		}
		
		// apply the update
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i=0; i<packet.localAddresses.length; i++) {
			cube.setBlockState(cube.localAddressToBlockPos(pos, packet.localAddresses[i]), packet.blockStates[i]);
		}
		cube.markForRenderUpdate();
		for (BlockEntity blockEntity : cube.getBlockEntities()) {
			blockEntity.updateContainingBlockInfo();
		}	
	}
}
