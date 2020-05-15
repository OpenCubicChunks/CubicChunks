package cubicchunks.cc.mixin;

import com.mojang.datafixers.TypeRewriteRule;
import net.minecraft.util.datafix.fixes.ChunkGenStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenStatus.class)
public class MixinChunkGenStatus {

    @Inject(at = @At("HEAD"), method = "makeRule")
    private void yeet(CallbackInfoReturnable<TypeRewriteRule> cir) {
    }
}
