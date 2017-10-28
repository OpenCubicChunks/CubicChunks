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
package cubicchunks.worldgen.gui.component;

import cubicchunks.worldgen.gui.event.IMouseDragListener;
import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.renderer.icon.Icon;
import net.malisis.core.util.MouseButton;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.ShaderManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ShaderMultiImageView<T extends ShaderMultiImageView<T>> extends UIComponent<T> {

    private final Icon icon;
    private ShaderManager shader;
    private ITextureObject[] textures;
    private Consumer<ShaderManager> onTick;
    private List<IMouseDragListener> mouseDragListeners = new ArrayList<>();

    public ShaderMultiImageView(MalisisGui gui, ShaderManager shader, ITextureObject...textures) {
        super(gui);
        this.shader = shader;
        this.textures = textures;
        this.setSize(300, 128);

        this.icon = new Icon().flip(false, true);
    }

    public ShaderMultiImageView<T> addOnShaderTick(Consumer<ShaderManager> onTick) {
        Consumer<ShaderManager> prev = this.onTick;
        this.onTick = shader -> {
            if (prev != null) {
                prev.accept(shader);
            }
            onTick.accept(shader);
        };
        return this;
    }

    @Override public void drawBackground(GuiRenderer guiRenderer, int i, int i1, float v) {

    }

    @Override public void drawForeground(GuiRenderer guiRenderer, int i, int i1, float v) {

        // todo: make it possible to set it from the outside
        float uMin = -getWidth() / 2;
        float uMax = getWidth() / 2;
        float vMin = -getHeight() / 2;
        float vMax = getHeight() / 2;
        guiRenderer.next(DefaultVertexFormats.POSITION_TEX);
        shader.useShader();
        if (onTick != null) {
            onTick.accept(shader);
        }
        this.rp.icon.set(icon);
        this.rp.icon.get().setUVs(uMin, vMin, uMax, vMax);
        guiRenderer.drawShape(this.shape, rp);
        guiRenderer.next();
        shader.endShader();
    }

    public void addOnMouseDrag(IMouseDragListener listener) {
        this.mouseDragListeners.add(listener);
    }

    @Override public boolean onDrag(int lastX, int lastY, int x, int y, MouseButton button) {
        mouseDragListeners.forEach(l -> l.onDrag(lastX, lastY, x, y, button));
        return true;
    }
}
