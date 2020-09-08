package io.github.opencubicchunks.cubicchunks.mixin.core.common.ticket;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTicketTracker;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.ITicketManager;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.PlayerCubeTicketTracker;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.PlayerCubeTracker;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkHolderAccess;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.TicketAccess;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(TicketManager.class)
public abstract class MixinTicketManager implements ITicketManager {
    private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> cubeTickets = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersPerCube = new Long2ObjectOpenHashMap<>();

    private final Set<ChunkHolder> cubesToUpdateFutures = new HashSet<>();
    private final LongSet cubeTicketsToRelease = new LongOpenHashSet();

    @Final @Shadow private Executor mainThreadExecutor;
    @Shadow private long ticketTickCounter;

    @Shadow private static int getTicketLevelAt(SortedArraySet<Ticket<?>> p_229844_0_) {
        throw new Error("Mixin did not apply correctly");
    }

    private final CubeTicketTracker cubeTicketTracker = new CubeTicketTracker(this);
    private final PlayerCubeTracker naturalSpawnCubeCounter = new PlayerCubeTracker(this, 8);
    private final PlayerCubeTicketTracker playerCubeTicketTracker = new PlayerCubeTicketTracker(this, 33);
    private CubeTaskPriorityQueueSorter cubeTicketThrottler;
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> cubeTicketThrottlerInput;
    private ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> cubeTicketThrottlerReleaser;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(Executor executor, Executor executor2, CallbackInfo ci) {
        ITaskExecutor<Runnable> itaskexecutor = ITaskExecutor.of("player ticket throttler", executor2::execute);
        CubeTaskPriorityQueueSorter throttler = new CubeTaskPriorityQueueSorter(ImmutableList.of(itaskexecutor), executor, 4);
        this.cubeTicketThrottler = throttler;
        this.cubeTicketThrottlerInput = throttler.createExecutor(itaskexecutor, true);
        this.cubeTicketThrottlerReleaser = throttler.createSorterExecutor(itaskexecutor);
    }

    @Override
    public void addCubeTicket(long cubePosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getCubeTickets(cubePosIn);
        int i = getTicketLevelAt(sortedarrayset);
        Ticket<?> ticket = sortedarrayset.addOrGet(ticketIn);
        ((TicketAccess) ticket).invokeSetCreatedTick(this.ticketTickCounter);
        if (ticketIn.getTicketLevel() < i) {
            this.cubeTicketTracker.updateSourceLevel(cubePosIn, ticketIn.getTicketLevel(), true);
        }
    }

    @Override
    public void removeCubeTicket(long cubePosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getCubeTickets(cubePosIn);
        sortedarrayset.remove(ticketIn);

        if (sortedarrayset.isEmpty()) {
            this.cubeTickets.remove(cubePosIn);
        }

        this.cubeTicketTracker.updateSourceLevel(cubePosIn, getTicketLevelAt(sortedarrayset), false);
        // TODO: release chunk tickets when releasing cube tickets
    }

    private SortedArraySet<Ticket<?>> getCubeTickets(long cubePos) {
        return this.cubeTickets.computeIfAbsent(cubePos, (p_229851_0_) -> SortedArraySet.create(4));
    }

    @Inject(method = "updatePlayerTickets", at = @At("HEAD"))
    protected void onUpdatePlayerTickets(int viewDistance, CallbackInfo ci) {
        this.playerCubeTicketTracker.updateCubeViewDistance(Coords.sectionToCubeRenderDistance(viewDistance));
    }

    //BEGIN INJECT

