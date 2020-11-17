package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.chunk.biome.ColumnBiomeContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private ClientLevel level;
    @Shadow private RegistryAccess registryAccess;

    @Shadow public abstract ClientLevel getLevel();

//    private static final ChunkBiomeContainer dummyContainer = new ChunkBiomeContainer(new IdMapper<>(), new Biome[ChunkBiomeContainer.BIOMES_SIZE]);
//    @Nullable
//    @Redirect(method = "handleLevelChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientChunkCache;replaceWithPacketData(IILnet/minecraft/world/level/chunk/ChunkBiomeContainer;Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/world/level/chunk/LevelChunk;"))
//    private LevelChunk on$handleLevelChunk(ClientChunkCache clientChunkCache, int i, int j, ChunkBiomeContainer chunkBiomeContainer, FriendlyByteBuf friendlyByteBuf, CompoundTag compoundTag, int k) {
//        return clientChunkCache.replaceWithPacketData(i, j, dummyContainer, friendlyByteBuf, compoundTag, k);
//    }

    @Inject(method = "handleLevelChunk", at = @At("HEAD"), cancellable = true)
    public void handleLevelChunk(ClientboundLevelChunkPacket clientboundLevelChunkPacket, CallbackInfo ci) {
        //TODO: implement non-cubic world, inject at head and cancel
        ci.cancel();

        PacketUtils.ensureRunningOnSameThread(clientboundLevelChunkPacket, (ClientPacketListener)(Object)this, this.minecraft);
        int chunkX = clientboundLevelChunkPacket.getX();
        int chunkZ = clientboundLevelChunkPacket.getZ();

        //For a cc world we will always get null biomes
        ColumnBiomeContainer biomeContainer = new ColumnBiomeContainer(this.registryAccess.registryOrThrow(Registry.BIOME_REGISTRY), new Biome[ChunkBiomeContainer.BIOMES_SIZE]);
        LevelChunk levelChunk = this.level.getChunkSource().replaceWithPacketData(chunkX, chunkZ, biomeContainer, clientboundLevelChunkPacket.getReadBuffer(), clientboundLevelChunkPacket.getHeightmaps(), clientboundLevelChunkPacket.getAvailableSections());

        for (int k = this.level.getMinSection(); k < this.level.getMinSection() - 1; ++k) {
            this.level.setSectionDirtyWithNeighbors(chunkX, k, chunkZ);
        }

        if (levelChunk != null) {
            for (CompoundTag compoundTag : clientboundLevelChunkPacket.getBlockEntitiesTags()) {
                BlockPos blockPos = new BlockPos(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"));
                BlockEntity blockEntity = levelChunk.getBlockEntity(blockPos, LevelChunk.EntityCreationType.IMMEDIATE);
                if (blockEntity != null) {
                    blockEntity.load(compoundTag);
                }
            }
        }
    }


    @Redirect(method = {"handleForgetLevelChunk", "handleLevelChunk"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMaxSection()I"))
    private int getFakeMaxSectionY(ClientLevel clientLevel) {
        return clientLevel.getMinSection() - 1; // disable the loop, cube packets do the necessary work
    }
}
