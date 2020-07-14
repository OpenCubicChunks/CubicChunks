package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.server;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class MixinPlayerList {
    @Inject(method = "playerLoggedOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;markDirty()V"))
    private void onPlayerLoggedOut(ServerPlayerEntity player, CallbackInfo ci) {
        CubePos playerCubePos = CubePos.from(SectionPos.of(player.chunkCoordX, player.chunkCoordY, player.chunkCoordZ));
        ((ICubicWorld)player.getServerWorld()).getCube(playerCubePos.getX(), playerCubePos.getY(), playerCubePos.getZ());
    }
}
