/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
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
package io.github.opencubicchunks.cubicchunks.core.util;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight.IASMEventHandler;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common.fakeheight.IEventBus;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.ListenerList;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CompatHandler {

    private static final Set<String> IWORLDGENERATOR_FAKE_HEIGHT = ImmutableSet.of(
            "ic2",
            "thaumcraft",
            "fossil",
            "realistictorches",
            "iceandfire"
    );

    private static final Set<String> POPULATE_EVENT_PRE_FAKE_HEIGHT = ImmutableSet.of(
            "reccomplex"
    );

    private static final Set<String> DECORATE_EVENT_FAKE_HEIGHT = ImmutableSet.of(
            "reccomplex"
    );

    private static final Set<String> POST_DECORATE_EVENT_FAKE_HEIGHT = ImmutableSet.of(
            "joshxmas"
    );

    private static final Set<String> FAKE_CHUNK_LOAD = ImmutableSet.of(
            "zerocore"
    );

    private static final Map<String, String> packageToModId = getPackageToModId();

    private static IEventListener[] fakeChunkLoadListeners;

    public static void init() {
        fakeChunkLoadListeners = getFakeEventListeners(
                new ChunkEvent.Load(new Chunk(null, 0, 0)).getListenerList(),
                MinecraftForge.EVENT_BUS, FAKE_CHUNK_LOAD
        );
    }
    private static Map<String, String> getPackageToModId() {
        return Collections.unmodifiableMap(Loader.instance().getActiveModList().stream()
                .flatMap(mod -> mod.getOwnedPackages().stream().map(pkg -> new AbstractMap.SimpleEntry<>(pkg, mod.getModId())))
                .collect(Collectors
                        .toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (a, b) -> a.equals(b) ? a : a + " or " + b)));
    }

    public static Set<String> getModsForStacktrace(StackTraceElement[] stacktrace) {
        Set<String> mods = new HashSet<>();
        for (StackTraceElement traceElement : stacktrace) {
            try {
                Class<?> cl = Class.forName(traceElement.getClassName());
                if (cl != null && cl.getPackage() != null) {
                    String modid = packageToModId.get(cl.getPackage().getName());
                    if (modid != null && !modid.equals("minecraft") && !modid.equals("forge") && !modid.equals("cubicchunks")) {
                        mods.add(modid);
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return mods;
    }

    public static void beforeGenerate(World world, IWorldGenerator generator) {
        Class<? extends IWorldGenerator> genClass = generator.getClass();
        if (!packageToModId.containsKey(genClass.getPackage().getName())) {
            CubicChunks.bigWarning("Found IWorldGenerator %s that doesn't come from any mod! This is most likely a bug.", genClass);
            return;
        }
        String modid = packageToModId.get(genClass.getPackage().getName());
        if (IWORLDGENERATOR_FAKE_HEIGHT.contains(modid)) {
            ((ICubicWorldInternal.Server) world).fakeWorldHeight(256);
        }
    }

    public static void afterGenerate(World world) {
        ((ICubicWorldInternal.Server) world).fakeWorldHeight(0);
    }

    // this is called from a mixin into ForgeEventFactory
    // it's needed for mods that use events instead of IWorldGenerator
    // performance is not a big issue there, as foring most of those events
    // is a relatively small part of the whole worldgen

    public static boolean postChunkPopulatePreWithFakeWorldHeight(PopulateChunkEvent.Pre event) {
        if (!(MinecraftForge.EVENT_BUS instanceof IEventBus)) {
            MinecraftForge.EVENT_BUS.post(event);
        }
        return postEventPerModFakeHeight(event.getWorld(), event, MinecraftForge.EVENT_BUS, POPULATE_EVENT_PRE_FAKE_HEIGHT);
    }

    public static boolean postBiomeDecorateWithFakeWorldHeight(DecorateBiomeEvent.Decorate event) {
        if (!(MinecraftForge.EVENT_BUS instanceof IEventBus)) {
            MinecraftForge.EVENT_BUS.post(event);
        }
        return postEventPerModFakeHeight(event.getWorld(), event, MinecraftForge.EVENT_BUS, DECORATE_EVENT_FAKE_HEIGHT);
    }

    public static boolean postBiomePostDecorateWithFakeWorldHeight(DecorateBiomeEvent.Post event) {
        if (!(MinecraftForge.EVENT_BUS instanceof IEventBus)) {
            MinecraftForge.EVENT_BUS.post(event);
        }
        return postEventPerModFakeHeight(event.getWorld(), event, MinecraftForge.EVENT_BUS, POST_DECORATE_EVENT_FAKE_HEIGHT);
    }

    private static boolean postEventPerModFakeHeight(World world, Event event, EventBus eventBus, Set<String> modIds) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return eventBus.post(event);
        }
        return postEvent((ICubicWorldInternal.Server) world, event, eventBus, modIds, w -> w.fakeWorldHeight(256), w -> w.fakeWorldHeight(0));
    }

    public static void onCubeLoad(ChunkEvent.Load load) {
        if (fakeChunkLoadListeners == null || fakeChunkLoadListeners.length == 0) {
            return;
        }
        onChunkLoadImpl(load);
    }

    private static void onChunkLoadImpl(ChunkEvent.Load load) {
        IEventBus bus = (IEventBus) MinecraftForge.EVENT_BUS;
        if (bus.isShutdown()) {
            return;
        }
        int i = -1;
        try {
            for (i = 0; i < fakeChunkLoadListeners.length; i++) {
                IEventListener fakeChunkLoadListener = fakeChunkLoadListeners[i];
                fakeChunkLoadListener.invoke(load);
            }
        } catch (Throwable throwable) {
            bus.getExceptionHandler().handleException(MinecraftForge.EVENT_BUS, load, fakeChunkLoadListeners, i, throwable);
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }
    }

    private static <T> boolean postEvent(T ctx, Event event, EventBus eventBus, Set<String> modIds, Consumer<T> preEvt, Consumer<T> postEvt) {
        IEventBus forgeEventBus = (IEventBus) eventBus;
        if (forgeEventBus.isShutdown()) {
            return false;
        }
        IEventListener[] listeners = event.getListenerList().getListeners(forgeEventBus.getBusID());
        int index = 0;
        try {
            for (; index < listeners.length; index++) {
                try {
                    IEventListener listener = listeners[index];
                    if (listener instanceof IASMEventHandler) {
                        IASMEventHandler handler = (IASMEventHandler) listener;
                        String modid = handler.getOwner().getModId();
                        if (modIds.contains(modid)) {
                            preEvt.accept(ctx);
                        }
                    }
                    listener.invoke(event);
                } finally {
                    postEvt.accept(ctx);
                }
            }
        } catch (Throwable throwable) {
            forgeEventBus.getExceptionHandler().handleException(eventBus, event, listeners, index, throwable);
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }
        return event.isCancelable() && event.isCanceled();
    }

    private static <T> IEventListener[] getFakeEventListeners(ListenerList listenerList, EventBus eventBus, Set<String> modIds) {
        if (!(eventBus instanceof IEventBus)) {
            CubicChunks.LOGGER.error("Failed to initialize CompatHandler! No event bus mixin!");
            return null;
        }
        IEventBus forgeEventBus = (IEventBus) eventBus;
        IEventListener[] listeners = listenerList.getListeners(forgeEventBus.getBusID());
        List<IEventListener> newList = new ArrayList<>();
        int index = 0;
        for (; index < listeners.length; index++) {
            IEventListener listener = listeners[index];
            if (listener instanceof IASMEventHandler) {
                IASMEventHandler handler = (IASMEventHandler) listener;
                String modid = handler.getOwner().getModId();
                if (modid.equals("forge")) {
                    // workaround for https://github.com/ZeroNoRyouki/ZeroCore/issues/28
                    String desc = handler.toString();
                    if (desc.startsWith("ASM: ") && desc.contains("@")) {
                        String modClass = desc.split("@")[0].substring("ASM: ".length());
                        try {
                            Class<?> cl = Class.forName(modClass);
                            String newModid = cl.getPackage() == null ? null : getPackageToModId().get(cl.getPackage().getName());
                            if (newModid != null) {
                                modid = newModid;
                            }
                        } catch (ClassNotFoundException t) {
                        }
                    }
                }
                if (modIds.contains(modid)) {
                    newList.add(listener);
                }
            }
        }

        return newList.toArray(new IEventListener[0]);
    }

}
