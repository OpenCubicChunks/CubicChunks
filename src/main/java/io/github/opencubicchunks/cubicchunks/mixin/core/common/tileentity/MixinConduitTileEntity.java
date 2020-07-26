package io.github.opencubicchunks.cubicchunks.mixin.core.common.tileentity;

import net.minecraft.tileentity.ConduitTileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConduitTileEntity.class)
public class MixinConduitTileEntity {

    @Redirect(method = "addEffectsToPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getHeight()I"))
    private int on$addEffectsToPlayers(World world) {
        return 256;
    }

}
