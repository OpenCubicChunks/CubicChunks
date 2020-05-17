package cubicchunks.cc.mixin.core.common;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.TicketType;

import java.util.Comparator;

public class CCTicketType {
    public static final TicketType<SectionPos> CCPLAYER = create("player", Comparator.comparingLong(SectionPos::asLong));

    public static <T> TicketType<T> create(String nameIn, Comparator<T> comparator) {
        return new TicketType<>(nameIn, comparator, 0L);
    }
}
