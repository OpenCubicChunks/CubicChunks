package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import com.mojang.authlib.GameProfile;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity extends Player {
    public MixinServerPlayerEntity(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }

    @Shadow public abstract ServerLevel getLevel();

    @Redirect(method = "trackChunk",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 0))
    public void onSendChunkLoad(ServerGamePacketListenerImpl serverPlayNetHandler, Packet<?> packetIn) {
        if (!((CubicLevelHeightAccessor) this.getLevel()).isCubic()) {
            serverPlayNetHandler.send(packetIn);
        }
    }

    // This debug code probably causes considerable lag and other issues; it should only be used while debugging lighting
//    @Inject(method = "tick",
//        at = @At("HEAD"))
//    private void onTick(CallbackInfo ci) {
//        if (((CubicLevelHeightAccessor) this.getLevel()).isCubic()) {
//            PacketDispatcher.sendTo(new PacketUpdateLight(CubePos.from(new BlockPos(this.position())), this.level.getLightEngine(), true), (ServerPlayer) (Object) this);
//        }
//    }
}