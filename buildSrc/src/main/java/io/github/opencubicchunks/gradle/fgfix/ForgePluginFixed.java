package io.github.opencubicchunks.gradle.fgfix;

import net.minecraftforge.gradle.user.patcherUser.PatcherUserConstants;
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin;

public class ForgePluginFixed extends ForgePlugin {
    @Override protected Object getStartDir() {
        String version = (String) project.property("theMappingsVersion");
        return delayedFile(PatcherUserConstants.DIR_API_BASE + "/" + version + "/start");
    }
}
