/*
 *  This file is part of CubicChunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.util.CompatUtil;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldSettings;
import io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.ServerChunkProvider;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends MixinWorld implements ICubicWorldInternal.Server {

    @Shadow public abstract DimensionSavedDataManager getSavedData();

    @Shadow public abstract ServerChunkProvider getChunkProvider();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(MinecraftServer server, Executor executor, SaveHandler saveHandler, WorldInfo worldInfo,
        DimensionType dimensionType, IProfiler profiler, IChunkStatusListener statusListener, CallbackInfo ci) {

        WorldSavedCubicChunksData savedData = this.getSavedData().get(WorldSavedCubicChunksData::new, "cubicChunksData");
        boolean ccWorldType = this.getWorldType() instanceof ICubicWorldType;
        boolean ccGenerator = ccWorldType && ((ICubicWorldType) this.getWorldType()).hasCubicGeneratorForWorld((World) (Object) this);
        boolean savedCC = savedData != null && savedData.isCubicChunks;
        boolean ccWorldInfo = ((ICubicWorldSettings) this.getWorldInfo()).isCubic() && (savedData == null || savedData.isCubicChunks);
        boolean excludeCC = false; //CubicChunksConfig.isDimensionExcluded(this.provider.getDimension());
        boolean forceExclusions = false; //CubicChunksConfig.forceDimensionExcludes;
        // TODO: simplify this mess of booleans and document where each of them comes from
        // these espressions are generated using Quine McCluskey algorithm
        // using the JQM v1.2.0 (Java QuineMcCluskey) program:
        // IS_CC := CC_GEN OR CC_TYPE AND NOT(EXCLUDED) OR SAVED_CC AND NOT(EXCLUDED) OR SAVED_CC AND NOT(F_EX) OR CC_NEW AND NOT(EXCLUDED);
        boolean isCC = ccGenerator
            || (ccWorldType && !excludeCC)
            || (savedCC && !excludeCC)
            || (savedCC && !forceExclusions)
            || (ccWorldInfo && !excludeCC);

        if ((CubicChunksConfig.SERVER.forceCCMode.get() == CubicChunksConfig.ForceCCMode.LOAD_NOT_EXCLUDED && !excludeCC)
            || CubicChunksConfig.SERVER.forceCCMode.get() == CubicChunksConfig.ForceCCMode.ALWAYS) {
            isCC = true;
        }

        if (savedData == null) {
            savedData = new WorldSavedCubicChunksData(isCC);
        }
        savedData.markDirty();
        this.getSavedData().set(savedData);
        this.getSavedData().save();

        if (!isCC) {
            return;
        }

        if (CompatUtil.shouldSkipWorld((World) (Object) this)) {
            LOGGER.info("Skipping world {} ({}) with type {} and chunk provider {} due to potential compatibility issues",
                this.getWorldInfo().getWorldName(), this, this.getWorldType().getName(), this.getChunkProvider());
            return;
        }

        IntRange generationRange = new IntRange(0, this.getActualHeight());
        WorldType type = this.getWorldType();
        if (type instanceof ICubicWorldType && ((ICubicWorldType) type).hasCubicGeneratorForWorld((World) (Object) this)) {
            generationRange = ((ICubicWorldType) type).calculateGenerationHeightRange((ServerWorld) (Object) this);
        }

        int minHeight = savedData.minHeight;
        int maxHeight = savedData.maxHeight;

        this.initCubicWorld(new IntRange(minHeight, maxHeight), generationRange);
    }

    @Override public void initCubicServerWorld() {
        LOGGER.info("  Applying server-specific cubic chunks world initialization for {} ({})", this.getWorldInfo().getWorldName(), this);
    }
}
