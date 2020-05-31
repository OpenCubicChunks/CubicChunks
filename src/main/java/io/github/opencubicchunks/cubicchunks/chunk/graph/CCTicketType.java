package io.github.opencubicchunks.cubicchunks.chunk.graph;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.server.TicketType;

import java.util.Comparator;

public class CCTicketType {
    public static final TicketType<CubePos> CCPLAYER = create("player", Comparator.comparingLong(CubePos::asLong));
    public static final TicketType<CubePos> CCFORCED = create("forced", Comparator.comparingLong(CubePos::asLong));
    public static final TicketType<CubePos> CCLIGHT = create("light", Comparator.comparingLong(CubePos::asLong));
    public static final TicketType<CubePos> CCUNKNOWN = create("unknown", Comparator.comparingLong(CubePos::asLong), 1);


    public static <T> TicketType<T> create(String nameIn, Comparator<T> comparator) {
        return new TicketType<>(nameIn, comparator, 0L);
    }

    public static <T> TicketType<T> create(String nameIn, Comparator<T> comparator, int lifespanIn) {
        return new TicketType<>(nameIn, comparator, lifespanIn);
    }
}
