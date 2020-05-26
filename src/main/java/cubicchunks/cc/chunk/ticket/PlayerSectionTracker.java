package cubicchunks.cc.chunk.ticket;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SectionDistanceGraph;

public class PlayerSectionTracker extends SectionDistanceGraph {
    public final Long2ByteMap sectionsInRange = new Long2ByteOpenHashMap();
    protected final int range;

    private final ITicketManager iTicketManager;

    public PlayerSectionTracker(ITicketManager iTicketManager, int i) {
        super(i + 2, 16, 256);
        this.range = i;
        this.sectionsInRange.defaultReturnValue((byte) (i + 2));
        this.iTicketManager = iTicketManager;
    }

    protected int getLevel(long sectionPosIn) {
        return this.sectionsInRange.get(sectionPosIn);
    }

    protected void setLevel(long sectionPosIn, int level) {
        byte b0;
        if (level > this.range) {
            b0 = this.sectionsInRange.remove(sectionPosIn);
        } else {
            b0 = this.sectionsInRange.put(sectionPosIn, (byte) level);
        }

        this.chunkLevelChanged(sectionPosIn, b0, level);
    }

    protected void chunkLevelChanged(long sectionPosIn, int oldLevel, int newLevel) {
    }

    protected int getSourceLevel(long pos) {
        return this.hasPlayerInChunk(pos) ? 0 : Integer.MAX_VALUE;
    }

    private boolean hasPlayerInChunk(long sectionPosIn) {
        ObjectSet<ServerPlayerEntity> objectset = iTicketManager.getPlayersBySectionPos().get(sectionPosIn);
        return objectset != null && !objectset.isEmpty();
    }

    public void processAllUpdates() {
        this.processUpdates(Integer.MAX_VALUE);
    }
}