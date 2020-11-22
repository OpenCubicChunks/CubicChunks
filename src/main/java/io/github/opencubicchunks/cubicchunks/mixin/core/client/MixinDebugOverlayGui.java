package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import java.util.List;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DebugScreenOverlay.class)
public class MixinDebugOverlayGui {
    @SuppressWarnings("rawtypes")
    @Inject(method = "getGameInformation",
        at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 6),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void onAddChunkInfo(CallbackInfoReturnable<List> cir, /*IntegratedServer integratedserver, Connection networkmanager, float f, float f1,*/
                                String s, BlockPos blockpos, Entity entity, Direction direction, String s1, /*ChunkPos chunkpos,*/ Level world, LongSet longset,
                                List debugScreenList/*, String s2*/) {
        //noinspection unchecked
        debugScreenList.add(String.format("Cube:  %d %d %d in %d %d %d",
            blockpos.getX() & (IBigCube.DIAMETER_IN_BLOCKS - 1), blockpos.getY() & (IBigCube.DIAMETER_IN_BLOCKS - 1), blockpos.getZ() & (IBigCube.DIAMETER_IN_BLOCKS - 1),
            Coords.blockToCube(blockpos.getX()), Coords.blockToCube(blockpos.getY()), Coords.blockToCube(blockpos.getZ()))
        );
    }
}