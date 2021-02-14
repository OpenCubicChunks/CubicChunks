package io.github.opencubicchunks.cubicchunks.mixin.core.server;

import io.github.opencubicchunks.cubicchunks.chunk.IVerticalView;
import io.github.opencubicchunks.cubicchunks.server.IDedicatedServerProperties;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DedicatedPlayerList.class)
public class MixinDedicatedPlayerList {


    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedPlayerList;setViewDistance(I)V"))
    private void setVerticalViewDistance(DedicatedPlayerList dedicatedPlayerList, int viewDistance, DedicatedServer dedicatedServer, RegistryAccess.RegistryHolder registryHolder,
                                         PlayerDataStorage playerDataStorage) {
        ((IVerticalView) (dedicatedPlayerList)).setIncomingVerticalViewDistance(((IDedicatedServerProperties) dedicatedServer.getProperties()).getVerticalViewDistance());
        (dedicatedPlayerList).setViewDistance(viewDistance);

    }
}
