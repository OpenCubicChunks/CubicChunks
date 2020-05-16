package cubicchunks.cc.mixin.core.server;

import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {

    //Sets the new height and removes the warning players recieve when attempting to place blocks beyond 256.
    @ModifyConstant(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Lcom/mojang/authlib/minecraft/MinecraftSessionService;Lcom/mojang/authlib/GameProfileRepository;Lnet/minecraft/server/management/PlayerProfileCache;Lnet/minecraft/world/chunk/listener/IChunkStatusListenerFactory;)V", constant = @Constant(intValue = 256))
    private int setCCHeightLimit(int orig) {
        return 512;
    }
}
