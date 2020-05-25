package cubicchunks.cc.mixin.core.common.chunk;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.palette.UpgradeData;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ChunkSerializer.class)
public class MixinChunkSerializer {

    private static CompoundNBT compoundNBT;

    @ModifyConstant(method = "read", constant = @Constant(intValue = 16),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=Sections"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;getDimension()Lnet/minecraft/world/dimension/Dimension;")
            ))
    private static int getSectionCount(int _16) {
        return 32;
    }

    @Inject(locals = LocalCapture.CAPTURE_FAILHARD, method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;getByte(Ljava/lang/String;)B"))
    private static void captureTagData(ServerWorld worldIn, TemplateManager templateManagerIn, PointOfInterestManager poiManager, ChunkPos pos,
                                       CompoundNBT compound, CallbackInfoReturnable<ChunkPrimer> cir, ChunkGenerator chunkgenerator,
                                       BiomeProvider biomeprovider, CompoundNBT compoundnbt, ChunkPos chunkpos, BiomeContainer biomecontainer,
                                       UpgradeData upgradedata, ChunkPrimerTickList chunkprimerticklist, ChunkPrimerTickList chunkprimerticklist1,
                                       boolean flag, ListNBT listnbt, int i, ChunkSection achunksection[], boolean flag1,
                                       AbstractChunkProvider abstractchunkprovider, WorldLightManager worldlightmanager, int j,
                                       CompoundNBT compoundnbt1) {
        MixinChunkSerializer.compoundNBT = compoundnbt1;
    }

    @ModifyVariable(method = "read", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/nbt/CompoundNBT;getByte(Ljava/lang/String;)B"), ordinal = 2)
    private static int getY(int previousValue)
    {
        return compoundNBT.getInt("Y");
    }

    @ModifyConstant(method = "read", constant = @Constant(intValue = 255))
    private static int getMaxY(int _255) {
        return 512;
    }

    @ModifyConstant(method = "write", constant = @Constant(intValue = 17))
    private static int getMaxSectionY(int _17) {
        return 33;
    }

    @Inject(locals = LocalCapture.CAPTURE_FAILHARD, method = "write", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/nbt/CompoundNBT;putByte(Ljava/lang/String;B)V"))
    private static void afterPutByte(ServerWorld worldIn, IChunk chunkIn, CallbackInfoReturnable<CompoundNBT> cir, ChunkPos chunkpos, CompoundNBT compoundnbt, CompoundNBT compoundnbt1, UpgradeData upgradedata, ChunkSection achunksection[], ListNBT listnbt, WorldLightManager worldlightmanager, boolean flag, int i, int j, ChunkSection chunksection, NibbleArray nibblearray, NibbleArray nibblearray1, CompoundNBT compoundnbt2) {
        compoundnbt2.remove("Y");
        compoundnbt2.putInt("Y", j);
    }
}
