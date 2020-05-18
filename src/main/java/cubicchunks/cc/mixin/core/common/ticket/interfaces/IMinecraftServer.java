package cubicchunks.cc.mixin.core.common.ticket.interfaces;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftServer.class)
public interface IMinecraftServer {
    @Invoker("getWorld")
    ServerWorld getServerWorld(DimensionType dimension);

    @Invoker("runScheduledTasks") void runSchedule();
}
