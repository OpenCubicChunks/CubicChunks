package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.vanillaclient;

import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.server.vanillaproxy.IVanillaEncodingPacket;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketEntityProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.List;

@Mixin(SPacketEntityProperties.class)
public abstract class MixinSPacketEntityProperties implements IVanillaEncodingPacket {
    @Shadow
    @Final
    private List<SPacketEntityProperties.Snapshot> snapshots;
    @Shadow
    private int entityId;

    @Override
    public void writeVanillaPacketData(PacketBuffer buf, EntityPlayerMP player) throws IOException {
        if (VanillaNetworkHandler.hasFML(player)) {
            this.writePacketData(buf); //forge players can accept all entity data
        }

        //the player is vanilla, don't send what would be unknown properties

        buf.writeVarInt(this.entityId);

        int sizeIndex = buf.writerIndex();
        buf.writeInt(-1); //we don't know the actual size until we've iterated through the properties

        int writtenCount = 0;
        for (SPacketEntityProperties.Snapshot snapshot : this.snapshots) {
            if (!VanillaNetworkHandler.isVanillaEntityProperty(snapshot.getName())) { //property isn't vanilla, skip it
                continue;
            }

            //encode property as normal
            buf.writeString(snapshot.getName());
            buf.writeDouble(snapshot.getBaseValue());
            buf.writeVarInt(snapshot.getModifiers().size());
            for (AttributeModifier modifier : snapshot.getModifiers()) {
                buf.writeUniqueId(modifier.getID());
                buf.writeDouble(modifier.getAmount());
                buf.writeByte(modifier.getOperation());
            }

            writtenCount++;
        }

        buf.setInt(sizeIndex, writtenCount); //set property count now that we know it
    }

    @Shadow
    public abstract void writePacketData(PacketBuffer buf) throws IOException;
}
