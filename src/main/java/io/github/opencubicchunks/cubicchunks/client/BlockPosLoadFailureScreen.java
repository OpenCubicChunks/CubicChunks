package io.github.opencubicchunks.cubicchunks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.BlockPosAccess;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

public class BlockPosLoadFailureScreen extends Screen {
    private MultiLineLabel message;

    public BlockPosLoadFailureScreen(int currentServerXZPacked) {
        super(new TranslatableComponent("blockposlongfailure.title", (1 << currentServerXZPacked), (1 << BlockPosAccess.getPackedXLength())));
        this.message = MultiLineLabel.EMPTY;
    }

    protected void init() {
        super.init();
        this.message = MultiLineLabel.create(this.font, this.getTitle(), this.width / 3);
        this.addRenderableWidget(new Button(this.width / 3, this.height / 2, this.width / 3, 20, new TranslatableComponent("gui.toTitle"), (button) -> {
            this.minecraft.setScreen(null);
        }));
    }

    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.message.renderCentered(matrices, this.width / 2, this.height / 4);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }
}