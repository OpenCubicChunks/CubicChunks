/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2018 @PhiPro95 and @Mathe172
 *  Copyright (c) 2021 @jellysquid_
 *  Copyright (c) 2021 OpenCubicChunks
 *  Copyright (c) 2021 contributors
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
package io.github.opencubicchunks.cubicchunks.core.lighting.phosphor;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToLocal;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.lighting.phosphor.LightingHooks.EnumBoundaryFacing;
import io.github.opencubicchunks.cubicchunks.core.world.IColumnInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.concurrent.locks.ReentrantLock;

public class PhosphorLightEngine {
    public static final boolean ENABLE_ILLEGAL_THREAD_ACCESS_WARNINGS = true;

    private static final int MAX_SCHEDULED_COUNT = 1 << 22;

    private static final int MAX_LIGHT = 15;

    private final Thread ownedThread = Thread.currentThread();
    private final World world;
    private final Profiler profiler;

    // value=padding
    private final PooledLightUpdateQueue[] queuedLightUpdates = new PooledLightUpdateQueue[EnumSkyBlock.values().length];

    // value=padding
    private final PooledLightUpdateQueue[] queuedDarkenings = new PooledLightUpdateQueue[MAX_LIGHT + 1];
    private final PooledLightUpdateQueue[] queuedBrightenings = new PooledLightUpdateQueue[MAX_LIGHT + 1];

    // value=newLight
    private final PooledLightUpdateQueue initialBrightenings;
    // value=padding
    private final PooledLightUpdateQueue initialDarkenings;

    private boolean updating = false;

    private static final long neighborShiftsX, neighborShiftsY, neighborShiftsZ;

    static {
        long sx = 0, sy = 0, sz = 0;
        for (int i = 0; i < 6; ++i) {
            final Vec3i offset = EnumFacing.VALUES[i].getDirectionVec();
            sx |= (offset.getX() & 0xFFL) << (i * 8);
            sy |= (offset.getY() & 0xFFL) << (i * 8);
            sz |= (offset.getZ() & 0xFFL) << (i * 8);
        }
        neighborShiftsX = sx;
        neighborShiftsY = sy;
        neighborShiftsZ = sz;
    }

    //Iteration state data
    //Cache position to avoid allocation of new object each time
    private final MutableBlockPos curPos = new MutableBlockPos();
    private ICube curCube;
    private int curCubeIdentifierX, curCubeIdentifierY, curCubeIdentifierZ;
    private int curDataX, curDataY, curDataZ, curDataVal;

    //Cached data about neighboring blocks (of tempPos)
    private boolean isNeighborDataValid = false;

    private final NeighborInfo[] neighborInfos = new NeighborInfo[6];
    private PooledLightUpdateQueue.LightUpdateQueueIterator queueIt;

    private final ReentrantLock lock = new ReentrantLock();

    public PhosphorLightEngine(final World world) {
        this.world = world;
        this.profiler = world.profiler;

        PooledLightUpdateQueue.Pool pool = new PooledLightUpdateQueue.Pool();

        this.initialBrightenings = new PooledLightUpdateQueue(pool);
        this.initialDarkenings = new PooledLightUpdateQueue(pool);

        for (int i = 0; i < EnumSkyBlock.values().length; ++i) {
            this.queuedLightUpdates[i] = new PooledLightUpdateQueue(pool);
        }

        for (int i = 0; i < this.queuedDarkenings.length; ++i) {
            this.queuedDarkenings[i] = new PooledLightUpdateQueue(pool);
        }

        for (int i = 0; i < this.queuedBrightenings.length; ++i) {
            this.queuedBrightenings[i] = new PooledLightUpdateQueue(pool);
        }

        for (int i = 0; i < this.neighborInfos.length; ++i) {
            this.neighborInfos[i] = new NeighborInfo();
        }
    }

    /**
     * Schedules a light update for the specified light type and position to be processed later by
     * {@link #processLightUpdatesForType(EnumSkyBlock)}
     */
    public void scheduleLightUpdate(final EnumSkyBlock lightType, final BlockPos pos) {
        this.acquireLock();

        try {
            this.scheduleLightUpdate(lightType, pos.getX(), pos.getY(), pos.getZ());
        } finally {
            this.releaseLock();
        }
    }

