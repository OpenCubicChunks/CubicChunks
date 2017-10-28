/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.worldgen.gui;

import static com.flowpowered.noise.Noise.gradientNoise3D;
import static cubicchunks.worldgen.gui.CustomCubicGui.HORIZONTAL_INSETS;
import static cubicchunks.worldgen.gui.CustomCubicGui.HORIZONTAL_PADDING;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.makeCheckbox;
import static cubicchunks.worldgen.gui.CustomCubicGuiUtils.malisisText;

import com.flowpowered.noise.NoiseQuality;
import com.flowpowered.noise.Utils;
import com.google.common.eventbus.Subscribe;
import cubicchunks.CubicChunks;
import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.util.MathUtil;
import cubicchunks.worldgen.generator.custom.ConversionUtils;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import cubicchunks.worldgen.gui.component.ShaderMultiImageView;
import cubicchunks.worldgen.gui.component.TerrainPreviewScaleOverlay;
import cubicchunks.worldgen.gui.component.UIBorderLayout;
import cubicchunks.worldgen.gui.component.UIVerticalTableLayout;
import cubicchunks.worldgen.gui.render.RawFloatImage;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.control.IControlComponent;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.FloatBuffer;
import java.util.function.Consumer;

public class TerrainPreview {

    private final UIBorderLayout borderContainer;
    private Matrix4f previewTransform = new Matrix4f();
    private float biomeScale = 0.01f;
    private float biomeOffset = 0;

    UICheckBox keepPreviewVisible;

