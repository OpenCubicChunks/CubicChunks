package cubicchunks.cc.mixin.core.common.chunk;

import cubicchunks.cc.chunk.ICubeGenerator;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator implements ICubeGenerator {

    @Inject(method = "generateStructures", at = @At("HEAD"), cancellable = true)
    public void onGenerateStructures(BiomeManager p_227058_1_, IChunk p_227058_2_, ChunkGenerator<?> p_227058_3_, TemplateManager p_227058_4_,
            CallbackInfo ci) {

        ci.cancel();
    }


    @Inject(method = "generateStructureStarts", at = @At("HEAD"), cancellable = true)
    public void generateStructureStarts(IWorld worldIn, IChunk chunkIn, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "generateBiomes", at = @At("HEAD"), cancellable = true)
    public void generateBiomes(IChunk chunkIn, CallbackInfo ci) {
        ci.cancel();
    }

}
