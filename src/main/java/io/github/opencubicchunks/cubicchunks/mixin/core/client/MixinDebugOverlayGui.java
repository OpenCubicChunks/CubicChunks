package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.gui.overlay.DebugOverlayGui;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(DebugOverlayGui.class)
public class MixinDebugOverlayGui {
    @SuppressWarnings("rawtypes")
    @Inject(method = "getGameInformation",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 6),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void onAddChunkInfo(CallbackInfoReturnable<List> cir, IntegratedServer integratedserver, NetworkManager networkmanager, float f, float f1, String s, BlockPos blockpos, Entity entity, Direction direction, String s1, ChunkPos chunkpos, World world, LongSet longset, List debugScreenList, String s2) {
        //noinspection unchecked
        debugScreenList.add(String.format("Cube:  %d %d %d in %d %d %d",
                blockpos.getX() & (IBigCube.DIAMETER_IN_BLOCKS-1), blockpos.getY() & (IBigCube.DIAMETER_IN_BLOCKS-1), blockpos.getZ() & (IBigCube.DIAMETER_IN_BLOCKS-1),
                Coords.blockToCube(blockpos.getX()), Coords.blockToCube(blockpos.getY()), Coords.blockToCube(blockpos.getZ()))
        );
    }
}