    public TerrainPreview(CustomCubicGui gui) {
        ShaderMultiImageView<?> view;

        RawFloatImage img = generateNoiseTexture();
        RawFloatImage biomes = generateBiomesTexture();
        view = createImageView(gui, img, biomes);

        TerrainPreviewScaleOverlay overlay = new TerrainPreviewScaleOverlay(gui, view);

        view.addOnShaderTick(shader -> {
            CustomGeneratorSettings conf = gui.getConfig();

            shader.getShaderUniformOrDefault("previewTransform").set(getZoomOffsetMatrix());
            shader.getShaderUniformOrDefault("waterLevel").set(conf.waterLevel);

            shader.getShaderUniformOrDefault("heightVariationFactor").set(conf.heightVariationFactor);
            shader.getShaderUniformOrDefault("heightVariationSpecial").set(conf.specialHeightVariationFactorBelowAverageY);
            shader.getShaderUniformOrDefault("heightVariationOffset").set(conf.heightVariationOffset);
            shader.getShaderUniformOrDefault("heightFactor").set(conf.heightFactor);
            shader.getShaderUniformOrDefault("heightOffset").set(conf.heightOffset);

            shader.getShaderUniformOrDefault("depthFactor").set(conf.depthNoiseFactor);
            shader.getShaderUniformOrDefault("depthOffset").set(conf.depthNoiseOffset);
            shader.getShaderUniformOrDefault("depthFreq").set(conf.depthNoiseFrequencyX, conf.depthNoiseFrequencyZ);
            // this set method should be named setSafeInts
            shader.getShaderUniformOrDefault("depthOctaves").set(conf.depthNoiseOctaves, 0, 0, 0);

            shader.getShaderUniformOrDefault("selectorFactor").set(conf.selectorNoiseFactor);
            shader.getShaderUniformOrDefault("selectorOffset").set(conf.selectorNoiseOffset);
            shader.getShaderUniformOrDefault("selectorFreq").set(
                    conf.selectorNoiseFrequencyX, conf.selectorNoiseFrequencyY, conf.selectorNoiseFrequencyZ);
            shader.getShaderUniformOrDefault("selectorOctaves").set(conf.selectorNoiseOctaves, 0, 0, 0);

            shader.getShaderUniformOrDefault("lowFactor").set(conf.lowNoiseFactor);
            shader.getShaderUniformOrDefault("lowOffset").set(conf.lowNoiseOffset);
            shader.getShaderUniformOrDefault("lowFreq").set(conf.lowNoiseFrequencyX, conf.lowNoiseFrequencyY, conf.lowNoiseFrequencyZ);
            shader.getShaderUniformOrDefault("lowOctaves").set(conf.lowNoiseOctaves, 0, 0, 0);

            shader.getShaderUniformOrDefault("highFactor").set(conf.highNoiseFactor);
            shader.getShaderUniformOrDefault("highOffset").set(conf.highNoiseOffset);
            shader.getShaderUniformOrDefault("highFreq").set(conf.highNoiseFrequencyX, conf.highNoiseFrequencyY, conf.highNoiseFrequencyZ);
            shader.getShaderUniformOrDefault("highOctaves").set(conf.highNoiseOctaves, 0, 0, 0);

            overlay.setTransform(previewTransform.m00, previewTransform.m30, previewTransform.m31);
        });

        view.addOnMouseDrag((lastX, lastY, x, y, button) -> {
            previewTransform = previewTransform.translate(new Vector2f(lastX - x, y - lastY));
        });
        view.addControlComponent(new IControlComponent() {
            public static final long ANIMATION_LENGTH = 300; //ms
            float startZoom = 1;
            float targetZoom = 1;
            long animationStartTime = 0, animationEndTime = 0;

            @Override public void setParent(UIComponent<?> uiComponent) {
            }

            @Override public UIComponent<?> getParent() {
                return null;
            }

            @Override public UIComponent<?> getComponentAt(int i, int i1) {
                return null;
            }

            @Override public boolean onKeyTyped(char c, int i) {
                return false;
            }

            @Override public boolean onScrollWheel(int x, int y, int delta) {
                if (delta > 0) {
                    for (int i = 0; i < delta; i++) {
                        targetZoom /= Math.sqrt(2);
                    }
                } else {
                    for (int i = 0; i < -delta; i++) {
                        targetZoom *= Math.sqrt(2);
                    }
                }
                if (delta != 0) {
                    resetAnimation();
                }
                return true;
            }

            private void resetAnimation() {
                animationStartTime = System.currentTimeMillis();
                animationEndTime = animationStartTime + ANIMATION_LENGTH;
                startZoom = previewTransform.m00; // this relies on no rotation, and zoom being the same in all directions
            }

            @Override public void draw(GuiRenderer guiRenderer, int mouseX, int mouseY, float v) {
                float progress = MathUtil.unlerp(System.currentTimeMillis(), animationStartTime, animationEndTime);
                if (Double.isNaN(progress) || Double.isInfinite(progress)) {
                    updateZoom(targetZoom, mouseX - view.getX(), mouseY - view.getY());
                    return;
                }
                progress = MathHelper.clamp(progress, 0, 1);
                updateZoom(MathUtil.lerp(progress, startZoom, targetZoom), mouseX - view.screenX() - view.getWidth() / 2, mouseY - view.screenY()
                        - view.getHeight() / 2);
            }
        });

        overlay.setSize(UIComponent.INHERITED - 200, 128);

        UIBorderLayout borderLayout = new UIBorderLayout(gui);
        borderLayout.setSize(UIComponent.INHERITED, 128);
        borderLayout.add(overlay, UIBorderLayout.Border.LEFT);

        UIContainer<?> settingsContrainer = new UIVerticalTableLayout(gui, 4)
                .setInsets(5, 5, 5, 5)
                .add(keepPreviewVisible = makeCheckbox(gui, "keepPreviewVisible", true), new UIVerticalTableLayout.GridLocation(0, 0, 4));
        settingsContrainer.setSize(200, 128);

        borderLayout.add(settingsContrainer, UIBorderLayout.Border.RIGHT);

        this.borderContainer = borderLayout;
    }

