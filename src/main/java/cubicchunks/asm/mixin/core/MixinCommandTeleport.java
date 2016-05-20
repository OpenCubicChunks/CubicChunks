package cubicchunks.asm.mixin.core;

import cubicchunks.world.ICubicWorld;
import net.minecraft.command.EntityNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.server.CommandTeleport;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.ref.WeakReference;

import static cubicchunks.asm.JvmNames.COMMAND_TELEPORT_GET_COMMAND_SENDER_AS_PLAYER;
import static cubicchunks.asm.JvmNames.COMMAND_TELEPORT_GET_ENTITY;
import static net.minecraft.command.CommandBase.getCommandSenderAsPlayer;
import static net.minecraft.command.CommandBase.getEntity;

@Mixin(CommandTeleport.class)
public class MixinCommandTeleport {
	private WeakReference<Entity> entity;

	@Inject(method = "execute",
	        at = @At(value = "INVOKE", target = COMMAND_TELEPORT_GET_ENTITY, ordinal = 0),
	        locals = LocalCapture.CAPTURE_FAILHARD,
	        require = 1)
	private void postGetEntityInject(MinecraftServer server, ICommandSender sender, String args[], CallbackInfo ci, int i) {
		try {
			entity = new WeakReference<>(getEntity(server, sender, args[0]));
		} catch (EntityNotFoundException e) {
		}
	}

	@Inject(method = "execute",
	        at = @At(value = "INVOKE", target = COMMAND_TELEPORT_GET_COMMAND_SENDER_AS_PLAYER, ordinal = 0),
	        locals = LocalCapture.CAPTURE_FAILHARD,
	        require = 1)
	private void postGetEntityPlayerInject(MinecraftServer server, ICommandSender sender, String args[], CallbackInfo ci, int i) {
		try {
			entity = new WeakReference<>(getCommandSenderAsPlayer(sender));
		} catch (PlayerNotFoundException e) {

		}
	}

	@ModifyConstant(method = "execute", constant = @Constant(intValue = -512), require = 1)
	private int getMinY(int orig) {
		if (entity == null || entity.get() == null) {
			return orig;
		}
		Entity entity = this.entity.get();
		return ((ICubicWorld) entity.getEntityWorld()).getMinHeight() + orig;
	}

	@ModifyConstant(method = "execute", constant = @Constant(intValue = 512), require = 1)
	private int getMaxY(int orig) {
		if (entity == null || entity.get() == null) {
			return orig;
		}
		Entity entity = this.entity.get();
		return ((ICubicWorld) entity.getEntityWorld()).getMaxHeight() + orig;
	}
}
