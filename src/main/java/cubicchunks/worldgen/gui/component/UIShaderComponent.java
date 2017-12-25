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

import net.malisis.core.client.gui.GuiRenderer;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.renderer.MalisisRenderer;
import net.malisis.core.renderer.icon.Icon;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.ShaderManager;

public class UIShaderComponent<T extends UIShaderComponent<T>> extends UIComponent<T> {

    protected Icon icon;
    protected final ShaderManager shader;

    public UIShaderComponent(MalisisGui gui, ShaderManager shader) {
        super(gui);
        this.shader = shader;
        this.setSize(300, 128);

        this.icon = new Icon();
    }

    @Override public void drawBackground(GuiRenderer guiRenderer, int mouseX, int mouseY, float partialTicks) {
    }

    @Override public void drawForeground(GuiRenderer guiRenderer, int mouseX, int mouseY, float partialTicks) {
        preShaderDraw(guiRenderer, mouseX, mouseY, partialTicks);
        if (this.isEnabled()) {
            guiRenderer.next(DefaultVertexFormats.POSITION_TEX);
            shader.useShader();
            this.rp.icon.set(icon);
            shaderDraw(guiRenderer, mouseX, mouseY, partialTicks);
            guiRenderer.next(MalisisRenderer.malisisVertexFormat);
            shader.endShader();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        postShaderDraw(guiRenderer, mouseX, mouseY, partialTicks);
    }

    protected void preShaderDraw(GuiRenderer guiRenderer, int mouseX, int mouseY, float partialTicks) {
        // default implementation, noop
    }

    protected void shaderDraw(GuiRenderer guiRenderer, int mouseX, int mouseY, float partialTicks) {
        guiRenderer.drawShape(this.shape, rp);
    }

    protected void postShaderDraw(GuiRenderer guiRenderer, int mouseX, int mouseY, float partialTicks) {
        // default implementation, noop
    }
}
