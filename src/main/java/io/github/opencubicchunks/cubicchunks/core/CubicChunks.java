/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.client.GuiHandler;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubicWorldInit;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CubicChunks.MODID)
public class CubicChunks {

    public static final String MODID = "cubicchunks";

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String PROTOCOL_VERSION = "0.1";


    public CubicChunks() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        CubicChunksConfig.register();

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(GuiHandler::handleGui));
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoinWorld);
    }

    private void setup(final FMLCommonSetupEvent event) {
        PacketDispatcher.register();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
    }

    // TODO use EntityJoinWorldEvent when it works as it's fired earlier
    private void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getPlayer() instanceof ServerPlayerEntity && ((ICubicWorld) evt.getPlayer().getEntityWorld()).isCubicWorld()) {
            PacketDispatcher.sendTo(new PacketCubicWorldInit((ServerWorld) evt.getPlayer().getEntityWorld()), (ServerPlayerEntity) evt.getPlayer());
        }
    }
}
