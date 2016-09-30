package cubicchunks.asm.mixin.core.server;

import cubicchunks.server.chunkio.async.AsyncWorldIOExecutor;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cubicchunks.asm.JvmNames.CHUNK_IO_EXECUTOR_TICK;
import static cubicchunks.asm.JvmNames.MINECRAFT_SERVER_UPDATE_TIME_LIGHT_AND_ENTITIES;

/**
 * @author Malte Schütze
 */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer_AsyncCubeIO {

	/**
	 * Add callback to tick the async cube loader. The server may host both vanilla and cubic worlds, so we tick our
	 * chunk loader after the forge one
	 */
	@Inject(method=MINECRAFT_SERVER_UPDATE_TIME_LIGHT_AND_ENTITIES, at = @At(value = "INVOKE", target = CHUNK_IO_EXECUTOR_TICK, shift = At.Shift.AFTER))
	private void tickCubeLoader(CallbackInfo info) {
		AsyncWorldIOExecutor.tick();
	}
}
