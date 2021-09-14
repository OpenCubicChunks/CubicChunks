package io.github.opencubicchunks.cubicchunks.mixin.core.client.progress;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.opencubicchunks.cubicchunks.client.CubicWorldLoadScreen;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public class MixinWorldLoadProgressScreen extends Screen {

    @Shadow @Final private static Object2IntMap<ChunkStatus> COLORS;

    protected MixinWorldLoadProgressScreen(Component titleIn) {
        super(titleIn);
    }

    @Inject(method = "renderChunks", at = @At("HEAD"), cancellable = true)
    private static void onDraw(PoseStack mStack, StoringChunkProgressListener trackerParam,
                               int xBase, int yBase, int scale, int spacing, CallbackInfo ci) {

        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            if (!((CubicLevelHeightAccessor) level).isCubic()) {
                return;
            }
        }

        ci.cancel();
        CubicWorldLoadScreen.doRender(mStack, trackerParam, xBase, yBase, scale, spacing, COLORS);

    }
}