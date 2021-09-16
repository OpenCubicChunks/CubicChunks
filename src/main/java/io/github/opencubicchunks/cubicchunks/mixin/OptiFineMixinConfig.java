package io.github.opencubicchunks.cubicchunks.mixin;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class OptiFineMixinConfig implements IMixinConfigPlugin {

    private boolean hasOptiFine;

    @Override public void onLoad(String mixinPackage) {
        //List<Map<String, String>> modlist = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get()).orElseThrow(Error::new);
        //for (Map<String, String> data : modlist) {
        //    String type = data.get("type");
        //    if (type == null || !type.equals("TRANSFORMATIONSERVICE")) {
        //        continue;
        //    }
        //    String name = data.get("name");
        //    if (name == null || !name.equals("OptiFine")) {
        //        continue;
        //    }
        //    hasOptiFine = true;
        //}
    }

    @Nullable
    @Override public String getRefMapperConfig() {
        return null;
    }

    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String pkg = mixinClassName.substring(0, mixinClassName.lastIndexOf("."));
        boolean isOptifineOnly = pkg.endsWith("optifine");
        boolean isVanillaOnly = pkg.endsWith("vanilla");
        return isOptifineOnly ? hasOptiFine : !isVanillaOnly || !hasOptiFine;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Nullable
    @Override public List<String> getMixins() {
        return null;
    }

    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}