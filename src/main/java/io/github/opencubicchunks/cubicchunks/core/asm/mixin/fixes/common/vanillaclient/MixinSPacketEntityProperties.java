/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
