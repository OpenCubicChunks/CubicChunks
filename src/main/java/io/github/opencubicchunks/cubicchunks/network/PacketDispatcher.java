package io.github.opencubicchunks.cubicchunks.network;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PacketDispatcher {

    // TODO: network compatibility check on fabric?
    //private static SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
    //        new ResourceLocation("ocbc", "net"),
    //        () -> CubicChunks.PROTOCOL_VERSION,
    //        CubicChunks.PROTOCOL_VERSION::equals, CubicChunks.PROTOCOL_VERSION::equals);;

    private static final Map<Class<?>, BiConsumer<?, FriendlyByteBuf>> ENCODERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ResourceLocation> PACKET_IDS = new ConcurrentHashMap<>();

    public static void register() {
        registerMessage("cubes", PacketCubes.class, PacketCubes::encode,
            PacketCubes::new, mainThreadHandler(PacketCubes.Handler::handle));
        registerMessage("unload", PacketUnloadCube.class, PacketUnloadCube::encode,
            PacketUnloadCube::new, mainThreadHandler(PacketUnloadCube.Handler::handle));
        registerMessage("blocks", PacketCubeBlockChanges.class, PacketCubeBlockChanges::encode,
            PacketCubeBlockChanges::new, mainThreadHandler(PacketCubeBlockChanges.Handler::handle));
        registerMessage("cubepos", PacketUpdateCubePosition.class, PacketUpdateCubePosition::encode,
            PacketUpdateCubePosition::new, mainThreadHandler(PacketUpdateCubePosition.Handler::handle));
        registerMessage("cube_radius", PacketCubeCacheRadius.class, PacketCubeCacheRadius::encode,
            PacketCubeCacheRadius::new, mainThreadHandler(PacketCubeCacheRadius.Handler::handle));
        registerMessage("light", PacketUpdateLight.class, PacketUpdateLight::encode,
            PacketUpdateLight::new, mainThreadHandler(PacketUpdateLight.Handler::handle));
        registerMessage("heightmap", PacketHeightmap.class, PacketHeightmap::encode,
            PacketHeightmap::new, mainThreadHandler(PacketHeightmap.Handler::handle));
        registerMessage("heights", PacketHeightmapChanges.class, PacketHeightmapChanges::encode,
            PacketHeightmapChanges::new, mainThreadHandler(PacketHeightmapChanges.Handler::handle));
        //        CHANNEL.registerMessage("init", PacketCubicWorldInit.class, PacketCubicWorldInit::encode,
        //                PacketCubicWorldInit::new, mainThreadHandler(PacketCubicWorldInit::handle));
    }

    private static <T> void registerMessage(String id, Class<T> clazz,
                                            BiConsumer<T, FriendlyByteBuf> encode,
                                            Function<FriendlyByteBuf, T> decode,
                                            BiConsumer<T, PacketContext> handler) {
        ENCODERS.put(clazz, encode);
        PACKET_IDS.put(clazz, new ResourceLocation("ocbc", id));
        ClientSidePacketRegistry.INSTANCE.register(
            new ResourceLocation("ocbc", id), (ctx, received) -> {
                T packet = decode.apply(received);
                handler.accept(packet, ctx);
            }
        );
    }

    public static <MSG> void sendTo(MSG packet, ServerPlayer player) {
        ResourceLocation packetId = PACKET_IDS.get(packet.getClass());
        @SuppressWarnings("unchecked")
        BiConsumer<MSG, FriendlyByteBuf> encoder = (BiConsumer<MSG, FriendlyByteBuf>) ENCODERS.get(packet.getClass());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        encoder.accept(packet, buf);
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, packetId, buf);
    }

    public static <MSG> void sendTo(MSG packet, List<ServerPlayer> players) {
        ResourceLocation packetId = PACKET_IDS.get(packet.getClass());
        @SuppressWarnings("unchecked")
        BiConsumer<MSG, FriendlyByteBuf> encoder = (BiConsumer<MSG, FriendlyByteBuf>) ENCODERS.get(packet.getClass());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        encoder.accept(packet, buf);

        players.forEach(player -> {
            if (((CubicLevelHeightAccessor) player.getLevel()).isCubic())
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, packetId, buf);
        });
    }

    private static <T> BiConsumer<T, PacketContext> mainThreadHandler(Consumer<? super T> handler) {
        return (packet, ctx) -> ctx.getTaskQueue().submit(() -> handler.accept(packet));
    }

    private static <T> BiConsumer<T, PacketContext> mainThreadHandler(BiConsumer<? super T, ? super Level> handler) {
        return (packet, ctx) -> ctx.getTaskQueue().submit(() -> handler.accept(packet, Minecraft.getInstance().level));
    }
}