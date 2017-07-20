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
package cubicchunks.debug;

import cubicchunks.CubicChunks;
import cubicchunks.debug.item.GetLightValueItem;
import cubicchunks.debug.item.RelightSkyBlockItem;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings({"WeakerAccess", "ConstantConditions"})
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(modid = CubicChunks.MODID)
@ObjectHolder(CubicChunks.MODID)
public class DebugTools {

    public static final Item relight_sky_block = null;
    public static final Item get_light_value = null;

    @SubscribeEvent public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!CubicChunks.DEBUG_ENABLED) {
            return;
        }
        CreativeTabs tab = new CreativeTabs("cubic_chunks_debug_tab") {
            @SideOnly(Side.CLIENT) @Override public ItemStack getTabIconItem() {
                return relight_sky_block.getDefaultInstance();
            }
        };

        event.getRegistry().registerAll(
                new RelightSkyBlockItem("relight_sky_block").setCreativeTab(tab),
                new GetLightValueItem("get_light_value").setCreativeTab(tab)
        );
    }

    @Mod.EventBusSubscriber(value = Side.CLIENT, modid = CubicChunks.MODID)
    public static class ClientEventHandler {

        @SubscribeEvent public static void onModelRegistry(ModelRegistryEvent event) {
            if (!CubicChunks.DEBUG_ENABLED) {
                return;
            }
            ModelLoader.setCustomModelResourceLocation(relight_sky_block, 0,
                    new ModelResourceLocation(relight_sky_block.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(get_light_value, 0,
                    new ModelResourceLocation(get_light_value.getRegistryName(), "inventory"));
        }
    }
}
