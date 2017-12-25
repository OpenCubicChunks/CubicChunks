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

import com.google.common.eventbus.Subscribe;
import mcp.MethodsReturnNonnullByDefault;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UITabbedContainer extends UIContainer<UITabbedContainer> {

    private final UIButton previous;
    private final UIButton next;
    private final List<Tab> tabs = new ArrayList<>();
    private final Consumer<String> onTitleUpdate;
    private int currentTab = -1;

    /**
     * Default constructor, creates the components list.
     *
     * @param gui the gui
     */
    public UITabbedContainer(MalisisGui gui, UIButton previous, UIButton next, Consumer<String> onTitleUpdate) {
        super(gui);
        this.previous = previous;
        this.next = next;
        next.register(new Object() {
            @Subscribe
            public void onClick(UIButton.ClickEvent evt) {
                updateTab(currentTab + 1);
            }
        });
        previous.register(new Object() {
            @Subscribe
            public void onClick(UIButton.ClickEvent evt) {
                updateTab(currentTab - 1);
            }
        });
        this.onTitleUpdate = onTitleUpdate;
    }

    private void updateTab(int i) {
        int previousTab = currentTab;
        currentTab = i;
        if (tabs.size() != 0) {
            currentTab = MathHelper.clamp(currentTab, 0, tabs.size() - 1);
        } else {
            currentTab = -1;
        }
        if (currentTab == -1) {
            previous.setEnabled(true);
            next.setEnabled(false);
        } else {
            previous.setEnabled(currentTab > 0);
            next.setEnabled(currentTab < tabs.size() - 1);
        }
        if (previousTab != -1) {
            // remove the previous tab
            remove(tabs.get(previousTab).getComponent());
        }
        if (currentTab != -1) {
            add(tabs.get(currentTab).getComponent());
            onTitleUpdate.accept(tabs.get(currentTab).getTitle());
        }

    }

    public void addTab(UIComponent<?> tab, String title) {
        tabs.add(new Tab(tab, title));
        updateTab(currentTab == -1 ? 0 : currentTab);
    }

    private static final class Tab {

        private final UIComponent<?> component;
        private final String title;

        private Tab(UIComponent<?> component, String title) {
            this.component = component;
            this.title = title;
        }

        public UIComponent<?> getComponent() {
            return component;
        }

        public String getTitle() {
            return title;
        }
    }
}
