package cubicchunks.cc.mixin.core.client.world;

import cubicchunks.cc.chunk.IClientCubeProvider;
import cubicchunks.cc.chunk.IColumn;
import cubicchunks.cc.mixin.core.client.interfaces.IClientChunkProviderChunkArray;
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
    public Chunk loadCube(int sectionX, int sectionY, int sectionZ, @Nullable BiomeContainer biomes, PacketBuffer readBuffer, CompoundNBT nbtTagIn, boolean sectionExists) {
        if (!((IClientChunkProviderChunkArray) (Object) this.array).invokeInView(sectionX, sectionZ)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", sectionX, sectionZ);
            return null;
        } else {
            int idx = ((IClientChunkProviderChunkArray) (Object) this.array).invokeGetIndex(sectionX, sectionZ);
            Chunk chunk = ((IClientChunkProviderChunkArray) (Object) this.array).getChunks().get(idx);
            if (!isValid(chunk, sectionX, sectionZ)) {
                //if (biomes == null) {
                //    LOGGER.warn("Ignoring chunk since we don't have complete data: {}, {}", sectionZ, sectionZ);
                //    return null;
                //}

                chunk = new Chunk(this.world, new ChunkPos(sectionZ, sectionZ), new BiomeContainer(BIOMES));
                ((IColumn) chunk).readSection(sectionY, null, readBuffer, nbtTagIn, sectionExists);
                ((IClientChunkProviderChunkArray) (Object) this.array).invokeReplace(idx, chunk);
            } else {
                ((IColumn) chunk).readSection(sectionY, null, readBuffer, nbtTagIn, sectionExists);
            }

            ChunkSection[] allSections = chunk.getSections();
            WorldLightManager lightManager = this.getLightManager();
            lightManager.enableLightSources(new ChunkPos(sectionZ, sectionZ), true);

            ChunkSection chunksection = allSections[sectionY];
            lightManager.updateSectionStatus(SectionPos.of(sectionZ, sectionY, sectionZ), ChunkSection.isEmpty(chunksection));

            this.world.onChunkLoaded(sectionZ, sectionZ);
            // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
            return chunk;
        }
    }
}
