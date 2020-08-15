package io.github.opencubicchunks.cubicchunks.mixin.core.common.tileentity;

import net.minecraft.tileentity.ConduitTileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConduitTileEntity.class)
public class MixinConduitTileEntity {
    /**
     * @author NotStirred
     * @reason Conduits now only affect players within +256 of the block. This prevents near infinite cube loading in the conduit's column
     */
    //TODO: NotStirred merge this with BeaconTileEntity to reduce duplicated code
    @Redirect(method = {"addEffectsToPlayers()V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getHeight()I"))
    private int on$getHeight(World world) {
        return 256;
    }
}
