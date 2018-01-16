package cubicchunks.asm.mixin.fixes.common;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayServer {

    @Overwrite
    private static boolean isMovePlayerPacketInvalid(CPacketPlayer packetIn) {
        if (Doubles.isFinite(packetIn.getX(0.0D)) && Doubles.isFinite(packetIn.getY(0.0D)) && Doubles.isFinite(packetIn.getZ(0.0D)) && Floats
                .isFinite(packetIn.getPitch(0.0F)) && Floats.isFinite(packetIn.getYaw(0.0F))) {
            return Math.abs(packetIn.getX(0.0D)) > 3.0E7D /*|| Math.abs(packetIn.getY(0.0D)) > 3.0E7D*/ || Math.abs(packetIn.getZ(0.0D)) > 3.0E7D;
        } else {
            return true;
        }
    }
}
