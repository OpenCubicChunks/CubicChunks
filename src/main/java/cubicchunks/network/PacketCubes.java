/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.network;

import cubicchunks.CubicChunks;
import cubicchunks.client.CubeProviderClient;
import cubicchunks.util.CubePos;
import cubicchunks.util.PacketUtils;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PacketCubes implements IMessage {

    private CubePos[] cubePos;
    private byte[] data;
    private List<List<NBTTagCompound>> tileEntityTags;

    public PacketCubes() {
    }

    public PacketCubes(List<Cube> cubes) {
        this.cubePos = new CubePos[cubes.size()];
        for (int i = 0; i < cubes.size(); i++) {
            cubePos[i] = cubes.get(i).getCoords();
        }
        this.data = new byte[WorldEncoder.getEncodedSize(cubes)];
        PacketBuffer out = new PacketBuffer(WorldEncoder.createByteBufForWrite(this.data));

        WorldEncoder.encodeCubes(out, cubes);

        this.tileEntityTags = new ArrayList<>();

        cubes.forEach(cube ->
                tileEntityTags.add(cube.getTileEntityMap().values().stream().map(TileEntity::getUpdateTag).collect(Collectors.toList()))
        );
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int cubeCount = buf.readUnsignedShort();
        cubePos = new CubePos[cubeCount];
        for (int i = 0; i < this.cubePos.length; i++) {
            cubePos[i] = PacketUtils.readCubePos(buf);
        }

        this.data = new byte[buf.readInt()];
        buf.readBytes(this.data);

        this.tileEntityTags = new ArrayList<>();
        for (int i = 0; i < cubeCount; i++) {
            int numTiles = buf.readInt();
            List<NBTTagCompound> tags = new ArrayList<>();
            for (int j = 0; j < numTiles; j++) {
                tags.add(ByteBufUtils.readTag(buf));
            }
            this.tileEntityTags.add(tags);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(cubePos.length);
        for (CubePos pos : cubePos) {
            PacketUtils.write(buf, pos);
        }

        buf.writeInt(this.data.length);
        buf.writeBytes(this.data);

        this.tileEntityTags.forEach(tags -> {
            buf.writeInt(tags.size());
            tags.forEach(tag -> ByteBufUtils.writeTag(buf, tag));
        });
    }

    CubePos[] getCubePos() {
        return cubePos;
    }

    byte[] getData() {
        return data;
    }

    List<List<NBTTagCompound>> getTileEntityTags() {
        return this.tileEntityTags;
    }

    public static class Handler extends AbstractClientMessageHandler<PacketCubes> {

        @Nullable @Override
        public IMessage handleClientMessage(EntityPlayer player, PacketCubes message, MessageContext ctx) {
            PacketUtils.ensureMainThread(this, player, message, ctx);

            ICubicWorldClient worldClient = (ICubicWorldClient) player.getEntityWorld();
            CubeProviderClient cubeCache = worldClient.getCubeCache();

            CubePos[] cubePos = message.getCubePos();
            List<Cube> cubes = new ArrayList<>();
            for (CubePos pos : cubePos) {
                Cube cube = cubeCache.loadCube(pos); // new cube
                //isEmpty actually checks if the column is a BlankColumn
                if (cube == null) {
                    CubicChunks.LOGGER.error("Out of order cube received! No column for cube at {} exists!", pos);
                }
                cubes.add(cube);
            }


            byte[] data = message.getData();
            ByteBuf buf = WorldEncoder.createByteBufForRead(data);
            WorldEncoder.decodeCube(new PacketBuffer(buf), cubes);

            cubes.stream().filter(Objects::nonNull).forEach(Cube::markForRenderUpdate);

            message.getTileEntityTags().forEach(tags -> tags.forEach(tag -> {
                int blockX = tag.getInteger("x");
                int blockY = tag.getInteger("y");
                int blockZ = tag.getInteger("z");
                BlockPos pos = new BlockPos(blockX, blockY, blockZ);
                TileEntity tileEntity = worldClient.getTileEntity(pos);

                if (tileEntity != null) {
                    tileEntity.handleUpdateTag(tag);
                }
            }));
            return null;
        }
    }

    private static int index(int x, int z) {
        return x << 4 | z;
    }
}
