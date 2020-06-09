package io.github.opencubicchunks.cubicchunks.network;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.WorldLightManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class PacketUpdateLight {

    private final List<byte[]> skyLightData;
    private final List<byte[]> blockLightData;

    private final BitSet dataExists;

    private final CubePos cubePos;

    public PacketUpdateLight(CubePos pos, WorldLightManager lightManager) {
        this.cubePos = pos;
        this.skyLightData = Lists.newArrayList();
        this.blockLightData = Lists.newArrayList();

        this.dataExists = new BitSet(ICube.CUBE_SIZE*2);

        for(int i = 0; i < ICube.CUBE_SIZE; ++i) {
            NibbleArray skyNibbleArray = lightManager.getLightEngine(LightType.SKY).getData(Coords.sectionPosByIndex(pos, i));
            NibbleArray blockNibbleArray = lightManager.getLightEngine(LightType.BLOCK).getData(Coords.sectionPosByIndex(pos, i));
            if (skyNibbleArray != null) {
                if (!skyNibbleArray.isEmpty()) {
                    this.dataExists.set(i*2);
                    this.skyLightData.add(skyNibbleArray.getData().clone());
                }
            }
            if (blockNibbleArray != null) {
                if (!blockNibbleArray.isEmpty()) {
                    this.dataExists.set(i*2 + 1);
                    this.blockLightData.add(blockNibbleArray.getData().clone());
                }
            }
        }
    }

    PacketUpdateLight(PacketBuffer buf)
    {
        this.cubePos = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());

        int dataByteCount = MathUtil.ceilDiv(ICube.CUBE_SIZE*2, 8);
        this.dataExists = BitSet.valueOf(buf.readByteArray(dataByteCount));

        this.skyLightData = new ArrayList<>();
        int skyLightDataSize = buf.readInt();
        for(int i = 0; i < skyLightDataSize; i++) {
            this.skyLightData.add(buf.readByteArray(2048));
        }

        this.blockLightData = new ArrayList<>();
        int blockLightDataSize = buf.readInt();
        for(int i = 0; i < blockLightDataSize; i++) {
            this.blockLightData.add(buf.readByteArray(2048));
        }
    }


    void encode(PacketBuffer buf) {
        buf.writeInt(this.cubePos.getX());
        buf.writeInt(this.cubePos.getY());
        buf.writeInt(this.cubePos.getZ());

        byte[] byteArray = new byte[MathUtil.ceilDiv(ICube.CUBE_SIZE*2, 8)];
        byte[] byteArray2 = dataExists.toByteArray();

        System.arraycopy(byteArray2, 0, byteArray, 0, Math.min(byteArray.length, byteArray2.length));

        buf.writeByteArray(byteArray);

        buf.writeInt(this.skyLightData.size());
        for(byte[] array : this.skyLightData) {
            buf.writeByteArray(array);
        }
        buf.writeInt(this.blockLightData.size());
        for(byte[] array : this.blockLightData) {
            buf.writeByteArray(array);
        }
    }

    public static class Handler {
        public static void handle(PacketUpdateLight packet, World worldIn) {
            if(!(worldIn instanceof ClientWorld))
                throw new Error("PacketUpdateLight handle called on server");

            WorldLightManager worldlightmanager = worldIn.getChunkProvider().getLightManager();

            Iterator<byte[]> skyIterator = packet.skyLightData.iterator();
            Iterator<byte[]> blockIterator = packet.blockLightData.iterator();

            for(int i = 0; i < ICube.CUBE_SIZE; ++i) {
                SectionPos sectionPos = SectionPos.of(
                        packet.cubePos.getX() + Coords.indexToX(i),
                        packet.cubePos.getY() + Coords.indexToY(i),
                        packet.cubePos.getZ() + Coords.indexToZ(i)
                );

                if(packet.dataExists.get(i * 2)) {
                    worldlightmanager.setData(LightType.SKY, sectionPos, new NibbleArray(skyIterator.next()));
                    ((ClientWorld)worldIn).markSurroundingsForRerender(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
                }
                if(packet.dataExists.get(i * 2 + 1)) {
                    worldlightmanager.setData(LightType.BLOCK, sectionPos, new NibbleArray(blockIterator.next()));
                    ((ClientWorld)worldIn).markSurroundingsForRerender(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
                }
            }
        }
    }
}
