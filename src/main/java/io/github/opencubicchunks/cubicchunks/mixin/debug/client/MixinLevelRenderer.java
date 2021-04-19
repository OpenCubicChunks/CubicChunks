package io.github.opencubicchunks.cubicchunks.mixin.debug.client;

import java.lang.reflect.Field;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    private static final boolean doDebugRender = System.getProperty("cubicchunks.debug.statusrenderer", "false").equalsIgnoreCase("true");

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;"
        + "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V"))
    public void render(PoseStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture,
                       Matrix4f matrix4f, CallbackInfo ci) {
        if(!doDebugRender)
            return;

        ServerLevel levelAccessor = Minecraft.getInstance().getSingleplayerServer().getLevel(Level.OVERWORLD);
        RenderSystem.disableBlend();
        RenderSystem.disableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        double cameraX = camera.getPosition().x;
        double cameraY = camera.getPosition().y;
        double cameraZ = camera.getPosition().z;
        BlockPos blockPos = new BlockPos(cameraX, cameraY, cameraZ);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        Object2IntMap<ChunkStatus> colors = getField(
            LevelLoadingScreen.class, null, "COLORS" // TODO: intermediary name
        );

        int renderRadius = 5;

        int chunkRenderRadius = renderRadius * IBigCube.DIAMETER_IN_SECTIONS;
        for (int x = -chunkRenderRadius; x <= chunkRenderRadius; ++x) {
            for (int z = -chunkRenderRadius; z <= chunkRenderRadius; ++z) {
                BlockPos offset = blockPos.offset(x * 16, 0, z * 16);
                int color = 0;
                int chunkX = offset.getX() >> 4;
                int chunkZ = offset.getZ() >> 4;
                ChunkAccess chunkAccess = levelAccessor.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
                if (chunkAccess != null) {
                    ChunkStatus status = chunkAccess.getStatus();
                    color = colors.getOrDefault(status, 0);
                }

                Vector3f vector3f = new Vector3f(((color >> 16) & 0xff) / 256f, ((color >> 8) & 0xff) / 256f, (color & 0xff) / 256f); //this.getColor(types);

                int xPos = SectionPos.sectionToBlockCoord(chunkX, 0);
                int zPos = SectionPos.sectionToBlockCoord(chunkZ, 0);
                LevelRenderer.addChainedFilledBoxVertices(bufferBuilder, xPos + 4.25F - cameraX, 108 - cameraY, zPos + 4.25F - cameraZ, xPos + 11.75F - cameraX, 108 - cameraY + 0.09375F,
                    zPos + 11.75F - cameraZ, vector3f.x(), vector3f.y(), vector3f.z(), 1.0F);
            }
        }

        for (int x = -renderRadius; x <= renderRadius; ++x) {
            for (int z = -renderRadius; z <= renderRadius; ++z) {
                for (int y = -renderRadius; y < renderRadius; y++) {
                    BlockPos offset = blockPos.offset(x * IBigCube.DIAMETER_IN_BLOCKS, y * IBigCube.DIAMETER_IN_BLOCKS, z * IBigCube.DIAMETER_IN_BLOCKS);
                    int color = 0;
                    int cubeX = Coords.blockToCube(offset.getX());
                    int cubeY = Coords.blockToCube(offset.getY());
                    int cubeZ = Coords.blockToCube(offset.getZ());
                    IBigCube cube = ((ICubicWorld) levelAccessor).getCube(cubeX, cubeY, cubeZ, ChunkStatus.EMPTY, false);
                    if (cube != null) {
                        ChunkStatus status = cube.getStatus();
                        color = colors.getOrDefault(status, 0);
                    }

                    Vector3f vector3f = new Vector3f(((color >> 16) & 0xff) / 256f, ((color >> 8) & 0xff) / 256f, (color & 0xff) / 256f); //this.getColor(types);

                    int xPos = Coords.cubeToMinBlock(cubeX);
                    int yPos = Coords.cubeToMinBlock(cubeY);
                    int zPos = Coords.cubeToMinBlock(cubeZ);
                    LevelRenderer.addChainedFilledBoxVertices(bufferBuilder, xPos + 4.25F - cameraX, yPos - 4.25F - cameraY, zPos + 4.25F - cameraZ,
                        xPos + 11.75F - cameraX, yPos + 4.25F - cameraY, zPos + 11.75F - cameraZ, vector3f.x(), vector3f.y(), vector3f.z(), 1.0F);
                }
            }
        }

        tesselator.end();
        RenderSystem.enableTexture();
    }

    private static <T> T getField(Class<?> cl, Object obj, String name) {
        try {
            Field f = cl.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
