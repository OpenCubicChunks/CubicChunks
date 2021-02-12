package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.storage;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.storage.ISectionStorage;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.storage.RegionCubeIO;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionStorage.class) //TODO: Consider vanilla dimensions
public abstract class MixinSectionStorage<R> implements ISectionStorage {


    @Shadow @Final private LongLinkedOpenHashSet dirty;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final protected LevelHeightAccessor levelHeightAccessor;

    @Shadow @Final private Long2ObjectMap<Optional<R>> storage;

    @Shadow @Final private Function<Runnable, Codec<R>> codec;

    @Shadow protected abstract void setDirty(long pos);

    @Shadow @Final private DataFixer fixerUpper;

    @Shadow protected static int getVersion(Dynamic<?> dynamic) {
        throw new Error("Mixin didn't apply");
    }

    @Shadow @Final private DataFixTypes type;

    @Shadow protected abstract void onSectionLoad(long pos);

    @Shadow @Nullable protected abstract Optional<R> get(long pos);

    @Shadow protected abstract boolean outsideStoredRange(SectionPos pos);


    RegionCubeIO cubeWorker;


    @Inject(method = "<init>", at = @At("RETURN"))
    private void getServerLevel(File file, Function<Runnable, Codec<R>> function, Function<Runnable, R> function2, DataFixer dataFixer, DataFixTypes dataFixTypes, boolean bl,
                                LevelHeightAccessor levelHeightAccessor, CallbackInfo ci) throws IOException {

        cubeWorker = new RegionCubeIO(file, file.getName() + "-chunk", file.getName());
    }

    /**
     * @author CorgiTaco
     */
    @Overwrite
    protected void tick(BooleanSupplier shouldKeepTicking) {
        while (!this.dirty.isEmpty() && shouldKeepTicking.getAsBoolean()) {
            CubePos cubePos = CubePos.from(SectionPos.of(this.dirty.firstLong()));
            this.writeCube(cubePos);
        }
    }

    /**
     * @author CorgiTaco
     */
    @Overwrite
    protected Optional<R> getOrLoad(long pos) {
        SectionPos sectionPos = SectionPos.of(pos);
        if (this.outsideStoredRange(sectionPos)) {
            return Optional.empty();
        } else {
            Optional<R> optional = this.get(pos);
            if (optional != null) {
                return optional;
            } else {
                this.readCube(CubePos.from(sectionPos));
                optional = this.get(pos);
                if (optional == null) {
                    throw Util.pauseInIde(new IllegalStateException());
                } else {
                    return optional;
                }
            }
        }
    }

    @Nullable
    private CompoundTag tryReadCube(CubePos pos) {
        try {
            return this.cubeWorker.loadCubeNBT(pos);
        } catch (IOException var3) {
            LOGGER.error("Error reading cube {} data from disk", pos, var3);
            return null;
        }
    }

    private void readCube(CubePos cubePos) {
        this.readCube(cubePos, NbtOps.INSTANCE, this.tryReadCube(cubePos));
    }

    private <T> void readCube(CubePos cubePos, DynamicOps<T> dynamicOps, @Nullable T data) {
        if (data == null) {
            for (int i = 0; i < IBigCube.SECTION_COUNT; ++i) {
                SectionPos sectionPos = Coords.sectionPosByIndex(cubePos, i);
                long sectionLong = sectionPos.asLong();
                this.storage.put(sectionLong, Optional.empty());
            }
        } else {
            Dynamic<T> dynamic = new Dynamic<>(dynamicOps, data);
            int j = getVersion(dynamic);
            int k = SharedConstants.getCurrentVersion().getWorldVersion();
            boolean bl = j != k;
            Dynamic<T> dynamic2 = this.fixerUpper.update(this.type.getType(), dynamic, j, k);
            OptionalDynamic<T> optionalDynamic = dynamic2.get("Sections");

            for (int l = 0; l < IBigCube.SECTION_COUNT; ++l) {
                SectionPos sectionPos = Coords.sectionPosByIndex(cubePos, l);
                long sectionLong = sectionPos.asLong();
                Optional<R> optional = optionalDynamic.get(Integer.toString(l)).result().flatMap((dynamicx) -> (this.codec.apply(() -> {
                    this.setDirty(sectionLong);
                })).parse(dynamicx).resultOrPartial(LOGGER::error));
                this.storage.put(sectionLong, optional);
                optional.ifPresent((object) -> {
                    this.onSectionLoad(sectionLong);
                    if (bl) {
                        this.setDirty(sectionLong);
                    }

                });
            }
        }

    }


    private void writeCube(CubePos pos) {
        Dynamic<Tag> dynamic = this.writeCube(pos, NbtOps.INSTANCE);
        Tag tag = dynamic.getValue();
        if (tag instanceof CompoundTag) {
            this.cubeWorker.saveCubeNBT(pos, (CompoundTag) tag);
        } else {
            LOGGER.error("Expected compound tag, got {}", tag);
        }
    }

    private <T> Dynamic<T> writeCube(CubePos cubePos, DynamicOps<T> dynamicOps) {
        Map<T, T> map = Maps.newHashMap();

        for (int i = 0; i < IBigCube.SECTION_COUNT; ++i) {
            SectionPos sectionPos = Coords.sectionPosByIndex(cubePos, i);
            long sectionLong = sectionPos.asLong();
            this.dirty.remove(sectionLong);
            Optional<R> optional = this.storage.get(sectionLong);
            if (optional != null && optional.isPresent()) {
                DataResult<T> dataResult = (this.codec.apply(() -> {
                    this.setDirty(sectionLong);
                })).encodeStart(dynamicOps, optional.get());
                String string = Integer.toString(i);
                dataResult.resultOrPartial(LOGGER::error).ifPresent((object) -> {
                    map.put(dynamicOps.createString(string), object);
                });
            }
        }

        return new Dynamic<>(dynamicOps, dynamicOps.createMap(
            ImmutableMap.of(dynamicOps.createString("Sections"), dynamicOps.createMap(map), dynamicOps.createString("DataVersion"),
                dynamicOps.createInt(SharedConstants.getCurrentVersion().getWorldVersion()))));
    }

    public void flush(CubePos cubePos) {
        if (!this.dirty.isEmpty()) {
            for (int i = 0; i < IBigCube.SECTION_COUNT; ++i) {
                SectionPos sectionPos = Coords.sectionPosByIndex(cubePos, i);
                long sectionLong = sectionPos.asLong();
                if (this.dirty.contains(sectionLong)) {
                    this.writeCube(cubePos);
                    return;
                }
            }
        }

    }
}
