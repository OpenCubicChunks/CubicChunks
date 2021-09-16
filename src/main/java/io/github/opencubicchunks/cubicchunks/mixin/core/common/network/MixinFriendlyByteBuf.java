package io.github.opencubicchunks.cubicchunks.mixin.core.common.network;


import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FriendlyByteBuf.class)
public abstract class MixinFriendlyByteBuf {

    @Shadow public abstract long readLong();

    @Shadow public abstract int readVarInt();

    @Shadow public abstract ByteBuf writeLong(long l);

    @Shadow public abstract FriendlyByteBuf writeVarInt(int i);

    @Inject(method = "readBlockPos", at = @At("HEAD"), cancellable = true)
    private void heightFixPosRead(CallbackInfoReturnable<BlockPos> cir) {
        BlockPos data = BlockPos.of(this.readLong());
        if (data.getY() == DimensionType.MIN_Y) {
            cir.setReturnValue(new BlockPos(data.getX(), this.readVarInt(), data.getZ()));
        } else {
            cir.setReturnValue(data);
        }
    }

    @Inject(method = "writeBlockPos", at = @At("HEAD"), cancellable = true)
    private void heightFixPosWrite(BlockPos pos, CallbackInfoReturnable<FriendlyByteBuf> cir) {
        int y = pos.getY();
        if (y >= DimensionType.MAX_Y - 1 || y <= DimensionType.MIN_Y + 1) {
            this.writeLong(new BlockPos(pos.getX(), DimensionType.MIN_Y, pos.getZ()).asLong());
            this.writeVarInt(y);
            cir.setReturnValue((FriendlyByteBuf) (Object) this);
        }
    }
}
