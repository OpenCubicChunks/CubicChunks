package io.github.opencubicchunks.cubicchunks.network;


import io.github.opencubicchunks.cubicchunks.utils.BufferUtils;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PacketCubeBlockChanges {

    private final short[] localAddresses;
    private final BlockState[] blockStates;
    private final SectionPos sectionPos;

    public PacketCubeBlockChanges(FriendlyByteBuf in) {
        this.sectionPos = SectionPos.of(
            BufferUtils.readSignedVarInt(in),
            BufferUtils.readSignedVarInt(in),
            BufferUtils.readSignedVarInt(in));

        short numBlocks = in.readShort();
        localAddresses = new short[numBlocks];
        blockStates = new BlockState[numBlocks];

        for (int i = 0; i < numBlocks; i++) {
            localAddresses[i] = in.readShort();
            blockStates[i] = Block.BLOCK_STATE_REGISTRY.byId(in.readVarInt());
        }
    }

    public PacketCubeBlockChanges(CubeAccess cube, SectionPos sectionPos, ShortList localAddresses) {
        this.sectionPos = sectionPos;
        this.localAddresses = localAddresses.toShortArray();
        this.blockStates = new BlockState[localAddresses.size()];
        for (int i = 0; i < localAddresses.size(); i++) {
            short localAddress = this.localAddresses[i];
            BlockPos changedPos = sectionPos.relativeToBlockPos(localAddress);
            this.blockStates[i] = cube.getBlockState(changedPos);
        }
    }

    public void encode(FriendlyByteBuf out) {
        BufferUtils.writeSignedVarInt(out, this.sectionPos.getX());
        BufferUtils.writeSignedVarInt(out, this.sectionPos.getY());
        BufferUtils.writeSignedVarInt(out, this.sectionPos.getZ());
        out.writeShort(localAddresses.length);
        for (int i = 0; i < localAddresses.length; i++) {
            out.writeShort(localAddresses[i]);
            out.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(blockStates[i]));
        }
    }

    BlockPos getPos(int i) {
        final short addr = this.localAddresses[i];
        return sectionPos.relativeToBlockPos(addr);
    }

    public static class Handler {
        public static void handle(PacketCubeBlockChanges packet, Level level) {
            ClientLevel clientLevel = (ClientLevel) level;
            for (int i = 0; i < packet.localAddresses.length; i++) {
                clientLevel.setKnownState(packet.getPos(i), packet.blockStates[i]);
            }
        }
    }
}