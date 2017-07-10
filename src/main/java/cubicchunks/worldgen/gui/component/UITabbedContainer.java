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
import net.malisis.core.client.gui.event.component.SpaceChangeEvent;
import net.minecraft.util.math.MathHelper;

import java.awt.Button;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UITabbedContainer extends UIAutoResizableContainer<UITabbedContainer> {
    private final List<Tab> tabs = new ArrayList<>();
    private Consumer<String> onTitleUpdate;
    private IntConsumer onTabSet;
    private int currentTab = -1;
    private boolean autoResizeX;
    private boolean autoResizeY;

    /**
     * Default constructor, creates the components list.
     *
     * @param gui the gui
     */
    public UITabbedContainer(MalisisGui gui) {
        super(gui);
    }

    /**
     * Sets the current tab to the given number. Always results in calling all tab set listeners.
     *
     * If there are no tabs yet, sets tab number to a negative value.
     * Otherwise it's set to the requested number, clamped to be a valid tab number.
     */
    public void setTab(int newTab) {
        int previousTab = currentTab;
        currentTab = newTab;
        if (tabs.size() != 0) {
            currentTab = MathHelper.clamp(currentTab, 0, tabs.size() - 1);
        } else {
            currentTab = -1;
        }

        if (currentTab != -1 && currentTab != previousTab) {
            // add current new tab
            add(tabs.get(currentTab).getComponent());
            if (onTitleUpdate != null) {
                onTitleUpdate.accept(tabs.get(currentTab).getTitle());
            }
        }
        // remove the previous tab AFTER adding new one so that size never drops to zero
        // as that would cause unwanted scroll position changes if this is inside scrollable container
        if (previousTab != -1 && currentTab != previousTab) {
            // remove the previous tab
            remove(tabs.get(previousTab).getComponent());
        }
        if (onTabSet != null) {
            onTabSet.accept(currentTab);
        }
    }

    /**
     * Returns the currently selected tab, or negative value if no tab is set.
     */
    public int currentTab() {
        return currentTab;
    }

    /**
     * Returns the current tab count
     */
    public int size() {
        return tabs.size();
    }

    /**
     * Adds a listener called whenever tab number is set (even when it didn't change).
     *
     * Ths listener will be always called right after it's added. Tab number will be negative if there are no tabs yet.
     *
     * @param onTabSet the listener
     */
    public void onTabSet(IntConsumer onTabSet) {
        if (this.onTabSet == null) {
            this.onTabSet = onTabSet;
        } else {
            this.onTabSet = this.onTabSet.andThen(onTabSet);
        }
    }

    /**
     * Adds a listener called whenever the title needs to be updated.
     *
     * Ths listener will be always called right after it's added
     *
     * @param onTitleUpdate the listener
     */
    public void onTitleUpdate(Consumer<String> onTitleUpdate) {
        if (this.onTitleUpdate == null) {
            this.onTitleUpdate = onTitleUpdate;
        } else {
            this.onTitleUpdate = this.onTitleUpdate.andThen(onTitleUpdate);
        }
    }

    /**
     * Adds new tab with a given title, after the last tab. If this is the first tab added, it's set as the current tab.
     *
     * Note that a title change listener needs to be added to use the title.
     *
     * @param tab the tab to add
     * @param title the title of this tab
     */
    public void addTab(UIComponent<?> tab, String title) {
        tabs.add(new Tab(tab, title));
        setTab(currentTab == -1 ? 0 : currentTab);
    }

    public static UITabbedContainer withPrevNextButton(MalisisGui gui, UIButton previous, UIButton next, Consumer<String> onTitleUpdate) {
        UITabbedContainer container = new UITabbedContainer(gui);

        container.onTabSet(tab -> {
            previous.setDisabled(tab < 0 || tab <= 0);
            next.setDisabled(tab < 0 || tab >= container.size() - 1);
        });
        next.register(new Object() {
            @Subscribe
            public void onClick(UIButton.ClickEvent evt) {
                container.setTab(container.currentTab() + 1);
            }
        });
        previous.register(new Object() {
            @Subscribe
            public void onClick(UIButton.ClickEvent evt) {
                container.setTab(container.currentTab() - 1);
            }
        });
        container.onTitleUpdate(onTitleUpdate);

        return container;
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
