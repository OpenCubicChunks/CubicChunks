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
package cubicchunks.api;

import javax.annotation.Nullable;

import cubicchunks.server.CubeWatcher;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * This event fired from server thread every time player start to watch 
 * (sent updates to client) {@link Cube} associated with {@link CubeWatcher}. 
 * This event can be fired several times for a same cube during game session
 * for a same and for a different players.
 *
 * This is not an {@link #isCancelable()} event.
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class CubeWatchEvent extends Event {

    @Nullable private final Cube cube;
    private final CubePos cubePos;
    private final CubeWatcher cubeWatcher;
    private final EntityPlayerMP player;

    public CubeWatchEvent(@Nullable Cube cubeIn, CubePos cubePosIn, CubeWatcher cubeWatcherIn, EntityPlayerMP playerIn) {
        super();
        cube = cubeIn;
        cubePos = cubePosIn;
        cubeWatcher = cubeWatcherIn;
        player = playerIn;
    }

    @Nullable
    public Cube getCube() {
        return cube;
    }
    
    public CubePos getCubePos() {
        return cubePos;
    }
    
    public CubeWatcher getCubeWatcher() {
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