    private RawFloatImage generateBiomesTexture() {
        // TODO: this duplicates vanilla code, and code already in BiomeSource, reuse the BiomeSource code directly?
        int samplesPerBiome = 16;
        int count = CubicBiome.REGISTRY.getValues().size();
        float[][] data = new float[count * samplesPerBiome][2];// 2 values per biome
        CubicBiome[] biomes = CubicBiome.REGISTRY.getValues().toArray(new CubicBiome[0]);

        int smoothRadius = 2;
        double[] nearBiomeWeightArray = new double[smoothRadius * 2 + 1];
        for (int z = -smoothRadius; z <= smoothRadius; z++) {
            double val = 0;
            // add up all the weights from the other axis to "simulate" infinitely long lines of biomes
            for (int x = -smoothRadius; x <= smoothRadius; x++) {
                val += 10.0F / Math.sqrt(x * x + z * z + 0.2F);
            }

            nearBiomeWeightArray[z + smoothRadius] = val;
        }

        for (int x = 0; x < count * samplesPerBiome; x++) {
            double totalHeight = 0;
            double totalHeightVariation = 0;
            double totalWeight = 0;

            int centerIdx = Math.floorDiv(x, samplesPerBiome);
            CubicBiome centerBiome = biomes[centerIdx];

            for (int dx = -smoothRadius; dx <= smoothRadius; dx++) {
                int pos = x + dx;
                int biomeIdx = Math.floorMod(Math.floorDiv(pos, samplesPerBiome), count);
                CubicBiome biome = biomes[biomeIdx];

                double biomeWeight = nearBiomeWeightArray[dx + smoothRadius];

                double biomeHeight = biome.getBiome().getBaseHeight();
                if (biomeHeight > centerBiome.getBiome().getBaseHeight()) {
                    // prefer biomes with lower height?
                    biomeWeight /= 2.0F;
                }

                totalHeight += biomeHeight * biomeWeight;
                totalWeight += biomeWeight;
                totalHeightVariation += biome.getBiome().getHeightVariation() * biomeWeight;
            }

            totalHeight /= totalWeight;
            totalHeightVariation /= totalWeight;

            data[x][0] = ConversionUtils.biomeHeightVanilla((float) totalHeight);
            data[x][1] = ConversionUtils.biomeHeightVariationVanilla((float) totalHeightVariation);
        }

        RawFloatImage obj = new RawFloatImage(data, 2);
        obj.loadTexture(null);
        return obj;
    }

    public void onSetKeepVisible(Consumer<Boolean> handler) {
        this.keepPreviewVisible.register(new Object() {
            @Subscribe
            public void onSet(UICheckBox.CheckEvent evt) {
                handler.accept(evt.isChecked());
            }
        });
    }

    private Matrix4f getZoomOffsetMatrix() {
        return previewTransform.transpose(new Matrix4f());
    }

    private void updateZoom(float newZoom, float mouseX, float mouseY) {
        float scale = newZoom / previewTransform.m00;
        previewTransform.translate(new Vector2f(mouseX, -mouseY))
                .scale(new Vector3f(scale, scale, scale))
                .translate(new Vector2f(-mouseX, mouseY));
    }

