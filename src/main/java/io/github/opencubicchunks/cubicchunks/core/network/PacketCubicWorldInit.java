package io.github.opencubicchunks.cubicchunks.core.network;

import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCubicWorldInit {

    private boolean isCubicWorld;
    private int minHeight;
    private int maxHeight;
    private int minGenerationHeight;
    private int maxGenerationHeight;

    public PacketCubicWorldInit(ServerWorld world) {
        this.minHeight = 0;
        this.maxHeight = 256;
        if (((ICubicWorld) world).isCubicWorld()) {
            this.isCubicWorld = true;
            this.minHeight = ((ICubicWorld) world).getMinHeight();
            this.maxHeight = ((ICubicWorld) world).getMaxHeight();
            if (world.getWorldType() instanceof ICubicWorldType) {
                ICubicWorldType type = (ICubicWorldType) world.getWorldType();
                IntRange range = type.calculateGenerationHeightRange(world);
                this.minGenerationHeight = range.getMin();
                this.maxGenerationHeight = range.getMax();
            } else {
                this.minGenerationHeight = 0;
                this.maxGenerationHeight = 256;
            }
        }
    }

    PacketCubicWorldInit(PacketBuffer buf) {
        isCubicWorld = buf.readBoolean();
        minHeight = buf.readInt();
        maxHeight = buf.readInt();
        minGenerationHeight = buf.readInt();
        maxGenerationHeight = buf.readInt();
    }

    void encode(PacketBuffer buf) {
        buf.writeBoolean(this.isCubicWorld);
        buf.writeInt(this.minHeight);
        buf.writeInt(this.maxHeight);
        buf.writeInt(this.minGenerationHeight);
        buf.writeInt(this.maxGenerationHeight);
    }

    void handle(Supplier<NetworkEvent.Context> ctx) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        ClientWorld world = Minecraft.getInstance().getConnection().getWorld();
        // initialize only if sending packet about cubic world
        if (isCubicWorld) {
            ((ICubicWorldInternal.Client) world).initCubicWorld(
                new IntRange(minHeight, maxHeight),
                new IntRange(minGenerationHeight, maxGenerationHeight)
            );
            Minecraft.getInstance().worldRenderer.setDisplayListEntitiesDirty();
        }
    }
}
