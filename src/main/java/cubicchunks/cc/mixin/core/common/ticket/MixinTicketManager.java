package cubicchunks.cc.mixin.core.common.ticket;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.ISectionHolder;
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
import net.minecraft.world.chunk.ChunkSection;
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

    private final Set<ChunkHolder> sectionHolders = new HashSet<>();
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

    private final SectionTicketTracker sectionTicketTracker = new SectionTicketTracker(this);
    private final PlayerSectionTracker playerSectionTracker = new PlayerSectionTracker(this, 8);
    private final PlayerSectionTicketTracker playerSectionTicketTracker = new PlayerSectionTicketTracker(this, 33);
    private SectionTaskPriorityQueueSorter sectionTaskPriorityQueueSorter;
    private ITaskExecutor<SectionTaskPriorityQueueSorter.FunctionEntry<Runnable>> playerSectionTicketThrottler;
    private ITaskExecutor<SectionTaskPriorityQueueSorter.RunnableEntry> playerSectionTicketThrottlerSorter;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(Executor executor, Executor executor2, CallbackInfo ci) {
        ITaskExecutor<Runnable> itaskexecutor = ITaskExecutor.inline("player ticket throttler", executor2::execute);
        SectionTaskPriorityQueueSorter
                sectionTaskPriorityQueueSorter = new SectionTaskPriorityQueueSorter(ImmutableList.of(itaskexecutor), executor, 4);
        this.sectionTaskPriorityQueueSorter = sectionTaskPriorityQueueSorter;
        this.playerSectionTicketThrottler = sectionTaskPriorityQueueSorter.createExecutor(itaskexecutor, true);
        this.playerSectionTicketThrottlerSorter = sectionTaskPriorityQueueSorter.createSorterExecutor(itaskexecutor);
    }

    private void registerSection(long sectionPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getSectionTicketSet(sectionPosIn);
        int i = getLevel(sortedarrayset);
        Ticket<?> ticket = sortedarrayset.func_226175_a_(ticketIn);
        ((InvokeTicket) ticket).setTimestampCC(this.currentTime);
        if (ticketIn.getLevel() < i) {
            this.sectionTicketTracker.updateSourceLevel(sectionPosIn, ticketIn.getLevel(), true);
        }
        this.register(SectionPos.from(sectionPosIn).asChunkPos().asLong(), ticketIn);
    }

    private void releaseSection(long sectionPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getSectionTicketSet(sectionPosIn);
        sortedarrayset.remove(ticketIn);

        if (sortedarrayset.isEmpty()) {
            this.sectionTickets.remove(sectionPosIn);
        }

        this.sectionTicketTracker.updateSourceLevel(sectionPosIn, getLevel(sortedarrayset), false);
    }

    private SortedArraySet<Ticket<?>> getSectionTicketSet(long sectionPos) {
        return this.sectionTickets.computeIfAbsent(sectionPos, (p_229851_0_) -> SortedArraySet.newSet(4));
    }

    @Inject(method = "setViewDistance", at = @At("HEAD"))
    protected void setViewDistance(int viewDistance, CallbackInfo ci)
    {
        this.playerSectionTicketTracker.setViewDistance(viewDistance);
    }

    //BEGIN INJECT

    @Inject(method = "processUpdates", at = @At("RETURN"), cancellable = true)
    public void processUpdates(ChunkManager chunkManager, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        this.playerSectionTracker.processAllUpdates();
        this.playerSectionTicketTracker.processAllUpdates();
        int i = Integer.MAX_VALUE - this.sectionTicketTracker.update(Integer.MAX_VALUE);
        boolean flag = i != 0;
        if (!this.sectionHolders.isEmpty()) {
            this.sectionHolders.forEach((cubeHolder) -> ((InvokeChunkHolder) cubeHolder).processUpdatesCC(chunkManager));
            this.sectionHolders.clear();
            callbackInfoReturnable.setReturnValue(true);
            return;
        } else {
            if (!this.sectionPositions.isEmpty()) {
                LongIterator longiterator = this.sectionPositions.iterator();

                while (longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.getSectionTicketSet(j).stream().anyMatch((p_219369_0_) -> p_219369_0_.getType() == CCTicketType.CCPLAYER)) {
                        ChunkHolder chunkholder = ((InvokeChunkManager) chunkManager).chunkHold(SectionPos.from(j).asChunkPos().asLong());
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> sectionEntityTickingFuture =
                                ((ISectionHolder)chunkholder).getSectionEntityTickingFuture();
                        sectionEntityTickingFuture.thenAccept((p_219363_3_) -> this.field_219388_p.execute(() -> {
                            this.playerSectionTicketThrottlerSorter.enqueue(SectionTaskPriorityQueueSorter.createSorterMsg(() -> {
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
    @Inject(method = "tick", at = @At("RETURN"))
    protected void tickSection(CallbackInfo ci) {
        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.sectionTickets.long2ObjectEntrySet().fastIterator();
        while (objectiterator.hasNext()) {
            Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> entry = objectiterator.next();
            if (entry.getValue().removeIf((ticket) -> ((InvokeTicket) ticket).cc$isexpired(this.currentTime))) {
                this.sectionTicketTracker.updateSourceLevel(entry.getLongKey(), getLevel(entry.getValue()), false);
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
        this.playerSectionTracker.updateSourceLevel(i, 0, true);
        this.playerSectionTicketTracker.updateSourceLevel(i, 0, true);
    }

    @Override
    public void removePlayer(SectionPos sectionPosIn, ServerPlayerEntity player) {
        long i = sectionPosIn.asLong();
        ObjectSet<ServerPlayerEntity> objectset = this.playersBySectionPos.get(i);
        objectset.remove(player);
        if (objectset.isEmpty()) {
            this.playersBySectionPos.remove(i);
            this.playerSectionTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
            this.playerSectionTicketTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
        }
    }

    /**
     * Returns the number of chunks taken into account when calculating the mob cap
     */
    @Override
    public int getSpawningSectionsCount() {
        this.playerSectionTracker.processAllUpdates();
        return this.playerSectionTracker.sectionsInRange.size();
    }

    @Override
    public boolean isSectionOutsideSpawningRadius(long sectionPosIn) {
        this.playerSectionTracker.processAllUpdates();
        return this.playerSectionTracker.sectionsInRange.containsKey(sectionPosIn);
    }

    @Override
    public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getSectionTickets() {
        return sectionTickets;
    }

    @Override
    public ITaskExecutor<SectionTaskPriorityQueueSorter.FunctionEntry<Runnable>> getSectionPlayerTicketThrottler() {
        return playerSectionTicketThrottler;
    }

    @Override
    public ITaskExecutor<SectionTaskPriorityQueueSorter.RunnableEntry> getPlayerSectionTicketThrottlerSorter() { return playerSectionTicketThrottlerSorter; }

    @Override
    public LongSet getSectionPositions() {
        return sectionPositions;
    }

    @Override
    public Set<ChunkHolder> getSectionHolders()
    {
        return this.sectionHolders;
    }

    @Override
    public Executor executor() {
        return field_219388_p;
    }

    @Override
    public SectionTaskPriorityQueueSorter getSectionTaskPriorityQueueSorter() {
        return sectionTaskPriorityQueueSorter;
    }

    @Override
    public Long2ObjectMap<ObjectSet<ServerPlayerEntity>> getPlayersBySectionPos()
    {
        return this.playersBySectionPos;
    }
}