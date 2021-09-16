package io.github.opencubicchunks.cubicchunks.network;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class PacketUpdateLight {

    private final List<byte[]> skyLightData;
    private final List<byte[]> blockLightData;

    private final BitSet dataExists;

    private final CubePos cubePos;
    private final boolean lightFlag;

    PacketUpdateLight(FriendlyByteBuf buf) {
        this.lightFlag = buf.readBoolean();
        this.cubePos = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());

        int dataByteCount = MathUtil.ceilDiv(CubeAccess.SECTION_COUNT * 2, 8);
        this.dataExists = BitSet.valueOf(buf.readByteArray(dataByteCount));

        this.skyLightData = new ArrayList<>();
        int skyLightDataSize = buf.readInt();
        for (int i = 0; i < skyLightDataSize; i++) {
            this.skyLightData.add(buf.readByteArray(2048));
        }

        this.blockLightData = new ArrayList<>();
        int blockLightDataSize = buf.readInt();
        for (int i = 0; i < blockLightDataSize; i++) {
            this.blockLightData.add(buf.readByteArray(2048));
        }
    }

    public PacketUpdateLight(CubePos pos, LevelLightEngine lightEngine, boolean lightFlag) {
        this.cubePos = pos;
        this.lightFlag = lightFlag;
        this.skyLightData = Lists.newArrayList();
        this.blockLightData = Lists.newArrayList();

        this.dataExists = new BitSet(CubeAccess.SECTION_COUNT * 2);

        for (int i = 0; i < CubeAccess.SECTION_COUNT; ++i) {
            DataLayer skyLayer = lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            DataLayer blockLayer = lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(Coords.sectionPosByIndex(pos, i));
            if (skyLayer != null) {
                if (!skyLayer.isEmpty()) {
                    this.dataExists.set(i * 2);
                    this.skyLightData.add(skyLayer.getData().clone());
                }
            }
            if (blockLayer != null) {
                if (!blockLayer.isEmpty()) {
                    this.dataExists.set(i * 2 + 1);
                    this.blockLightData.add(blockLayer.getData().clone());
                }
            }
        }
    }


    void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(lightFlag);
        buf.writeInt(this.cubePos.getX());
        buf.writeInt(this.cubePos.getY());
        buf.writeInt(this.cubePos.getZ());

        byte[] byteArray = new byte[MathUtil.ceilDiv(CubeAccess.SECTION_COUNT * 2, 8)];
        byte[] byteArray2 = dataExists.toByteArray();

        System.arraycopy(byteArray2, 0, byteArray, 0, Math.min(byteArray.length, byteArray2.length));

        buf.writeByteArray(byteArray);

        buf.writeInt(this.skyLightData.size());
        for (byte[] array : this.skyLightData) {
            buf.writeByteArray(array);
        }
        buf.writeInt(this.blockLightData.size());
        for (byte[] array : this.blockLightData) {
            buf.writeByteArray(array);
        }
    }

    public static class Handler {
        public static void handle(PacketUpdateLight packet, Level level) {
            if (!(level instanceof ClientLevel clientLevel)) {
                throw new Error("PacketUpdateLight handle called on server");
            }
            LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();

            Iterator<byte[]> skyIterator = packet.skyLightData.iterator();
            Iterator<byte[]> blockIterator = packet.blockLightData.iterator();

            for (int i = 0; i < CubeAccess.SECTION_COUNT; ++i) {
                SectionPos sectionPos = Coords.sectionPosByIndex(packet.cubePos, i);

                if (packet.dataExists.get(i * 2)) {
                    lightEngine.queueSectionData(LightLayer.SKY, sectionPos, new DataLayer(skyIterator.next()), packet.lightFlag);
                    clientLevel.setSectionDirtyWithNeighbors(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
                }
                if (packet.dataExists.get(i * 2 + 1)) {
                    lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, new DataLayer(blockIterator.next()), packet.lightFlag);
                    clientLevel.setSectionDirtyWithNeighbors(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
                }
            }
        }
    }
}