package cubicchunks.cc.chunk.ticket;

import net.minecraft.util.SectionDistanceGraph;

public class CCTicketManager {

    public static class CubeTicketManager extends SectionDistanceGraph
    {

        public CubeTicketManager(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
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

    public static class PlayerCubeTracker extends SectionDistanceGraph
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

    public static class PlayerTicketTracker extends PlayerCubeTracker
    {

        public PlayerTicketTracker(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
            super(p_i50706_1_, p_i50706_2_, p_i50706_3_);
        }
    }

}
