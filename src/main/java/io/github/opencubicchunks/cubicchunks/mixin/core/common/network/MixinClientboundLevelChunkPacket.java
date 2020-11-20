package io.github.opencubicchunks.cubicchunks.mixin.core.common.network;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientboundLevelChunkPacket.class)
public class MixinClientboundLevelChunkPacket {

    /**
     * @author NotStirred
     * @reason CC doesn't need to send chunk biome data, as it's in cubes.
     */
    @Redirect(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkBiomeContainer;writeBiomes()[I"))
    public int[] nullWriteBiomes(ChunkBiomeContainer chunkBiomeContainer) {
        return null;
    }

}
