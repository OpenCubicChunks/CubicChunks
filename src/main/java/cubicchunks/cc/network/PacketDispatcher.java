package cubicchunks.cc.network;

import cubicchunks.cc.CubicChunks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketDispatcher {
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(
            new ResourceLocation("ocbc", "net"))
            .clientAcceptedVersions(CubicChunks.PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(CubicChunks.PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> CubicChunks.PROTOCOL_VERSION)
            .simpleChannel();

    public static void register() {
        CHANNEL.registerMessage(0, PacketCubes.class, PacketCubes::encode,
                PacketCubes::new, mainThreadHandler(PacketCubes::handle));
//        CHANNEL.registerMessage(5, PacketCubicWorldInit.class, PacketCubicWorldInit::encode,
//                PacketCubicWorldInit::new, mainThreadHandler(PacketCubicWorldInit::handle));
    }

    public static <MSG> void sendTo(MSG packet, ServerPlayerEntity player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> mainThreadHandler(Consumer<? super T> handler) {
        return (packet, ctx) -> ctx.get().enqueueWork(() -> handler.accept(packet));
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> mainThreadHandler(BiConsumer<? super T, ? super World> handler) {
        return (packet, ctx) -> ctx.get().enqueueWork(() -> handler.accept(packet, Minecraft.getInstance().world));
    }
}