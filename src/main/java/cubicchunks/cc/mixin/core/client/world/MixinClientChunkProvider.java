package cubicchunks.cc.mixin.core.client.world;

import cubicchunks.cc.chunk.IClientCubeProvider;
import cubicchunks.cc.chunk.IColumn;
import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.util.CubePos;
import cubicchunks.cc.mixin.core.client.interfaces.IClientChunkProviderChunkArray;
import cubicchunks.cc.utils.Coords;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.lighting.WorldLightManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;

import javax.annotation.Nullable;

@Mixin(ClientChunkProvider.class)
public abstract class MixinClientChunkProvider implements IClientCubeProvider {
    private static final Biome[] BIOMES = Util.make(new Biome[BiomeContainer.BIOMES_SIZE], (array) ->
            Arrays.fill(array, Biomes.PLAINS));

    @Shadow private volatile ClientChunkProvider.ChunkArray array;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private static boolean isValid(@Nullable Chunk chunkIn, int x, int z) {
        throw new Error("Mixin did not apply");
    }

    @Shadow @Final private ClientWorld world;

    @Shadow public abstract WorldLightManager getLightManager();

    @SuppressWarnings("ConstantConditions") @Override
    public Chunk loadCube(int cubeX, int cubeY, int cubeZ, @Nullable BiomeContainer biomes, PacketBuffer readBuffer, CompoundNBT nbtTagIn, boolean sectionExists) {
        {
            int emptyFlags = readBuffer.readUnsignedByte();
            for (int i = 0; i < ICube.CUBESIZE; i++) {
                boolean exists = ((emptyFlags >>> i) & 1) != 0;

                //        byte emptyFlags = 0;
                //        for (int i = 0; i < sections.length; i++) {
                //            if (sections[i] != null && !sections[i].isEmpty()) {
                //                emptyFlags |= 1 << i;
                //            }
                //        }
                //        buf.writeByte(emptyFlags);
                //        for (int i = 0; i < sections.length; i++) {
                //            if (sections[i] != null && !sections[i].isEmpty()) {
                //                sections[i].write(buf);
                //            }
                //        }
                //        return false;

                int dx = Coords.indexTo32X(i);
                int dy = Coords.indexTo32Y(i);
                int dz = Coords.indexTo32Z(i);

                SectionPos sectionPos = CubePos.of(cubeX, cubeY, cubeZ).asSectionPos();
                int x = sectionPos.getX() + dx;
                int y = sectionPos.getY() + dy;
                int z = sectionPos.getZ() + dz;
                Chunk chunk;
                int idx;
                if (!((IClientChunkProviderChunkArray) (Object) this.array).invokeInView(x, z)) {
                    LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", x, z);
                    chunk = null;
                    idx = -1;
                } else {
                    idx = ((IClientChunkProviderChunkArray) (Object) this.array).invokeGetIndex(x, z);
                    chunk = ((IClientChunkProviderChunkArray) (Object) this.array).getChunks().get(idx);
                }
                if (!isValid(chunk, x, z)) {
                    //if (biomes == null) {
                    //    LOGGER.warn("Ignoring chunk since we don't have complete data: {}, {}", sectionZ, sectionZ);
                    //    return null;
                    //}

                    chunk = new Chunk(this.world, new ChunkPos(x, z), new BiomeContainer(BIOMES));
                    ((IColumn) chunk).readSection(y, null, readBuffer, nbtTagIn, exists);
                    if(idx >= 0) {
                        ((IClientChunkProviderChunkArray) (Object) this.array).invokeReplace(idx, chunk);
                    }
                } else {
                    ((IColumn) chunk).readSection(y, null, readBuffer, nbtTagIn, exists);
                }

                ChunkSection[] allSections = chunk.getSections();
                WorldLightManager lightManager = this.getLightManager();
                lightManager.enableLightSources(new ChunkPos(x, z), true);

                ChunkSection chunksection = allSections[cubeY];
                lightManager.updateSectionStatus(sectionPos, ChunkSection.isEmpty(chunksection));

                this.world.onChunkLoaded(x, z);
                // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
            }
            return null;
        }
    }
}