    /**
     * Schedules a light update for the specified light type and position to be processed later by {@link #processLightUpdates()}
     */
    private void scheduleLightUpdate(final EnumSkyBlock lightType, final int x, final int y, final int z) {
        final PooledLightUpdateQueue queue = this.queuedLightUpdates[lightType.ordinal()];

        queue.add(x, y, z, 0);

        //make sure there are not too many queued light updates
        if (queue.size() >= MAX_SCHEDULED_COUNT) {
            this.processLightUpdatesForType(lightType);
        }
    }

    public boolean hasLightUpdates() {
        return !this.queuedLightUpdates[0].isEmpty() || !this.queuedLightUpdates[1].isEmpty();
    }

    /**
     * Calls {@link #processLightUpdatesForType(EnumSkyBlock)} for both light types
     *
     */
    public void processLightUpdates() {
        this.processLightUpdatesForType(EnumSkyBlock.SKY);
        this.processLightUpdatesForType(EnumSkyBlock.BLOCK);
    }

    /**
     * Processes light updates of the given light type
     */
    public void processLightUpdatesForType(final EnumSkyBlock lightType) {
        // We only want to perform updates if we're being called from a tick event on the client
        // There are many locations in the client code which will end up making calls to this method, usually from
        // other threads.
        if (this.world.isRemote && !ClientUtils.isCallingFromMainThread()) {
            return;
        }

        final PooledLightUpdateQueue queue = this.queuedLightUpdates[lightType.ordinal()];

        // Quickly check if the queue is empty before we acquire a more expensive lock.
        if (queue.isEmpty()) {
            return;
        }

        this.acquireLock();

        try {
            this.processLightUpdatesForTypeInner(lightType, queue);
        } finally {
            this.updating = false;
            this.releaseLock();
        }
    }

    private void acquireLock() {
        if (!this.lock.tryLock()) {
            // If we cannot lock, something has gone wrong... Only one thread should ever acquire the lock.
            // Validate that we're on the right thread immediately so we can gather information.
            // It is NEVER valid to call World methods from a thread other than the owning thread of the world instance.
            // Users can safely disable this warning, however it will not resolve the issue.
            if (ENABLE_ILLEGAL_THREAD_ACCESS_WARNINGS) {
                Thread current = Thread.currentThread();

                if (current != this.ownedThread) {
                    IllegalAccessException e = new IllegalAccessException(String.format("World is owned by '%s' (ID: %s)," +
                                    " but was accessed from thread '%s' (ID: %s)",
                            this.ownedThread.getName(), this.ownedThread.getId(), current.getName(), current.getId()));

                    CubicChunks.LOGGER.warn(
                            "Something (likely another mod) has attempted to modify the world's state from the wrong thread!\n" +
                                    "This is *bad practice* and can cause severe issues in your game. Phosphor has done as best as it can to "
                                    + "mitigate this violation,"
                                    +
                                    " but it may negatively impact performance or introduce stalls.\nIn a future release, this violation may result"
                                    + " in a hard crash instead"
                                    +
                                    " of the current soft warning. You should report this issue to our issue tracker with the following stacktrace "
                                    + "information.\n(If you are"
                                    +
                                    " aware you have misbehaving mods and cannot resolve this issue, you can safely disable this warning by setting" +
                                    " `enable_illegal_thread_access_warnings` to `false` in Phosphor's configuration file for the time being.)", e);

                }

            }

            // Wait for the lock to be released. This will likely introduce unwanted stalls, but will mitigate the issue.
            this.lock.lock();
        }
    }

    private void releaseLock() {
        this.lock.unlock();
    }

