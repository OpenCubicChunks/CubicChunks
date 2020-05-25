package cubicchunks.cc.mixin.core.client.progress;

import com.mojang.blaze3d.systems.RenderSystem;
import cubicchunks.cc.chunk.ITrackingSectionStatusListener;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldLoadProgressScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.TrackingChunkStatusListener;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(WorldLoadProgressScreen.class)
public class MixinWorldLoadProgressScreen extends Screen {

    @Shadow @Final private static Object2IntMap<ChunkStatus> COLORS;

    protected MixinWorldLoadProgressScreen(ITextComponent titleIn) {
        super(titleIn);
    }

    @ModifyVariable(method = "drawProgress", at = @At("HEAD"), ordinal = 0)
    private static int shiftLeft(int xBase) {
        return xBase - 100;
    }

    @Inject(method = "drawProgress", at = @At("RETURN"))
    private static void onDraw(TrackingChunkStatusListener trackerParam,
            int xBase, int yBase, int scale, int spacing, CallbackInfo ci) {
        xBase += 200;

        int squareScale = scale + spacing;
        int loadDiameter = trackerParam.getDiameter();
        int diameterPixels = loadDiameter * squareScale - spacing;
        int totalDiameter = trackerParam.func_219523_d();
        int totalDiameterPixels = totalDiameter * squareScale - spacing;

        int minX = xBase - totalDiameterPixels / 2;
        int minZ = yBase - totalDiameterPixels / 2;

        int radiusPixels = diameterPixels / 2 + 1;

        int color = 0xff0011ff;
        if (spacing != 0) {
            fill(xBase - radiusPixels, yBase - radiusPixels, xBase - radiusPixels + 1, yBase + radiusPixels, color);
            fill(xBase + radiusPixels - 1, yBase - radiusPixels, xBase + radiusPixels, yBase + radiusPixels, color);
            fill(xBase - radiusPixels, yBase - radiusPixels, xBase + radiusPixels, yBase - radiusPixels + 1, color);
            fill(xBase - radiusPixels, yBase + radiusPixels - 1, xBase + radiusPixels, yBase + radiusPixels, color);
        }

        final List<ChunkStatus> statuses = ChunkStatus.getAll();
        final List<ChunkStatus> statusesReverse = new ArrayList<>(statuses);
        Collections.reverse(statusesReverse);

        for (int dx = 0; dx < totalDiameter; ++dx) {
            for (int dz = 0; dz < totalDiameter; ++dz) {
                Map<ChunkStatus, Integer> statusCounts = new HashMap<>();
                for (int dy = 0; dy < totalDiameter; dy++) {
                    ChunkStatus chunkstatus = ((ITrackingSectionStatusListener) trackerParam).getSectionStatus(dx, dy, dz);
                    statusCounts.putIfAbsent(chunkstatus, 0);
                    //noinspection ConstantConditions
                    statusCounts.compute(chunkstatus, (status, count) -> count + 1);
                }
                Map<ChunkStatus, Float> squareSizes = new HashMap<>();

                int count = 0;
                final float centerX = minX + dx * squareScale + squareScale * 0.5f;
                final float centerZ = minZ + dz * squareScale + squareScale * 0.5f;
                for (ChunkStatus status : statuses) {
                    if (!statusCounts.containsKey(status)) {
                        continue;
                    }
                    count += statusCounts.get(status);
                    float fraction = count / (float) loadDiameter;
                    float radius = fraction * squareScale * 0.5f;
                    squareSizes.put(status, radius);
                }

                fillFloat(TransformationMatrix.identity().getMatrix(),
                        centerX, centerZ, centerX + squareScale, centerZ +  squareScale, COLORS.getInt(null) | 0xff000000);

                for (ChunkStatus status : statusesReverse) {
                    if (!squareSizes.containsKey(status)) {
                        continue;
                    }
                    float radius = squareSizes.get(status);

                    float screenX = centerX - radius;
                    float screenY = centerZ - radius;

                    fillFloat(TransformationMatrix.identity().getMatrix(),
                            screenX, screenY, screenX + radius * 2, screenY + radius * 2, COLORS.getInt(status) | 0xff000000);
                }
            }
        }

    }

    private static void fillFloat(Matrix4f transform, float x1, float y1, float x2, float y2, int color) {
        if (x1 < x2) {
            float i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            float j = y1;
            y1 = y2;
            y2 = j;
        }

        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(transform, x1, y2, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.pos(transform, x2, y2, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.pos(transform, x2, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.pos(transform, x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.finishDrawing();

        WorldVertexBufferUploader.draw(buffer);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
