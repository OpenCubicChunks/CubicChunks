package cubicchunks.cc.network;

import cubicchunks.cc.CubicChunks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketDispatcher {

    private static SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CubicChunks.MODID, "net"),
            () -> CubicChunks.PROTOCOL_VERSION,
            CubicChunks.PROTOCOL_VERSION::equals, CubicChunks.PROTOCOL_VERSION::equals);;

    public static void register() {
        CHANNEL.registerMessage(0, PacketCubes.class, PacketCubes::encode,
                PacketCubes::new, mainThreadHandler(PacketCubes.Handler::handle),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, PacketUnloadCube.class, PacketUnloadCube::encode,
                PacketUnloadCube::new, mainThreadHandler(PacketUnloadCube.Handler::handle),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, PacketCubeBlockChanges.class, PacketCubeBlockChanges::encode,
                PacketCubeBlockChanges::new, mainThreadHandler(PacketCubeBlockChanges.Handler::handle),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(8, PacketUpdateCubePosition.class, PacketUpdateCubePosition::encode,
                PacketUpdateCubePosition::new, mainThreadHandler(PacketUpdateCubePosition.Handler::handle),
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        //        CHANNEL.registerMessage(5, PacketCubicWorldInit.class, PacketCubicWorldInit::encode,
        //                PacketCubicWorldInit::new, mainThreadHandler(PacketCubicWorldInit::handle));
    }

    public static <MSG> void sendTo(MSG packet, ServerPlayerEntity player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> mainThreadHandler(Consumer<? super T> handler) {
        return (packet, ctx) -> {
            ctx.get().enqueueWork(() -> handler.accept(packet));
            ctx.get().setPacketHandled(true);
        };
    }

    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> mainThreadHandler(BiConsumer<? super T, ? super World> handler) {
        return (packet, ctx) -> {
            ctx.get().enqueueWork(() -> handler.accept(packet, Minecraft.getInstance().world));
            ctx.get().setPacketHandled(true);
        };
    }
}