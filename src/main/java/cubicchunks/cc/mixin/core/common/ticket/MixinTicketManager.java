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

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(TicketManager.class)
public abstract class MixinTicketManager implements ITicketManager {
    @Final @Shadow private Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos;
    @Final @Shadow private Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets;
    @Final @Shadow private LongSet field_219387_o;
    @Final @Shadow private Executor field_219388_p;
    @Shadow private long currentTime;
    @Final @Shadow private Set<ChunkHolder> chunkHolders;
    //Abstracts
    @Shadow protected abstract ChunkHolder func_219335_b(long chunkPos);

    @Shadow protected abstract boolean contains(long p_219371_1_);

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

    private static int getLevel(SortedArraySet<Ticket<?>> ticketSet) {
        return !ticketSet.isEmpty() ? ticketSet.getSmallest().getLevel() : ChunkManager.MAX_LOADED_LEVEL + 1;
    }

    private void register(long sectionPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTicketSet(sectionPosIn);
        int i = getLevel(sortedarrayset);
        Ticket<?> ticket = sortedarrayset.func_226175_a_(ticketIn);
        ((InvokeTicket) ticket).setTimestampCC(this.currentTime);
        if (ticketIn.getLevel() < i) {
            this.cc$ticketTracker.updateSourceLevel(sectionPosIn, ticketIn.getLevel(), true);
        }
    }
    public void cc$register(long cubePos, Ticket<?> ticket)
    {
        this.register(cubePos, ticket);
    }

    private void release(long sectionPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTicketSet(sectionPosIn);
        sortedarrayset.remove(ticketIn);

        if (sortedarrayset.isEmpty()) {
            this.tickets.remove(sectionPosIn);
        }

        this.cc$ticketTracker.updateSourceLevel(sectionPosIn, getLevel(sortedarrayset), false);
    }
    public void cc$release(long cubePos, Ticket<?> ticket)
    {
        this.release(cubePos, ticket);
    }

    private SortedArraySet<Ticket<?>> getTicketSet(long p_229848_1_) {
        return this.tickets.computeIfAbsent(p_229848_1_, (p_229851_0_) -> SortedArraySet.newSet(4));
    }

    protected void forceCube(SectionPos pos, boolean add) {
        Ticket<SectionPos> ticket = new Ticket<>(CCTicketType.CCFORCED, 31, pos);
        if (add) {
            this.register(pos.asLong(), ticket);
        } else {
            this.release(pos.asLong(), ticket);
        }

    }

    protected String func_225413_c(long p_225413_1_) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.tickets.get(p_225413_1_);
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

    //BEGIN OVERWRITE

    /**
     * @author NotStirred
     * @reason to overwrite the vanilla processUpdates
     */
    @Overwrite
    public boolean processUpdates(ChunkManager chunkManager) {
        this.cc$playerCubeTracker.processAllUpdates();
        this.cc$playerTicketTracker.processAllUpdates();
        int i = Integer.MAX_VALUE - this.cc$ticketTracker.update(Integer.MAX_VALUE);
        boolean flag = i != 0;

        if (!this.chunkHolders.isEmpty()) {
            this.chunkHolders.forEach((chunkHolder) -> ((InvokeChunkHolder) chunkHolder).processUpdatesCC(chunkManager));
            this.chunkHolders.clear();
            return true;
        } else {
            if (!this.field_219387_o.isEmpty()) {
                LongIterator longiterator = this.field_219387_o.iterator();

                while (longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.getTicketSet(j).stream().anyMatch((p_219369_0_) -> p_219369_0_.getType() == CCTicketType.CCPLAYER)) {
                        ChunkHolder chunkholder = ((InvokeChunkManager) chunkManager).chunkHold(j);
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

                this.field_219387_o.clear();
            }

            return flag;
        }
    }

    //TODO: check if there is another way to do this
    /**
     * @author NotStirred
     * @reason idk & cba
     */
    @Overwrite
    protected void tick() {
        ++this.currentTime;
        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();

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
    public ChunkHolder cc$getChunkHolder(long chunkPos)
    {
        return this.func_219335_b(chunkPos);
    }

    @Override
    public boolean cc$contains(long variable) {
        return this.contains(variable);
    }

    @Override
    public <T> void registerWithLevel(TicketType<T> type, SectionPos pos, int level, T value) {
        this.register(pos.asLong(), new Ticket<>(type, level, value));
    }

    @Override
    public <T> void releaseWithLevel(TicketType<T> type, SectionPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value);
        this.release(pos.asLong(), ticket);
    }

    @Override
    public <T> void register(TicketType<T> type, SectionPos pos, int distance, T value) {
        this.register(pos.asLong(), new Ticket<>(type, 33 - distance, value));
    }

    @Override
    public <T> void release(TicketType<T> type, SectionPos pos, int distance, T value) {
        Ticket<T> ticket = new Ticket<>(type, 33 - distance, value);
        this.release(pos.asLong(), ticket);
    }

    @Override
    public void updatePlayerPosition(SectionPos sectionPosIn, ServerPlayerEntity player) {
        long i = sectionPosIn.asChunkPos().asLong();
        this.playersByChunkPos.computeIfAbsent(i, (p_219361_0_) -> new ObjectOpenHashSet<>()).add(player);
        this.cc$playerCubeTracker.updateSourceLevel(i, 0, true);
        this.cc$playerTicketTracker.updateSourceLevel(i, 0, true);
    }

    @Override
    public void removePlayer(SectionPos sectionPosIn, ServerPlayerEntity player) {
        long i = sectionPosIn.asChunkPos().asLong();
        ObjectSet<ServerPlayerEntity> objectset = this.playersByChunkPos.get(i);
        objectset.remove(player);
        if (objectset.isEmpty()) {
            this.playersByChunkPos.remove(i);
            this.cc$playerCubeTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
            this.cc$playerTicketTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
        }
    }

    /**
     * Returns the number of chunks taken into account when calculating the mob cap
     */
    @Override
    public int getSpawningChunksCount() {
        this.cc$playerCubeTracker.processAllUpdates();
        return this.cc$playerCubeTracker.cubesInRange.size();
    }

    @Override
    public boolean isOutsideSpawningRadius(long sectionPosIn) {
        this.cc$playerCubeTracker.processAllUpdates();
        return this.cc$playerCubeTracker.cubesInRange.containsKey(sectionPosIn);
    }

    @Override
    public String func_225412_c() {
        return this.cc$levelUpdateListener.debugString();
    }

    @Override
    public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getTickets() {
        return tickets;
    }

    @Override
    public ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> getCc$playerTicketThrottler() {
        return cc$playerTicketThrottler;
    }

    @Override
    public ITaskExecutor<CubeTaskPriorityQueueSorter.RunnableEntry> getplayerTicketThrottlerSorter() {
        return cc$playerTicketThrottlerSorter;
    }

    @Override
    public LongSet getChunkPositions() {
        return field_219387_o;
    }

    @Override
    public Set<ChunkHolder> getChunkHolders()
    {
        return this.chunkHolders;
    }

    @Override
    public Executor executor() {
        return field_219388_p;
    }

    @Override
    public CubeTaskPriorityQueueSorter getlevelUpdateListener() {
        return cc$levelUpdateListener;
    }
}