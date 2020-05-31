package io.github.opencubicchunks.cubicchunks.network;


import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.BufferUtils;
import io.github.opencubicchunks.cubicchunks.utils.AddressTools;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PacketCubeBlockChanges {

    CubePos cubePos;
    short[] localAddresses;
    BlockState[] blockStates;

    @SuppressWarnings("deprecation")
    public PacketCubeBlockChanges(PacketBuffer in) {
        this.cubePos = CubePos.of(
                BufferUtils.readSignedVarInt(in),
                BufferUtils.readSignedVarInt(in),
                BufferUtils.readSignedVarInt(in));

        short numBlocks = in.readShort();
        localAddresses = new short[numBlocks];
        blockStates = new BlockState[numBlocks];

        for (int i = 0; i < numBlocks; i++) {
            localAddresses[i] = in.readShort();
            blockStates[i] = Block.BLOCK_STATE_IDS.getByValue(in.readVarInt());
        }
    }

    public PacketCubeBlockChanges(ICube cube, ShortList localAddresses) {
        this.cubePos = cube.getCubePos();
        this.localAddresses = localAddresses.toShortArray();
        this.blockStates = new BlockState[localAddresses.size()];
        for (int i = 0; i < localAddresses.size(); i++) {
            int localAddress = this.localAddresses[i];
            int x = AddressTools.getLocalX(localAddress);
            int y = AddressTools.getLocalY(localAddress);
            int z = AddressTools.getLocalZ(localAddress);
            this.blockStates[i] = cube.getBlockState(x, y, z);
        }
    }

    @SuppressWarnings("deprecation")
    public void encode(PacketBuffer out) {
        BufferUtils.writeSignedVarInt(out, cubePos.getX());
        BufferUtils.writeSignedVarInt(out, cubePos.getY());
        BufferUtils.writeSignedVarInt(out, cubePos.getZ());
        out.writeShort(localAddresses.length);
        for (int i = 0; i < localAddresses.length; i++) {
            out.writeShort(localAddresses[i]);
            out.writeVarInt(Block.BLOCK_STATE_IDS.get(blockStates[i]));
        }
    }

    BlockPos getPos(int i) {
        final short addr = this.localAddresses[i];
        return cubePos.asBlockPos(AddressTools.getLocalX(addr), AddressTools.getLocalY(addr), AddressTools.getLocalZ(addr));
    }

    public static class Handler {
        public static void handle(PacketCubeBlockChanges packet, World world) {
            ClientWorld worldClient = (ClientWorld) world;
            for (int i = 0; i < packet.localAddresses.length; i++) {
                worldClient.invalidateRegionAndSetBlock(packet.getPos(i), packet.blockStates[i]);
            }
        }
    }
}
