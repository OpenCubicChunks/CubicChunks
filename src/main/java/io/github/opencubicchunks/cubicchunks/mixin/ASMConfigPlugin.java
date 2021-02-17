package io.github.opencubicchunks.cubicchunks.mixin;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.mixin.transform.MainTransformer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class ASMConfigPlugin implements IMixinConfigPlugin {

    @Override public void onLoad(String mixinPackage) {

    }

    @Override public String getRefMapperConfig() {
        return null;
    }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Nullable
    @Override public List<String> getMixins() {
        return null;
    }

    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        MappingResolver map = FabricLoader.getInstance().getMappingResolver();
        String chunkMapDistanceManager = map.mapClassName("intermediary", "net.minecraft.class_3898$class_3216");
        String chunkMap = map.mapClassName("intermediary", "net.minecraft.class_3898");
        String chunkHolder = map.mapClassName("intermediary", "net.minecraft.class_3193");
        String naturalSpawner = map.mapClassName("intermediary", "net.minecraft.class_1948");

        if (targetClassName.equals(chunkMapDistanceManager)) {
            MainTransformer.transformProxyTicketManager(targetClass);
        } else if (targetClassName.equals(chunkMap)) {
            MainTransformer.transformChunkManager(targetClass);
        } else if (targetClassName.equals(chunkHolder)) {
            MainTransformer.transformChunkHolder(targetClass);
        } else if (targetClassName.equals(naturalSpawner)) {
            MainTransformer.transformNaturalSpawner(targetClass);
        }
    }

    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}