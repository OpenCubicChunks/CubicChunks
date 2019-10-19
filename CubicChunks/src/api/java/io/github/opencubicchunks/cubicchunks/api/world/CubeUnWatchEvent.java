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
package io.github.opencubicchunks.cubicchunks.api.world;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * This event fired from server thread every time player stop watching 
 * (stop sending updates to client) {@link ICube} associated with {@link ICubeWatcher}.
 * This event can be fired several times for a same cube during game session
 * for a same and for a different players.
 *
 * This event may be fired even if there was no corresponding {@link CubeWatchEvent}.
 * This matches Forge behavior with {@link net.minecraftforge.event.world.ChunkWatchEvent.UnWatch} because
 * UnWatch event is fired even if chunk has never been sent to players, but Watch event is fired only
 * after the chunk packet has been sent.
 * This event is fired after the corresponding unload packet has been sent to the player.
 *
 *
 * This is not an {@link #isCancelable()} event.
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class CubeUnWatchEvent extends Event {

    @Nullable private final ICube cube;
    private final CubePos cubePos;
    private final ICubeWatcher cubeWatcher;
    private final EntityPlayerMP player;

    public CubeUnWatchEvent(@Nullable ICube cubeIn, CubePos cubePosIn, ICubeWatcher cubeWatcherIn, EntityPlayerMP playerIn) {
        super();
        cube = cubeIn;
        cubePos = cubePosIn;
        cubeWatcher = cubeWatcherIn;
        player = playerIn;
    }

    @Nullable
    public ICube getCube() {
        return cube;
    }
    
    public CubePos getCubePos() {
        return cubePos;
    }
    
    public ICubeWatcher getCubeWatcher() {
        return cubeWatcher;
    }

    public ICubicWorld getWorld() {
        return (ICubicWorld) player.world;
    }
    
    public EntityPlayerMP getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }
}
