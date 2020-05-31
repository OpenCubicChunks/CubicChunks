package io.github.opencubicchunks.cubicchunks.core.world.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
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
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkDistanceGraph;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkTaskPriorityQueueSorter;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class CubeTicketManager extends TicketManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PLAYER_TICKET_LEVEL = 33 + ChunkStatus.getDistance(ChunkStatus.FULL) - 2;
    private final Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();
    private final ChunkTicketTracker ticketTracker = new ChunkTicketTracker();
    private final PlayerChunkTracker playerChunkTracker = new PlayerChunkTracker(8);
    private final PlayerTicketTracker playerTicketTracker = new PlayerTicketTracker(33);
    private final Set<ChunkHolder> chunkHoldersToUpdate = Sets.newHashSet();
    private final ChunkTaskPriorityQueueSorter sorter;
    private final ITaskExecutor<ChunkTaskPriorityQueueSorter.FunctionEntry<Runnable>> field_219385_m;
    private final ITaskExecutor<ChunkTaskPriorityQueueSorter.RunnableEntry> field_219386_n;
    private final LongSet field_219387_o = new LongOpenHashSet();
    private final Executor mainThreadExecutor;
    private long currentTime;

    protected CubeTicketManager(Executor chunkExecutor, Executor mainThreadExecutor) {
        super(chunkExecutor, mainThreadExecutor);

        ITaskExecutor<Runnable> playerTicketThrottler = ITaskExecutor.inline("player ticket throttler", mainThreadExecutor::execute);
        ChunkTaskPriorityQueueSorter sorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(playerTicketThrottler), chunkExecutor, 4);
        this.sorter = sorter;
        this.field_219385_m = sorter.func_219087_a(playerTicketThrottler, true);
        this.field_219386_n = sorter.func_219091_a(playerTicketThrottler);
        this.mainThreadExecutor = mainThreadExecutor;
    }

    protected abstract boolean containsCube(long cubeAddress);

    @Nullable
    @Override
    protected ChunkHolder func_219335_b(long chunkPosIn) {
        return null;
    }

    @Nullable
    @Override
    protected ChunkHolder func_219372_a(long p_219372_1_, int p_219372_3_, @Nullable ChunkHolder p_219372_4_, int p_219372_5_) {
        return null;
    }



    @Override
    protected void tick() {
        ++this.currentTime;
        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> it = this.tickets.long2ObjectEntrySet().fastIterator();

        while(it.hasNext()) {
            Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> chunkTickets = it.next();
            if (chunkTickets.getValue().removeIf((ticket) -> ticket.isExpired(this.currentTime))) {
                this.ticketTracker.updateSourceLevel(chunkTickets.getLongKey(), getMinTicketLevel(chunkTickets.getValue()), false);
            }

            if (chunkTickets.getValue().isEmpty()) {
                it.remove();
            }
        }

    }

    private static int getMinTicketLevel(SortedArraySet<Ticket<?>> ticketArray) {
        return !ticketArray.isEmpty() ? ticketArray.func_226178_b_().getLevel() : ChunkManager.MAX_LOADED_LEVEL + 1;
    }

    @Override // containsColumn
    protected abstract boolean contains(long columnPos);

    @Nullable
    protected abstract ChunkHolder func_219335_b(long chunkPosIn);

    @Nullable
    protected abstract ChunkHolder func_219372_a(long p_219372_1_, int p_219372_3_, @Nullable ChunkHolder p_219372_4_, int p_219372_5_);

    @Override
    public boolean processUpdates(ChunkManager chunkManager) {
        this.playerChunkTracker.processUpdates();
        this.playerTicketTracker.processUpdates();
        int updated = Integer.MAX_VALUE - this.ticketTracker.processUpdatesNow(Integer.MAX_VALUE);
        boolean didUpdates = updated != 0;

        if (!this.chunkHoldersToUpdate.isEmpty()) {
            this.chunkHoldersToUpdate.forEach((chunkHolder) -> chunkHolder.func_219291_a(chunkManager));
            this.chunkHoldersToUpdate.clear();
            return true;
        } else {
            if (!this.field_219387_o.isEmpty()) {
                LongIterator longiterator = this.field_219387_o.iterator();

                while(longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.getTicketSet(j).stream().anyMatch((p_219369_0_) -> {
                        return p_219369_0_.getType() == TicketType.PLAYER;
                    })) {
                        ChunkHolder chunkholder = chunkManager.func_219220_a(j);
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> completablefuture = chunkholder.func_219297_b();
                        completablefuture.thenAccept((p_219363_3_) -> {
                            this.mainThreadExecutor.execute(() -> {
                                this.field_219386_n.enqueue(ChunkTaskPriorityQueueSorter.func_219073_a(() -> {
                                }, j, false));
                            });
                        });
                    }
                }

                this.field_219387_o.clear();
            }

            return didUpdates;
        }
    }

    private void register(long chunkPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTicketSet(chunkPosIn);
        int i = getMinTicketLevel(sortedarrayset);
        Ticket<?> ticket = sortedarrayset.func_226175_a_(ticketIn);
        ticket.setTimestamp(this.currentTime);
        if (ticketIn.getLevel() < i) {
            this.ticketTracker.updateSourceLevel(chunkPosIn, ticketIn.getLevel(), true);
        }

    }

    private void release(long chunkPosIn, Ticket<?> ticketIn) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.getTicketSet(chunkPosIn);
        if (sortedarrayset.remove(ticketIn)) {
            ;
        }

        if (sortedarrayset.isEmpty()) {
            this.tickets.remove(chunkPosIn);
        }

        this.ticketTracker.updateSourceLevel(chunkPosIn, getMinTicketLevel(sortedarrayset), false);
    }

    public <T> void registerWithLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        this.register(pos.asLong(), new Ticket<>(type, level, value));
    }

    public <T> void releaseWithLevel(TicketType<T> type, ChunkPos pos, int level, T value) {
        Ticket<T> ticket = new Ticket<>(type, level, value);
        this.release(pos.asLong(), ticket);
    }

    public <T> void register(TicketType<T> type, ChunkPos pos, int distance, T value) {
        this.register(pos.asLong(), new Ticket<>(type, 33 - distance, value));
    }

    public <T> void release(TicketType<T> type, ChunkPos pos, int distance, T value) {
        Ticket<T> ticket = new Ticket<>(type, 33 - distance, value);
        this.release(pos.asLong(), ticket);
    }

    private SortedArraySet<Ticket<?>> getTicketSet(long p_229848_1_) {
        return this.tickets.computeIfAbsent(p_229848_1_, (p_229851_0_) -> {
            return SortedArraySet.newSet(4);
        });
    }

    protected void forceChunk(ChunkPos pos, boolean add) {
        Ticket<ChunkPos> ticket = new Ticket<>(TicketType.FORCED, 31, pos);
        if (add) {
            this.register(pos.asLong(), ticket);
        } else {
            this.release(pos.asLong(), ticket);
        }

    }

    public void updatePlayerPosition(SectionPos sectionPosIn, ServerPlayerEntity player) {
        long i = sectionPosIn.asChunkPos().asLong();
        this.playersByChunkPos.computeIfAbsent(i, (p_219361_0_) -> {
            return new ObjectOpenHashSet<>();
        }).add(player);
        this.playerChunkTracker.updateSourceLevel(i, 0, true);
        this.playerTicketTracker.updateSourceLevel(i, 0, true);
    }

    public void removePlayer(SectionPos sectionPosIn, ServerPlayerEntity player) {
        long i = sectionPosIn.asChunkPos().asLong();
        ObjectSet<ServerPlayerEntity> objectset = this.playersByChunkPos.get(i);
        objectset.remove(player);
        if (objectset.isEmpty()) {
            this.playersByChunkPos.remove(i);
            this.playerChunkTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
            this.playerTicketTracker.updateSourceLevel(i, Integer.MAX_VALUE, false);
        }

    }

    protected String func_225413_c(long p_225413_1_) {
        SortedArraySet<Ticket<?>> sortedarrayset = this.tickets.get(p_225413_1_);
        String s;
        if (sortedarrayset != null && !sortedarrayset.isEmpty()) {
            s = sortedarrayset.func_226178_b_().toString();
        } else {
            s = "no_ticket";
        }

        return s;
    }

    protected void setViewDistance(int viewDistance) {
        this.playerTicketTracker.func_215508_a(viewDistance);
    }

    public int func_219358_b() {
        this.playerChunkTracker.processUpdates();
        return this.playerChunkTracker.field_215498_a.size();
    }

    public boolean func_223494_d(long p_223494_1_) {
        this.playerChunkTracker.processUpdates();
        return this.playerChunkTracker.field_215498_a.containsKey(p_223494_1_);
    }

    public String func_225412_c() {
        return this.sorter.func_225396_a();
    }

    class ChunkTicketTracker extends ChunkDistanceGraph {
        public ChunkTicketTracker() {
            super(ChunkManager.MAX_LOADED_LEVEL + 2, 16, 256);
        }

        protected int getSourceLevel(long pos) {
            SortedArraySet<Ticket<?>> sortedarrayset = CubeTicketManager.this.tickets.get(pos);
            if (sortedarrayset == null) {
                return Integer.MAX_VALUE;
            } else {
                return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.func_226178_b_().getLevel();
            }
        }

        protected int getLevel(long sectionPosIn) {
            if (!CubeTicketManager.this.contains(sectionPosIn)) {
                ChunkHolder chunkholder = CubeTicketManager.this.func_219335_b(sectionPosIn);
                if (chunkholder != null) {
                    return chunkholder.func_219299_i();
                }
            }

            return ChunkManager.MAX_LOADED_LEVEL + 1;
        }

        protected void setLevel(long sectionPosIn, int level) {
            ChunkHolder chunkholder = CubeTicketManager.this.func_219335_b(sectionPosIn);
            int i = chunkholder == null ? ChunkManager.MAX_LOADED_LEVEL + 1 : chunkholder.func_219299_i();
            if (i != level) {
                chunkholder = CubeTicketManager.this.func_219372_a(sectionPosIn, level, chunkholder, i);
                if (chunkholder != null) {
                    CubeTicketManager.this.chunkHoldersToUpdate.add(chunkholder);
                }

            }
        }

        public int processUpdatesNow(int count) {
            return this.processUpdates(count);
        }
    }

    class PlayerChunkTracker extends ChunkDistanceGraph {
        protected final Long2ByteMap field_215498_a = new Long2ByteOpenHashMap();
        protected final int field_215499_b;

        protected PlayerChunkTracker(int p_i50684_2_) {
            super(p_i50684_2_ + 2, 16, 256);
            this.field_215499_b = p_i50684_2_;
            this.field_215498_a.defaultReturnValue((byte)(p_i50684_2_ + 2));
        }

        protected int getLevel(long sectionPosIn) {
            return this.field_215498_a.get(sectionPosIn);
        }

        protected void setLevel(long sectionPosIn, int level) {
            byte b0;
            if (level > this.field_215499_b) {
                b0 = this.field_215498_a.remove(sectionPosIn);
            } else {
                b0 = this.field_215498_a.put(sectionPosIn, (byte)level);
            }

            this.func_215495_a(sectionPosIn, b0, level);
        }

        protected void func_215495_a(long p_215495_1_, int p_215495_3_, int p_215495_4_) {
        }

        protected int getSourceLevel(long pos) {
            return this.func_215496_d(pos) ? 0 : Integer.MAX_VALUE;
        }

        private boolean func_215496_d(long p_215496_1_) {
            ObjectSet<ServerPlayerEntity> objectset = CubeTicketManager.this.playersByChunkPos.get(p_215496_1_);
            return objectset != null && !objectset.isEmpty();
        }

        public void processUpdates() {
            this.processUpdates(Integer.MAX_VALUE);
        }
    }

    class PlayerTicketTracker extends PlayerChunkTracker {
        private int field_215512_e;
        private final Long2IntMap field_215513_f = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
        private final LongSet field_215514_g = new LongOpenHashSet();

        protected PlayerTicketTracker(int p_i50682_2_) {
            super(p_i50682_2_);
            this.field_215512_e = 0;
            this.field_215513_f.defaultReturnValue(p_i50682_2_ + 2);
        }

        protected void func_215495_a(long p_215495_1_, int p_215495_3_, int p_215495_4_) {
            this.field_215514_g.add(p_215495_1_);
        }

        public void func_215508_a(int p_215508_1_) {
            for(it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.field_215498_a.long2ByteEntrySet()) {
                byte b0 = entry.getByteValue();
                long i = entry.getLongKey();
                this.func_215504_a(i, b0, this.func_215505_c(b0), b0 <= p_215508_1_ - 2);
            }

            this.field_215512_e = p_215508_1_;
        }

        private void func_215504_a(long chunkPosIn, int p_215504_3_, boolean p_215504_4_, boolean p_215504_5_) {
            if (p_215504_4_ != p_215504_5_) {
                Ticket<?> ticket = new Ticket<>(TicketType.PLAYER, PLAYER_TICKET_LEVEL, new ChunkPos(chunkPosIn));
                if (p_215504_5_) {
                    CubeTicketManager.this.field_219385_m.enqueue(ChunkTaskPriorityQueueSorter.func_219069_a(() -> {
                        CubeTicketManager.this.mainThreadExecutor.execute(() -> {
                            if (this.func_215505_c(this.getLevel(chunkPosIn))) {
                                CubeTicketManager.this.register(chunkPosIn, ticket);
                                CubeTicketManager.this.field_219387_o.add(chunkPosIn);
                            } else {
                                CubeTicketManager.this.field_219386_n.enqueue(ChunkTaskPriorityQueueSorter.func_219073_a(() -> {
                                }, chunkPosIn, false));
                            }

                        });
                    }, chunkPosIn, () -> {
                        return p_215504_3_;
                    }));
                } else {
                    CubeTicketManager.this.field_219386_n.enqueue(ChunkTaskPriorityQueueSorter.func_219073_a(() -> {
                        CubeTicketManager.this.mainThreadExecutor.execute(() -> {
                            CubeTicketManager.this.release(chunkPosIn, ticket);
                        });
                    }, chunkPosIn, true));
                }
            }

        }

        public void processUpdates() {
            super.processUpdates();
            if (!this.field_215514_g.isEmpty()) {
                LongIterator longiterator = this.field_215514_g.iterator();

                while(longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    int j = this.field_215513_f.get(i);
                    int k = this.getLevel(i);
                    if (j != k) {
                        CubeTicketManager.this.sorter.func_219066_a(new ChunkPos(i), () -> {
                            return this.field_215513_f.get(i);
                        }, k, (p_215506_3_) -> {
                            if (p_215506_3_ >= this.field_215513_f.defaultReturnValue()) {
                                this.field_215513_f.remove(i);
                            } else {
                                this.field_215513_f.put(i, p_215506_3_);
                            }

                        });
                        this.func_215504_a(i, k, this.func_215505_c(j), this.func_215505_c(k));
                    }
                }

                this.field_215514_g.clear();
            }

        }

        private boolean func_215505_c(int p_215505_1_) {
            return p_215505_1_ <= this.field_215512_e - 2;
        }
    }
}
