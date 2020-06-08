package io.github.opencubicchunks.cubicchunks.network;

import com.google.common.collect.Lists;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.WorldLightManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PacketUpdateLight {

    private final List<byte[]> skyLightData;
    private final List<byte[]> blockLightData;

    private final CubePos cubePos;

    public PacketUpdateLight(CubePos pos, WorldLightManager lightManager) {
        this.cubePos = pos;
        this.skyLightData = Lists.newArrayList();
        this.blockLightData = Lists.newArrayList();

        for(int i = 0; i < ICube.CUBE_SIZE; ++i) {
            NibbleArray skyNibbleArray = lightManager.getLightEngine(LightType.SKY).getData(Coords.sectionPosByIndex(pos, i)); //vanilla has these offset by -1,
            NibbleArray blockNibbleArray = lightManager.getLightEngine(LightType.BLOCK).getData(Coords.sectionPosByIndex(pos, i)); // should we too?
            if (skyNibbleArray != null) {
                if (!skyNibbleArray.isEmpty()) {
                    this.skyLightData.add(skyNibbleArray.getData().clone());
                }
            }
            if (blockNibbleArray != null) {
                if (!blockNibbleArray.isEmpty()) {
                    this.blockLightData.add(blockNibbleArray.getData().clone());
                }
            }
        }
    }

    PacketUpdateLight(PacketBuffer buf)
    {
        this.cubePos = CubePos.of(buf.readInt(), buf.readInt(), buf.readInt());

        this.skyLightData = new ArrayList<>();
        int skyLightDataSize = buf.readInt();
        for(int i = 0; i < skyLightDataSize; i++) {
            int arraySize = buf.readInt();
            this.skyLightData.add(buf.readByteArray(arraySize));
        }

        this.blockLightData = new ArrayList<>();
        int blockLightDataSize = buf.readInt();
        for(int i = 0; i < blockLightDataSize; i++) {
            int arraySize = buf.readInt();
            this.blockLightData.add(buf.readByteArray(arraySize));
        }
    }


    void encode(PacketBuffer buf) {
        buf.writeInt(this.cubePos.getX());
        buf.writeInt(this.cubePos.getY());
        buf.writeInt(this.cubePos.getZ());

        buf.writeInt(this.skyLightData.size());
        for(byte[] array : this.skyLightData) {
            buf.writeInt(array.length);
            buf.writeByteArray(array);
        }
        buf.writeInt(this.blockLightData.size());
        for(byte[] array : this.blockLightData) {
            buf.writeInt(array.length);
            buf.writeByteArray(array);
        }
    }

    public static class Handler {
        public static void handle(PacketUpdateLight packet, World worldIn) {
            if(!(worldIn instanceof ClientWorld))
                throw new Error("PacketUpdateLight handle called on server");

            WorldLightManager worldlightmanager = worldIn.getChunkProvider().getLightManager();

            Iterator<byte[]> skyIterator = packet.skyLightData.iterator();
            if(skyIterator.hasNext())
                Handler.setLightData((ClientWorld)worldIn, packet.cubePos, worldlightmanager, LightType.SKY, skyIterator);

            Iterator<byte[]> blockIterator = packet.blockLightData.iterator();
            if(blockIterator.hasNext())
                Handler.setLightData((ClientWorld)worldIn, packet.cubePos, worldlightmanager, LightType.BLOCK, blockIterator);
        }

        private static void setLightData(ClientWorld worldIn, CubePos cubePos, WorldLightManager lightManager, LightType type, Iterator<byte[]> dataIn) {
            for(int i = 0; i < ICube.CUBE_SIZE; ++i) {
                SectionPos sectionPos = SectionPos.of(
                        cubePos.getX() + Coords.indexToX(i),
                        cubePos.getY() + Coords.indexToY(i),
                        cubePos.getZ() + Coords.indexToZ(i)
                );

                lightManager.setData(type, sectionPos,
                        new NibbleArray(dataIn.next().clone()));
                worldIn.markSurroundingsForRerender(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());
            }

        }
    }
}
