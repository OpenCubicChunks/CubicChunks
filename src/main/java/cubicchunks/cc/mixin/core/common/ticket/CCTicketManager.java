package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.util.SectionDistanceGraph;

public class CCTicketManager {

    public class CubeTicketManager extends SectionDistanceGraph
    {

        protected CubeTicketManager(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
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

    public class PlayerCubeTracker extends SectionDistanceGraph
    {

        protected PlayerCubeTracker(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
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

        protected PlayerTicketTracker(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
            super(p_i50706_1_, p_i50706_2_, p_i50706_3_);
        }
    }

}
