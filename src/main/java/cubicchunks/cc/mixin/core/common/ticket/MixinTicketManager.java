package cubicchunks.cc.mixin.core.common.ticket;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.chunk.ticket.*;
import cubicchunks.cc.mixin.core.common.chunk.interfaces.InvokeChunkHolder;
import cubicchunks.cc.mixin.core.common.chunk.interfaces.InvokeChunkManager;
import cubicchunks.cc.mixin.core.common.ticket.interfaces.InvokeTicket;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.*;
import org.spongepowered.asm.mixin.*;
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
    private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> sectionTickets = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersBySectionPos = new Long2ObjectOpenHashMap<>();

    private final Set<ChunkHolder> cubeHolders = new HashSet<>();
    private final LongSet sectionPositions = new LongOpenHashSet();


    //@Final @Shadow private Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos;
    //@Final @Shadow private LongSet chunkPositions;
    @Final @Shadow private Executor field_219388_p;
    @Shadow private long currentTime;
    //@Final @Shadow private Set<ChunkHolder> chunkHolders;

    @Shadow protected static int getLevel(SortedArraySet<Ticket<?>> p_229844_0_) {
        throw new Error("Mixin did not apply correctly");
    }

    @Shadow protected abstract void register(long chunkPosIn, Ticket<?> ticketIn);

    private final CubeTicketTracker cc$ticketTracker = new CubeTicketTracker(this);
    private final PlayerCubeTracker cc$playerCubeTracker = new PlayerCubeTracker(this, 8);
    private final PlayerTicketTracker cc$playerTicketTracker = new PlayerTicketTracker(this, 33);
    private CubeTaskPriorityQueueSorter cc$levelUpdateListener;
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> cc$playerTicketThrottler;
    private ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> cc$playerTicketThrottlerSorter;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(Executor executor, Executor executor2, CallbackInfo ci) {
        ITaskExecutor<Runnable> itaskexecutor = ITaskExecutor.inline("player ticket throttler", executor2::execute);
        CubeTaskPriorityQueueSorter cubeTaskPriorityQueueSorter = new CubeTaskPriorityQueueSorter(ImmutableList.of(itaskexecutor), executor, 4);
        this.cc$levelUpdateListener = cubeTaskPriorityQueueSorter;
        this.cc$playerTicketThrottler = cubeTaskPriorityQueueSorter.createExecutor(itaskexecutor, true);
        this.cc$playerTicketThrottlerSorter = cubeTaskPriorityQueueSorter.createSorterExecutor(itaskexecutor);
    }

    private void registerSection(long sectionPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTicketSet(sectionPosIn);
        int i = getLevel(sortedarrayset);
        Ticket<?> ticket = sortedarrayset.func_226175_a_(ticketIn);
        ((InvokeTicket) ticket).setTimestampCC(this.currentTime);
        if (ticketIn.getLevel() < i) {
            this.cc$ticketTracker.updateSourceLevel(sectionPosIn, ticketIn.getLevel(), true);
        }
        this.register(SectionPos.from(sectionPosIn).asChunkPos().asLong(), ticketIn);
    }
    public void cc$register(long cubePos, Ticket<?> ticket)
    {
        this.registerSection(cubePos, ticket);
    }

    private void releaseSection(long sectionPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTicketSet(sectionPosIn);
        sortedarrayset.remove(ticketIn);

        if (sortedarrayset.isEmpty()) {
            this.sectionTickets.remove(sectionPosIn);
        }

        this.cc$ticketTracker.updateSourceLevel(sectionPosIn, getLevel(sortedarrayset), false);
    }
    public void cc$release(long cubePos, Ticket<?> ticket)
    {
        this.releaseSection(cubePos, ticket);
    }

    private SortedArraySet<Ticket<?>> getTicketSet(long p_229848_1_) {
        return this.sectionTickets.computeIfAbsent(p_229848_1_, (p_229851_0_) -> SortedArraySet.newSet(4));
    }

    protected void forceCube(SectionPos pos, boolean add) {
        Ticket<SectionPos> ticket = new Ticket<>(CCTicketType.CCFORCED, 31, pos);
        if (add) {
            this.registerSection(pos.asLong(), ticket);
        } else {
            this.releaseSection(pos.asLong(), ticket);
        }

    }

    protected String func_225413_c(long p_225413_1_) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.sectionTickets.get(p_225413_1_);
        String s;
        if (sortedarrayset != null && !sortedarrayset.isEmpty()) {
            s = sortedarrayset.getSmallest().toString();
        } else {
            s = "no_ticket";
        }

        return s;
    }

    protected void setViewDistance(int viewDistance) {
        this.cc$playerTicketTracker.setViewDistance(viewDistance);
    }

    //BEGIN INJECT

    @Inject(method = "processUpdates", at = @At("RETURN"), cancellable = true)
    public void processUpdates(ChunkManager chunkManager, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        this.cc$playerCubeTracker.processAllUpdates();
        this.cc$playerTicketTracker.processAllUpdates();
        int i = Integer.MAX_VALUE - this.cc$ticketTracker.update(Integer.MAX_VALUE);
        boolean flag = i != 0;
        if (!this.cubeHolders.isEmpty()) {
            this.cubeHolders.forEach((cubeHolder) -> ((InvokeChunkHolder) cubeHolder).processUpdatesCC(chunkManager));
            this.cubeHolders.clear();
            callbackInfoReturnable.setReturnValue(true);
            return;
        } else {
            if (!this.sectionPositions.isEmpty()) {
                LongIterator longiterator = this.sectionPositions.iterator();

                while (longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.getTicketSet(j).stream().anyMatch((p_219369_0_) -> p_219369_0_.getType() == CCTicketType.CCPLAYER)) {
                        ChunkHolder chunkholder = ((InvokeChunkManager) chunkManager).chunkHold(SectionPos.from(j).asChunkPos().asLong());
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> completablefuture = chunkholder.getEntityTickingFuture();
                        completablefuture.thenAccept((p_219363_3_) -> this.field_219388_p.execute(() -> {
                            this.cc$playerTicketThrottlerSorter.enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                            }, j, false));
                        }));
                    }
                }
                this.sectionPositions.clear();
            }
            callbackInfoReturnable.setReturnValue(flag | callbackInfoReturnable.getReturnValueZ());
            return;
        }
    }

    //BEGIN OVERWRITE

    //TODO: check if there is another way to do this
    /**
     * @author NotStirred
     * @reason idk & cba
     */
    @Overwrite
    protected void tick() {
        ++this.currentTime;
        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.sectionTickets.long2ObjectEntrySet().fastIterator();

        while (objectiterator.hasNext()) {
            Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> entry = objectiterator.next();
            if (entry.getValue().removeIf((ticket) -> ((InvokeTicket) ticket).cc$isexpired(this.currentTime))) {
                this.cc$ticketTracker.updateSourceLevel(entry.getLongKey(), getLevel(entry.getValue()), false);
            }
            if (entry.getValue().isEmpty()) {
                objectiterator.remove();
            }
        }
    }

    //BEGIN OVERRIDES
    @Override
    public <T> void registerWithLevel(TicketType<T> type, SectionPos pos, int level, T value) {
        this.registerSection(pos.asLong(), new Ticket<>(type, level, value));
    }

    @Override
    public <T> void releaseWithLevel(TicketType<T> type, SectionPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value);
        this.releaseSection(pos.asLong(), ticket);
    }

    @Override
    public <T> void register(TicketType<T> type, SectionPos pos, int distance, T value) {
        this.registerSection(pos.asLong(), new Ticket<>(type, 33 - distance, value));
    }

    @Override
    public <T> void release(TicketType<T> type, SectionPos pos, int distance, T value) {
        Ticket<T> ticket = new Ticket<>(type, 33 - distance, value);
        this.releaseSection(pos.asLong(), ticket);
    }

    @Override
    public void updatePlayerPosition(SectionPos sectionPosIn, ServerPlayerEntity player) {
        long i = sectionPosIn.asLong();
        this.playersBySectionPos.computeIfAbsent(i, (x) -> new ObjectOpenHashSet<>()).add(player);
        this.cc$playerCubeTracker.updateSourceLevel(i, 0, true);
        this.cc$playerTicketTracker.updateSourceLevel(i, 0, true);
    }

    @Override
    public void removePlayer(SectionPos sectionPosIn, ServerPlayerEntity player) {
        long i = sectionPosIn.asLong();
        ObjectSet<ServerPlayerEntity> objectset = this.playersBySectionPos.get(i);
        objectset.remove(player);
        if (objectset.isEmpty()) {
            this.playersBySectionPos.remove(i);
            this.cc$playerCubeTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
            this.cc$playerTicketTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
        }
    }

    /**
     * Returns the number of chunks taken into account when calculating the mob cap
     */
    @Override
    public int getSpawningCubesCount() {
        this.cc$playerCubeTracker.processAllUpdates();
        return this.cc$playerCubeTracker.cubesInRange.size();
    }

    @Override
    public boolean isSectionOutsideSpawningRadius(long sectionPosIn) {
        this.cc$playerCubeTracker.processAllUpdates();
        return this.cc$playerCubeTracker.cubesInRange.containsKey(sectionPosIn);
    }

    @Override
    public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getSectionTickets() {
        return sectionTickets;
    }

    @Override
    public ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getSectionPlayerTicketThrottler() {
        return cc$playerTicketThrottler;
    }

    @Override
    public ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> getplayerTicketThrottlerSorter() {
        return cc$playerTicketThrottlerSorter;
    }

    @Override
    public LongSet getSectionPositions() {
        return sectionPositions;
    }

    @Override
    public Set<ChunkHolder> getCubeHolders()
    {
        return this.cubeHolders;
    }

    @Override
    public Executor executor() {
        return field_219388_p;
    }

    @Override
    public CubeTaskPriorityQueueSorter getCubeTaskPriorityQueueSorter() {
        return cc$levelUpdateListener;
    }

    @Override
    public Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getPlayersBySectionPos()
    {
        return this.playersBySectionPos;
    }
}