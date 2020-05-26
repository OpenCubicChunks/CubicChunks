package cubicchunks.cc.network;


import static cubicchunks.cc.utils.AddressTools.getLocalX;
import static cubicchunks.cc.utils.AddressTools.getLocalY;
import static cubicchunks.cc.utils.AddressTools.getLocalZ;

import cubicchunks.cc.utils.AddressTools;
import cubicchunks.cc.utils.BufferUtils;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SMultiBlockChangePacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;

public class PacketSectionBlockChanges {

    SectionPos sectionPos;
    short[] localAddresses;
    BlockState[] blockStates;

    @SuppressWarnings("deprecation")
    public PacketSectionBlockChanges(PacketBuffer in) {
        this.sectionPos = SectionPos.of(
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

    public PacketSectionBlockChanges(ChunkSection section, SectionPos pos, ShortList localAddresses) {
        this.sectionPos = pos;
        this.localAddresses = localAddresses.toShortArray();
        this.blockStates = new BlockState[localAddresses.size()];
        for (int i = 0; i < localAddresses.size(); i++) {
            int localAddress = this.localAddresses[i];
            int x = getLocalX(localAddress);
            int y = getLocalY(localAddress);
            int z = getLocalZ(localAddress);
            this.blockStates[i] = section.getBlockState(x, y, z);
        }
    }

    @SuppressWarnings("deprecation")
    public void encode(PacketBuffer out) {
        BufferUtils.writeSignedVarInt(out, sectionPos.getX());
        BufferUtils.writeSignedVarInt(out, sectionPos.getY());
        BufferUtils.writeSignedVarInt(out, sectionPos.getZ());
        out.writeShort(localAddresses.length);
        for (int i = 0; i < localAddresses.length; i++) {
            out.writeShort(localAddresses[i]);
            out.writeVarInt(Block.BLOCK_STATE_IDS.get(blockStates[i]));
        }
    }

    BlockPos getPos(int i) {
        final short addr = this.localAddresses[i];
        return new BlockPos(sectionPos.asBlockPos().add(getLocalX(addr), getLocalY(addr), getLocalZ(addr)));
    }

    public static class Handler {
        public static void handle(PacketSectionBlockChanges packet, World world) {
            ClientWorld worldClient = (ClientWorld) world;
            for (int i = 0; i < packet.localAddresses.length; i++) {
                worldClient.invalidateRegionAndSetBlock(packet.getPos(i), packet.blockStates[i]);
            }
        }
    }
}
