package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.Map;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkSerializer.class)
public interface ChunkSerializerAccess {

    @Invoker
    static CompoundTag invokePackStructureData(ServerLevel world, ChunkPos chunkPos, Map<StructureFeature<?>, StructureStart<?>> map, Map<StructureFeature<?>, LongSet> map2) {
        throw new Error("Mixin did not apply.");
    }

    @Invoker
    static Map<StructureFeature<?>, StructureStart<?>> invokeUnpackStructureStart(ServerLevel serverLevel, CompoundTag nbt, long worldSeed) {
        throw new Error("Mixin did not apply.");
    }

    @Invoker
    static Map<StructureFeature<?>, LongSet> invokeUnpackStructureReferences(ChunkPos pos, CompoundTag nbt) {
        throw new Error("Mixin did not apply.");
    }
}
