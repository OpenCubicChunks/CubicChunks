package io.github.opencubicchunks.cubicchunks.common;

import com.mojang.brigadier.CommandDispatcher;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.ICubicHeightmap;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;

public class CubicChunksCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("cc")

            // zone list
            .then(Commands.literal("dumpHeightmap")
                .executes(commandSource -> executeDumpHeightmap(commandSource.getSource()))
            )
        );
    }


    private static int executeDumpHeightmap(CommandSource source) {

        World world = source.getWorld();
        Entity sender = source.getEntity();
        BlockPos senderPos = sender.getPosition();

        IChunk chunk = world.getChunk(senderPos);
        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING);

        int chunkX = Coords.blockToLocal(senderPos.getX());
        int chunkZ = Coords.blockToLocal(senderPos.getZ());
        String dump = ((ICubicHeightmap) (Object) heightmap).dump(chunkX, chunkZ);

        for (String line : dump.split("\n")) {
            source.sendFeedback(new StringTextComponent(line), false);
        }

        return 0;
    }

}