    private void processLightUpdatesForTypeInner(final EnumSkyBlock lightType, final PooledLightUpdateQueue queue) {
        //avoid nested calls
        if (this.updating) {
            throw new IllegalStateException("Already processing updates!");
        }

        this.updating = true;

        this.curCubeIdentifierX = Integer.MIN_VALUE; //reset chunk cache
        this.curCubeIdentifierY = Integer.MIN_VALUE;
        this.curCubeIdentifierZ = Integer.MIN_VALUE;

        this.profiler.startSection("lighting");

        this.profiler.startSection("checking");

        this.queueIt = queue.iterator();

        //process the queued updates and enqueue them for further processing
        while (this.nextItem()) {
            if (this.curCube == null) {
                continue;
            }

            final int oldLight = this.getCursorCachedLight(lightType);
            final int newLight = this.calculateNewLightFromCursor(lightType);

            if (oldLight < newLight) {
                //don't enqueue directly for brightening in order to avoid duplicate scheduling
                this.initialBrightenings.add(this.curDataX, this.curDataY, this.curDataZ, newLight);
            } else if (oldLight > newLight) {
                //don't enqueue directly for darkening in order to avoid duplicate scheduling
                this.initialDarkenings.add(this.curDataX, this.curDataY, this.curDataZ, 0);
            }
        }

        this.queueIt = this.initialBrightenings.iterator();

        while (this.nextItem()) {
            final int newLight = this.curDataVal;

            if (newLight > this.getCursorCachedLight(lightType)) {
                //Sets the light to newLight to only schedule once. Clear leading bits of curData for later
                this.enqueueBrightening(this.curPos, this.curDataX, this.curDataY, this.curDataZ, newLight, this.curCube, lightType);
            }
        }

        this.queueIt = this.initialDarkenings.iterator();

        while (this.nextItem()) {
            final int oldLight = this.getCursorCachedLight(lightType);

            if (oldLight != 0) {
                //Sets the light to 0 to only schedule once
                this.enqueueDarkening(this.curPos, this.curDataX, this.curDataY, this.curDataZ, oldLight, this.curCube, lightType);
            }
        }

        this.profiler.endSection();

        //Iterate through enqueued updates (brightening and darkening in parallel) from brightest to darkest so that we only need to iterate once
        for (int curLight = MAX_LIGHT; curLight >= 0; --curLight) {
            this.profiler.startSection("darkening");

            this.queueIt = this.queuedDarkenings[curLight].iterator();

            while (this.nextItem()) {
                if (this.getCursorCachedLight(lightType) >= curLight) { //don't darken if we got brighter due to some other change
                    continue;
                }
                final IBlockState state = LightingEngineHelpers.posToState(this.curPos, this.curCube);
                final int luminosity = this.getCursorLuminosity(state, lightType);
                final int opacity; //if luminosity is high enough, opacity is irrelevant

                if (luminosity >= MAX_LIGHT - 1) {
                    opacity = 1;
                } else {
                    opacity = this.getPosOpacity(this.curPos, state);
                }

                //only darken neighbors if we indeed became darker
                if (this.calculateNewLightFromCursor(luminosity, opacity, lightType) < curLight) {
                    //need to calculate new light value from neighbors IGNORING neighbors which are scheduled for darkening
                    int newLight = luminosity;

                    this.fetchNeighborDataFromCursor(lightType);

                    NeighborInfo[] infos = this.neighborInfos;
                    for (int i = 0; i < infos.length; i++) {
                        NeighborInfo info = infos[i];
                        final ICube nCube = info.cube;

                        if (nCube == null) {
                            LightingHooks.flagSecBoundaryForUpdate(this.curCube, this.curPos, lightType, EnumFacing.VALUES[i], EnumBoundaryFacing.OUT);
                            continue;
                        }

                        final int nLight = info.light;

                        if (nLight == 0) {
                            continue;
                        }

                        final MutableBlockPos nPos = info.pos;

                        //schedule neighbor for darkening if we possibly light it
                        if (curLight - this.getPosOpacity(nPos, LightingEngineHelpers.posToState(nPos, info.section)) >= nLight) {
                            this.enqueueDarkening(nPos, info.blockX, info.blockY, info.blockZ, nLight, nCube, lightType);
                        } else { //only use for new light calculation if not
                            //if we can't darken the neighbor, no one else can (because of processing order) -> safe to let us be illuminated by it
                            newLight = Math.max(newLight, nLight - opacity);
                        }
                    }

                    //schedule brightening since light level was set to 0
                    this.enqueueBrighteningFromCursor(newLight, lightType);
                } else { //we didn't become darker, so we need to re-set our initial light value (was set to 0) and notify neighbors
                    //do not spread to neighbors immediately to avoid scheduling multiple times
                    this.enqueueBrighteningFromCursor(curLight, lightType);
                }
            }

            this.profiler.endStartSection("brightening");

            this.queueIt = this.queuedBrightenings[curLight].iterator();

            while (this.nextItem()) {
                final int oldLight = this.getCursorCachedLight(lightType);

                if (oldLight == curLight) { //only process this if nothing else has happened at this position since scheduling
                    this.world.notifyLightSet(this.curPos);

                    if (curLight > 1) {
                        this.spreadLightFromCursor(curLight, lightType);
                    }
                }
            }

            this.profiler.endSection();
        }

        this.profiler.endSection();
    }

