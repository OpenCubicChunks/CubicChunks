package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.CubicChunksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.VideoSettingsScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.minecraft.client.settings.SliderPercentageOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VideoSettingsScreen.class)
public class MixinVideoSettingsScreen {

    private static final AbstractOption[] MODIFIED_OPTIONS;

    private static final SliderPercentageOption VERTICAL_RENDER_DISTANCE = new SliderPercentageOption("options.renderDistance", 2.0D, 16.0D, 1.0F, (gameSettings) -> (double) CubicChunksConfig.verticalViewDistance.get(),
            (gameSettings, value) -> {
                CubicChunksConfig.verticalViewDistance.set(value.intValue());
                Minecraft.getInstance().worldRenderer.setDisplayListEntitiesDirty();
            }, (gameSettings, self) -> {
        double value = self.get(gameSettings);
        // TODO: Translations for menu option
        return "Vertical Cubes: " + (int) value; //self.getDisplayString() + I18n.format("options.chunks", (int) value);
    }
    );



    @Redirect(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/VideoSettingsScreen;OPTIONS:[Lnet/minecraft/client/settings/AbstractOption;"))
    private AbstractOption[] getOptionsMixin() {
        return MODIFIED_OPTIONS;
    }

    static {
        MODIFIED_OPTIONS = new AbstractOption[]{AbstractOption.GRAPHICS, AbstractOption.RENDER_DISTANCE, AbstractOption.AO, VERTICAL_RENDER_DISTANCE, AbstractOption.FRAMERATE_LIMIT, AbstractOption.VSYNC, AbstractOption.VIEW_BOBBING, AbstractOption.GUI_SCALE, AbstractOption.ATTACK_INDICATOR, AbstractOption.GAMMA, AbstractOption.RENDER_CLOUDS, AbstractOption.FULLSCREEN, AbstractOption.PARTICLES, AbstractOption.MIPMAP_LEVELS, AbstractOption.ENTITY_SHADOWS};
    }
}
