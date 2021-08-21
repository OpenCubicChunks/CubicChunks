package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.function.Function;

import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerTickList.class)
public interface ServerTickListAccess {
    @Invoker static <T> ListTag invokeSaveTickList(Function<T, ResourceLocation> identifierProvider, Iterable<TickNextTickData<T>> scheduledTicks, long time) {
        throw new Error("Mixin didn't apply");
    }
}
