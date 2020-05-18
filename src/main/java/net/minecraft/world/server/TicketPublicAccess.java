package net.minecraft.world.server;

public class TicketPublicAccess {
    public static boolean isExpired(Ticket<?> ticket, long currentTime) {
        return ticket.isExpired(currentTime);
    }
}
