package io.github.opencubicchunks.cubicchunks.config;

import io.github.opencubicchunks.cubicchunks.CubicChunks;

public class CommonConfig {

    public static final int MIN_RENDER_DISTANCE = 2;
    public static final int MAX_RENDER_DISTANCE = 32;

    public int verticalViewDistance;

    private final AbstractCommentedConfigHelper configHelper;

    public CommonConfig() {
        this.configHelper = new AbstractCommentedConfigHelper(CubicChunks.CONFIG_PATH.resolve(CubicChunks.MODID + "-common.toml"));
        verticalViewDistance = this.configHelper.addNumber("Cubic Chunks Vertical Render Distance.", "VerticalRenderDistance", 8, MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE); //TODO: Optifine check
        this.configHelper.build();
    }

    public void markDirty() {
        this.configHelper.updateValue("VerticalRenderDistance", verticalViewDistance);
        this.configHelper.build();
    }
}
