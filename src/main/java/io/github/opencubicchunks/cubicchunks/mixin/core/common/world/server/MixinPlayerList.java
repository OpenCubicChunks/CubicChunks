package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.server;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class MixinPlayerList {
    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;markUnsaved()V"))
    private void onPlayerLoggedOut(ServerPlayer player, CallbackInfo ci) {
        CubePos playerCubePos = CubePos.from(SectionPos.of(player.xChunk, player.yChunk, player.zChunk));
        ((ICubicWorld)player.getLevel()).getCube(playerCubePos.getX(), playerCubePos.getY(), playerCubePos.getZ()).setDirty(true);
    }
}