    /**
     * Gets data for neighbors of <code>curPos</code> and saves the results into neighbor state data members. If a neighbor can't be
     * accessed/doesn't exist, the corresponding entry in <code>neighborChunks</code> is <code>null</code> - others are not reset
     */
    private void fetchNeighborDataFromCursor(final EnumSkyBlock lightType) {
        //only update if curPos was changed
        if (this.isNeighborDataValid) {
            return;
        }

        this.isNeighborDataValid = true;

        for (int i = 0; i < this.neighborInfos.length; ++i) {
            NeighborInfo info = this.neighborInfos[i];

            final int bitIdx = i << 3;
            final int nPosX = info.blockX = this.curDataX + (byte) (neighborShiftsX >> bitIdx);
            final int nPosY = info.blockY = this.curDataY + (byte) (neighborShiftsY >> bitIdx);
            final int nPosZ = info.blockZ = this.curDataZ + (byte) (neighborShiftsZ >> bitIdx);

            final MutableBlockPos nPos = info.pos.setPos(nPosX, nPosY, nPosZ);

            final ICube nCube;

            final int nCubeX = blockToCube(nPosX);
            final int nCubeY = blockToCube(nPosY);
            final int nCubeZ = blockToCube(nPosZ);

            if (nCubeX == this.curCubeIdentifierX && nCubeY == this.curCubeIdentifierY && nCubeZ == this.curCubeIdentifierZ) {
                nCube = info.cube = this.curCube;
            } else {
                nCube = info.cube = this.getCube(nPos);
            }

            if (nCube != null) {
                ExtendedBlockStorage nSection = nCube.getStorage();

                info.light = getCachedLightFor(nCube, nSection, nPos, lightType);
                info.section = nSection;
            }
        }
    }

    private static boolean canSeeSky(ICube cube, BlockPos pos) {
        int topY = ((IColumnInternal) cube.getColumn()).getTopYWithStaging(blockToLocal(pos.getX()), blockToLocal(pos.getZ()));
        return pos.getY() > topY;
    }

    private static int getCachedLightFor(ICube cube, ExtendedBlockStorage storage, BlockPos pos, EnumSkyBlock type) {
        int localX = Coords.blockToLocal(pos.getX());
        int localY = Coords.blockToLocal(pos.getY());
        int localZ = Coords.blockToLocal(pos.getZ());

        if (storage == Chunk.NULL_BLOCK_STORAGE) {
            if (type == EnumSkyBlock.SKY && canSeeSky(cube, pos)) {
                return type.defaultLightValue;
            } else {
                return 0;
            }
        } else if (type == EnumSkyBlock.SKY) {
            if (!cube.getWorld().provider.hasSkyLight()) {
                return 0;
            } else {
                return storage.getSkyLight(localX, localY, localZ);
            }
        } else {
            if (type == EnumSkyBlock.BLOCK) {
                return storage.getBlockLight(localX, localY, localZ);
            } else {
                return type.defaultLightValue;
            }
        }
    }

    private int calculateNewLightFromCursor(final EnumSkyBlock lightType) {
        final IBlockState state = LightingEngineHelpers.posToState(this.curPos, this.curCube);

        final int luminosity = this.getCursorLuminosity(state, lightType);
        final int opacity;

        if (luminosity >= MAX_LIGHT - 1) {
            opacity = 1;
        } else {
            opacity = this.getPosOpacity(this.curPos, state);
        }

        return this.calculateNewLightFromCursor(luminosity, opacity, lightType);
    }

    private int calculateNewLightFromCursor(final int luminosity, final int opacity, final EnumSkyBlock lightType) {
        if (luminosity >= MAX_LIGHT - opacity) {
            return luminosity;
        }

        int newLight = luminosity;

        this.fetchNeighborDataFromCursor(lightType);

        NeighborInfo[] infos = this.neighborInfos;
        for (int i = 0; i < infos.length; i++) {
            NeighborInfo info = infos[i];
            if (info.cube == null) {
                LightingHooks.flagSecBoundaryForUpdate(this.curCube, this.curPos, lightType, EnumFacing.VALUES[i], EnumBoundaryFacing.IN);
                continue;
            }

            final int nLight = info.light;

            newLight = Math.max(nLight - opacity, newLight);
        }

        return newLight;
    }