    private ShaderMultiImageView<?> createImageView(CustomCubicGui gui, RawFloatImage img, RawFloatImage biomes) {
        // Note: the actual resource location name used will be "shaders/program/" + resourceName + ".json"
        ShaderMultiImageView<?> view;
        ShaderManager shader = null;
        try {
            shader = new ShaderManager(Minecraft.getMinecraft().getResourceManager(), CubicChunks.MODID + ":custom-cubic-preview");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        shader.addSamplerTexture("perlin1", img);
        shader.addSamplerTexture("biomes", biomes);
        view = new ShaderMultiImageView(gui, shader, img, biomes);
        return view;
    }

    private RawFloatImage generateNoiseTexture() {
        float[][] data = new float[256][256];
        float max = 0, min = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = (float) gradientCoherentNoise3DTileable(i * 0.125, j * 0.125, 0, 123456, NoiseQuality.BEST, data.length / 8 - 1);
                if (data[i][j] > max) {
                    max = data[i][j];
                }
                if (data[i][j] < min) {
                    min = data[i][j];
                }
            }
        }
        System.out.println("MIN=" + min + ", MAX=" + max);
        RawFloatImage img = new RawFloatImage(data, 1);
        img.loadTexture(null);
        return img;
    }


    private static double gradientCoherentNoise3DTileable(double x, double y, double z, int seed, NoiseQuality quality, int mask) {

        // Create a unit-length cube aligned along an integer boundary.  This cube
        // surrounds the input point.

        int x0 = ((x >= 0.0) ? (int) x : (int) x - 1);
        int x1 = x0 + 1;

        int y0 = ((y >= 0.0) ? (int) y : (int) y - 1);
        int y1 = y0 + 1;

        int z0 = ((z >= 0.0) ? (int) z : (int) z - 1);
        int z1 = z0 + 1;

        double fx = x - x0, fy = y - y0, fz = z - z0;
        // Map the difference between the coordinates of the input value and the
        // coordinates of the cube's outer-lower-left vertex onto an S-curve.
        double xs, ys, zs;
        if (quality == NoiseQuality.FAST) {
            xs = fx;
            ys = fy;
            zs = fz;
        } else if (quality == NoiseQuality.STANDARD) {
            xs = Utils.sCurve3(fx);
            ys = Utils.sCurve3(fy);
            zs = Utils.sCurve3(fz);
        } else {

            xs = Utils.sCurve5(fx);
            ys = Utils.sCurve5(fy);
            zs = Utils.sCurve5(fz);
        }

        x0 &= mask;
        x1 &= mask;
        y0 &= mask;
        y1 &= mask;
        z0 &= mask;
        z1 &= mask;

        // Now calculate the noise values at each vertex of the cube.  To generate
        // the coherent-noise value at the input point, interpolate these eight
        // noise values using the S-curve value as the interpolant (trilinear
        // interpolation.)
        double n0, n1, ix0, ix1, iy0, iy1;
        n0 = gradientNoise3D(x0 + fx, y0 + fy, z0 + fz, x0, y0, z0, seed);
        n1 = gradientNoise3D(x1 + fx - 1, y0 + fy, z0 + fz, x1, y0, z0, seed);
        ix0 = Utils.linearInterp(n0, n1, xs);

        n0 = gradientNoise3D(x0 + fx, y1 + fy - 1, z0 + fz, x0, y1, z0, seed);
        n1 = gradientNoise3D(x1 + fx - 1, y1 + fy - 1, z0 + fz, x1, y1, z0, seed);
        ix1 = Utils.linearInterp(n0, n1, xs);
        iy0 = Utils.linearInterp(ix0, ix1, ys);
        n0 = gradientNoise3D(x0 + fx, y0 + fy, z1 + fz - 1, x0, y0, z1, seed);
        n1 = gradientNoise3D(x1 + fx - 1, y0 + fy, z1 + fz - 1, x1, y0, z1, seed);
        ix0 = Utils.linearInterp(n0, n1, xs);
        n0 = gradientNoise3D(x0 + fx, y1 + fy - 1, z1 + fz - 1, x0, y1, z1, seed);
        n1 = gradientNoise3D(x1 + fx - 1, y1 + fy - 1, z1 + fz - 1, x1, y1, z1, seed);
        ix1 = Utils.linearInterp(n0, n1, xs);
        iy1 = Utils.linearInterp(ix0, ix1, ys);
        return Utils.linearInterp(iy0, iy1, zs) * 2 - 1;
    }

    public UIContainer<?> containerWithPadding() {
        borderContainer.setPadding(HORIZONTAL_PADDING + HORIZONTAL_INSETS, 0);
        return borderContainer;
    }

    public UIContainer<?> containerNoPadding() {
        borderContainer.setPadding(0, 0);
        return borderContainer;
    }
}
