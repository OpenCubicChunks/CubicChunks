package io.github.opencubicchunks.cubicchunks.mixin.core.client.progress;

import io.github.opencubicchunks.cubicchunks.client.CubicWorldLoadScreen;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldLoadProgressScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.TrackingChunkStatusListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldLoadProgressScreen.class)
public class MixinWorldLoadProgressScreen extends Screen {

    @Shadow @Final private static Object2IntMap<ChunkStatus> COLORS;

    protected MixinWorldLoadProgressScreen(ITextComponent titleIn) {
        super(titleIn);
    }

    //@ModifyVariable(method = "drawProgress", at = @At("HEAD"), ordinal = 0)
    //private static int shiftLeft(int xBase) {
    //    return xBase - 100;
    //}

    @Inject(method = "drawProgress", at = @At("HEAD"), cancellable = true)
    private static void onDraw(TrackingChunkStatusListener trackerParam,
            int xBase, int yBase, int scale, int spacing, CallbackInfo ci) {
        ci.cancel();

        CubicWorldLoadScreen.doRender(trackerParam, xBase, yBase, scale, spacing, COLORS);

    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/listener/TrackingChunkStatusListener;getPercentDone()I"))
    private int on$getPercentDone(TrackingChunkStatusListener trackingChunkStatusListener) {
        return trackingChunkStatusListener.getPercentDone();
    }
}