    private void spreadLightFromCursor(final int curLight, final EnumSkyBlock lightType) {
        this.fetchNeighborDataFromCursor(lightType);

        NeighborInfo[] infos = this.neighborInfos;
        for (int i = 0; i < infos.length; i++) {
            NeighborInfo info = infos[i];
            final ICube nCube = info.cube;

            if (nCube == null) {
                LightingHooks.flagSecBoundaryForUpdate(this.curCube, this.curPos, lightType, EnumFacing.VALUES[i], EnumBoundaryFacing.OUT);
                continue;
            }

            final int newLight = curLight - this.getPosOpacity(info.pos, LightingEngineHelpers.posToState(info.pos, info.section));

            if (newLight > info.light) {
                this.enqueueBrightening(info.pos, info.blockX, info.blockY, info.blockZ, newLight, nCube, lightType);
            }
        }
    }

    private void enqueueBrighteningFromCursor(final int newLight, final EnumSkyBlock lightType) {
        this.enqueueBrightening(this.curPos, this.curDataX, this.curDataY, this.curDataZ, newLight, this.curCube, lightType);
    }

    /**
     * Enqueues the pos for brightening and sets its light value to <code>newLight</code>
     */
    private void enqueueBrightening(final BlockPos pos,
            final int posX, final int posY, final int posZ, final int newLight,
            final ICube cube, final EnumSkyBlock lightType) {
        this.queuedBrightenings[newLight].add(posX, posY, posZ, newLight);

        cube.setLightFor(lightType, pos, newLight);
    }

    /**
     * Enqueues the pos for darkening and sets its light value to 0
     */
    private void enqueueDarkening(final BlockPos pos,
            final int posX, final int posY, final int posZ, final int oldLight,
            final ICube cube, final EnumSkyBlock lightType) {
        this.queuedDarkenings[oldLight].add(posX, posY, posZ, 0);

        cube.setLightFor(lightType, pos, 0);
    }

    private static int ITEMS_PROCESSED = 0, CHUNKS_FETCHED = 0;

    /**
     * Polls a new item from <code>curQueue</code> and fills in state data members
     *
     * @return If there was an item to poll
     */
    private boolean nextItem() {
        if (!this.queueIt.hasNext()) {
            this.queueIt.finish();
            this.queueIt = null;

            return false;
        }

        this.curDataX = this.queueIt.x();
        this.curDataY = this.queueIt.y();
        this.curDataZ = this.queueIt.z();
        this.curDataVal = this.queueIt.val();
        this.queueIt.next();
        this.isNeighborDataValid = false;

        this.curPos.setPos(this.curDataX, this.curDataY, this.curDataZ);

        final int cubeX = blockToCube(this.curDataX);
        final int cubeY = blockToCube(this.curDataY);
        final int cubeZ = blockToCube(this.curDataZ);

        if (this.curCubeIdentifierX != cubeX || this.curCubeIdentifierY != cubeY || this.curCubeIdentifierZ != cubeZ) {
            this.curCube = this.getCube(this.curPos);
            this.curCubeIdentifierX = cubeX;
            this.curCubeIdentifierY = cubeY;
            this.curCubeIdentifierZ = cubeZ;
            CHUNKS_FETCHED++;
        }

        ITEMS_PROCESSED++;

        return true;
    }

    private int getCursorCachedLight(final EnumSkyBlock lightType) {
        return ((Cube) this.curCube).getCachedLightFor(lightType, this.curPos);
    }

    /**
     * Calculates the luminosity for <code>curPos</code>, taking into account <code>lightType</code>
     */
    private int getCursorLuminosity(final IBlockState state, final EnumSkyBlock lightType) {
        if (lightType == EnumSkyBlock.SKY) {
            if (canSeeSky(this.curCube, this.curPos)) {
                return EnumSkyBlock.SKY.defaultLightValue;
            } else {
                return 0;
            }
        }

        return MathHelper.clamp(state.getLightValue(this.world, this.curPos), 0, MAX_LIGHT);
    }

    private int getPosOpacity(final BlockPos pos, final IBlockState state) {
        return MathHelper.clamp(state.getLightOpacity(this.world, pos), 1, MAX_LIGHT);
    }

    private ICube getCube(final BlockPos pos) {
        return ((ICubeProvider) this.world.getChunkProvider())
                .getLoadedCube(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()));
    }

    private static class NeighborInfo {

        ICube cube;
        ExtendedBlockStorage section;

        int light;

        int blockX, blockY, blockZ;

        final MutableBlockPos pos = new MutableBlockPos();
    }


    static class ClientUtils {
        static boolean isCallingFromMainThread() {
            return Minecraft.getMinecraft().isCallingFromMinecraftThread();
        }
    }
}
