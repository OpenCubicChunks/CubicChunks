package cubicchunks.cc.chunk.ticket;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.SectionDistanceGraph;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;

public abstract class CCTicketManager {
    private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();
    public class CubeTicketTracker extends SectionDistanceGraph
    {
        public CubeTicketTracker() {
            //TODO: change the arguments passed into super to CCCubeManager or CCColumnManager
            super(ChunkManager.MAX_LOADED_LEVEL + 2, 16, 256);
        }

        @Override
        protected int getSourceLevel(long pos) {
            SortedArraySet<Ticket<?>> sortedarrayset = CCTicketManager.this.tickets.get(pos);
            if (sortedarrayset == null) {
                return Integer.MAX_VALUE;
            } else {
                return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.getSmallest().getLevel();
            }
        }

        @Override
        protected int getLevel(long sectionPosIn) {
            return 0;
        }

        @Override
        protected void setLevel(long sectionPosIn, int level) {

        }
    }

    public class PlayerCubeTracker extends SectionDistanceGraph
    {

        public PlayerCubeTracker(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
            super(p_i50706_1_, p_i50706_2_, p_i50706_3_);
        }

        @Override
        protected int getSourceLevel(long pos) {
            return 0;
        }

        @Override
        protected int getLevel(long sectionPosIn) {
            return 0;
        }

        @Override
        protected void setLevel(long sectionPosIn, int level) {

        }
    }

    public class PlayerTicketTracker extends PlayerCubeTracker
    {

        public PlayerTicketTracker(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
            super(p_i50706_1_, p_i50706_2_, p_i50706_3_);
        }
    }

}
