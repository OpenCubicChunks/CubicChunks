package cubicchunks.asm.mixin.noncritical.client;

import cubicchunks.world.ICubicWorldClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import static cubicchunks.asm.JvmNames.GUI_OVERLAY_DEBUG_CALL;
import static cubicchunks.asm.JvmNames.BLOCK_POS_GETY;

/**
 * Goal of this mixin is to remove the "Outside of world..." message on the debug overlay for cubic world types.
 * We redirect the call to BlockPos.getY() for the bounds check in GuiOverlayDebug.call only
 * @author Malte Schütze
 */
@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {
	@Shadow private Minecraft mc;

	// As of Minecraft 1.10.2 / Forge 12.18.1.2092, the bounds checks are call 5 and 6 to getY()

	// This is call 5 (idx 4)
	@Redirect(method = GUI_OVERLAY_DEBUG_CALL, at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal = 4))
	private int blockPosGetYBoundsCheck1(BlockPos pos) {
		return blockPosGetYRedirect(pos);

	}
	// This is call 6 (idx 5)
	@Redirect(method = GUI_OVERLAY_DEBUG_CALL, at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal = 5))
	private int blockPosGetYBoundsCheck2(BlockPos pos) {
		return blockPosGetYRedirect(pos);

	}

	private int blockPosGetYRedirect(BlockPos pos) {// In bounds - don't modify anything
		if (pos.getY() >= 0 && pos.getY() < 256) return pos.getY();

		// Retain normal semantics for non-cubic worlds
		if (mc.theWorld instanceof ICubicWorldClient) {
			return MathHelper.clamp_int(pos.getY(), 0, 255);
		}

		return pos.getY();
	}
}
