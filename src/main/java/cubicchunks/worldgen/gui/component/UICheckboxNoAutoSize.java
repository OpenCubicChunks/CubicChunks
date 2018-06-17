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

import mcp.MethodsReturnNonnullByDefault;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.renderer.font.MalisisFont;
import net.minecraft.util.math.MathHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UICheckboxNoAutoSize extends UICheckBox {

    private static final MethodHandle textField;

    static {
        try {
            Field field = UICheckBox.class.getDeclaredField("text");
            field.setAccessible(true);
            textField = MethodHandles.lookup().unreflectSetter(field);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public UICheckboxNoAutoSize(MalisisGui gui, String text) {
        super(gui, text);
        this.setSize(30, 10);
    }

    public UICheckboxNoAutoSize(MalisisGui gui) {
        super(gui);
        this.setSize(30, 10);
    }


    public UICheckBox setFont(MalisisFont font) {
        this.font = font;
        return this;
    }

    public FontOptions getFontOptions() {
        return this.fontOptions;
    }

    public UICheckBox setFontOptions(FontOptions options) {
        this.fontOptions = options;
        return this;
    }

    public UICheckBox setText(String text) {
        try {
            this.textField.invoke(this, text);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        return this;
    }
}
