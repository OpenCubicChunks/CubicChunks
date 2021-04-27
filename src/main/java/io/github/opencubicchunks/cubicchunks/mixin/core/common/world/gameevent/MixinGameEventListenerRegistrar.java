package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.gameevent;

import java.util.Optional;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameEventListenerRegistrar.class)
public class MixinGameEventListenerRegistrar {

    @Shadow @Final private GameEventListener listener;

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Redirect(method = "onListenerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;blockToSection(J)J"))
    private long cubicChunksOnListenerMove(long blockPos, Level level) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            return blockPos;
        }

        Optional<BlockPos> position = this.listener.getListenerSource().getPosition(level);
        return Coords.blockToSection(position.get()).asLong();
    }
}