    @Inject(method = "runAllUpdates", at = @At("RETURN"), cancellable = true)
    public void processUpdates(ChunkManager chunkManager, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        // Minecraft.getInstance().getIntegratedServer().getProfiler().startSection("cubeTrackerUpdates");
        this.naturalSpawnCubeCounter.processAllUpdates();
        // Minecraft.getInstance().getIntegratedServer().getProfiler().endStartSection("cubeTicketTrackerUpdates");
        this.playerCubeTicketTracker.processAllUpdates();
        // Minecraft.getInstance().getIntegratedServer().getProfiler().endStartSection("cubeTicketTracker");
        int i = Integer.MAX_VALUE - this.cubeTicketTracker.update(Integer.MAX_VALUE);
        // Minecraft.getInstance().getIntegratedServer().getProfiler().endStartSection("cubeHolderTick");
        boolean flag = i != 0;
        if (!this.cubesToUpdateFutures.isEmpty()) {
            this.cubesToUpdateFutures.forEach((cubeHolder) -> ((ChunkHolderAccess) cubeHolder).invokeUpdateFutures(chunkManager));
            this.cubesToUpdateFutures.clear();
            callbackInfoReturnable.setReturnValue(true);
            //Minecraft.getInstance().getIntegratedServer().getProfiler().endSection();// cubeHolderTick
            return;
        } else {
            if (!this.cubeTicketsToRelease.isEmpty()) {
                LongIterator longiterator = this.cubeTicketsToRelease.iterator();

                while (longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.getCubeTickets(j).stream().anyMatch((p_219369_0_) -> p_219369_0_.getType() == CCTicketType.CCPLAYER)) {
                        ChunkHolder chunkholder = ((IChunkManager) chunkManager).getCubeHolder(j);
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> sectionEntityTickingFuture =
                                ((ICubeHolder)chunkholder).getCubeEntityTickingFuture();
                        sectionEntityTickingFuture.thenAccept((p_219363_3_) -> this.mainThreadExecutor.execute(() -> {
                            this.cubeTicketThrottlerReleaser.tell(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                            }, j, false));
                        }));
                    }
                }
                this.cubeTicketsToRelease.clear();
            }
            callbackInfoReturnable.setReturnValue(flag | callbackInfoReturnable.getReturnValueZ());
        }
        //Minecraft.getInstance().getIntegratedServer().getProfiler().endSection();// cubeHolderTick
    }

    //BEGIN OVERWRITE

    /**
     * @author NotStirred
     * @reason CC must also update it's tracker&tickets
     */
    @Inject(method = "purgeStaleTickets", at = @At("RETURN"))
    protected void purgeStaleCubeTickets(CallbackInfo ci) {
        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.cubeTickets.long2ObjectEntrySet().fastIterator();
        while (objectiterator.hasNext()) {
            Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> entry = objectiterator.next();
            if (entry.getValue().removeIf((ticket) -> ((TicketAccess) ticket).invokeTimedOut(this.ticketTickCounter))) {
                this.cubeTicketTracker.updateSourceLevel(entry.getLongKey(), getTicketLevelAt(entry.getValue()), false);
            }
            if (entry.getValue().isEmpty()) {
                objectiterator.remove();
            }
        }
    }

    //BEGIN OVERRIDES
    @Override
    public <T> void addCubeTicket(TicketType<T> type, CubePos pos, int level, T value) {
        this.addCubeTicket(pos.asLong(), new Ticket<>(type, level, value));
    }

    @Override
    public <T> void removeCubeTicket(TicketType<T> type, CubePos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value);
        this.removeCubeTicket(pos.asLong(), ticket);
    }

    @Override
    public <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value) {
        this.addCubeTicket(pos.asLong(), new Ticket<>(type, 33 - distance, value));
    }

    @Override
    public <T> void removeCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value) {
        Ticket<T> ticket = new Ticket<>(type, 33 - distance, value);
        this.removeCubeTicket(pos.asLong(), ticket);
    }

    // updateChunkForced
    @Override
    public void updateCubeForced(CubePos pos, boolean add) {
        Ticket<CubePos> ticket = new Ticket<>(CCTicketType.CCFORCED, 31, pos);
        if (add) {
            this.addCubeTicket(pos.asLong(), ticket);
        } else {
            this.removeCubeTicket(pos.asLong(), ticket);
        }
    }

    @Override
    public void addCubePlayer(CubePos cubePos, ServerPlayerEntity player) {
        long i = cubePos.asLong();
        this.playersPerCube.computeIfAbsent(i, (x) -> new ObjectOpenHashSet<>()).add(player);
        this.naturalSpawnCubeCounter.updateSourceLevel(i, 0, true);
        this.playerCubeTicketTracker.updateSourceLevel(i, 0, true);
    }

    @Override
    public void removeCubePlayer(CubePos cubePosIn, ServerPlayerEntity player) {
        long i = cubePosIn.asLong();
        ObjectSet<ServerPlayerEntity> objectset = this.playersPerCube.get(i);
        objectset.remove(player);
        if (objectset.isEmpty()) {
            this.playersPerCube.remove(i);
            this.naturalSpawnCubeCounter.updateSourceLevel(i, Integer.MAX_VALUE, false);
            this.playerCubeTicketTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
        }
    }

    /**
     * Returns the number of chunks taken into account when calculating the mob cap
     */
    @Override
    public int getNaturalSpawnCubeCount() {
        this.naturalSpawnCubeCounter.processAllUpdates();
        return this.naturalSpawnCubeCounter.cubesInRange.size();
    }

    @Override
    public boolean hasPlayersNearbyCube(long cubePosIn) {
        this.naturalSpawnCubeCounter.processAllUpdates();
        return this.naturalSpawnCubeCounter.cubesInRange.containsKey(cubePosIn);
    }

    @Override
    public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getCubeTickets() {
        return cubeTickets;
    }

    @Override
    public ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getCubeTicketThrottlerInput() {
        return cubeTicketThrottlerInput;
    }

    @Override
    public ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> getCubeTicketThrottlerReleaser() { return cubeTicketThrottlerReleaser; }

    @Override
    public LongSet getCubeTicketsToRelease() {
        return cubeTicketsToRelease;
    }

    @Override
    public Set<ChunkHolder> getCubesToUpdateFutures()
    {
        return this.cubesToUpdateFutures;
    }

    @Override
    public Executor getMainThreadExecutor() {
        return mainThreadExecutor;
    }

    @Override
    public CubeTaskPriorityQueueSorter getCubeTicketThrottler() {
        return cubeTicketThrottler;
    }

    @Override
    public Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getPlayersPerCube()
    {
        return this.playersPerCube;
    }
}