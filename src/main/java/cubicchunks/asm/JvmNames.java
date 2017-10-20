/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.asm;

@Deprecated // MinecraftDev plugin provides autocomplete for these, but it won't work for these fields, these should no longer be used
public class JvmNames {

    private JvmNames() {
    }

    // @formatter:off
    // We sort variables alphabetically here to make finding pre-existing constants easier
    // If you add a new one, please follow this sorting instead of adding them at the end
    public static final String
        BLOCK_FALLING = "Lnet/minecraft/block/BlockFalling;",
        BLOCK_POS = "Lnet/minecraft/util/math/BlockPos;",
        CHUNK = "Lnet/minecraft/world/chunk/Chunk;",
        CLASS_INHERITANCE_MULTI_MAP = "Lnet/minecraft/util/ClassInheritanceMultiMap;",
        COMMAND_BASE = "Lnet/minecraft/command/CommandBase;",
        COMMAND_TELEPORT = "Lnet/minecraft/command/server/CommandTeleport;",
        COMMAND_TP = "Lnet/minecraft/command/CommandTP;",
        ENTITY = "Lnet/minecraft/entity/Entity;",
        ENTITY_PLAYER_MP = "Lnet/minecraft/entity/player/EntityPlayerMP;",
        EXTENDED_BLOCK_STORAGE = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;",
        IBLOCK_ACCESS = "Lnet/minecraft/world/IBlockAccess;",
        IBLOCK_STATE = "Lnet/minecraft/block/state/IBlockState;",
        ICOMMAND_SENDER = "Lnet/minecraft/command/ICommandSender;",
        IMMUTABLE_SET_MULTIMAP = "Lcom/google/common/collect/ImmutableSetMultimap;",
        MATERIAL = "Lnet/minecraft/block/material/Material;",
        MINECRAFT_SERVER = "Lnet/minecraft/server/MinecraftServer;",
        MUTABLE_BLOCK_POS = "Lnet/minecraft/util/math/BlockPos$MutableBlockPos;",
        POOLED_MUTABLE_BLOCK_POS = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;",
        RENDER_CHUNK = "Lnet/minecraft/client/renderer/chunk/RenderChunk;",
        STRING = "Ljava/lang/String;",
        STRUCTURE_BOUNDING_BOX = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;",
        TESSELLATOR = "Lnet/minecraft/client/renderer/Tessellator;",
        WORLD = "Lnet/minecraft/world/World;",
        WORLD_CLIENT = "Lnet/minecraft/client/multiplayer/WorldClient;",
        WORLD_SERVER = "Lnet/minecraft/world/WorldServer;";

    // Sorted as above
    public static final String
        BLOCK_FALLING_CAN_FALL_THROUGH = BLOCK_FALLING + "canFallThrough("+IBLOCK_STATE+")Z",
        BLOCK_POS_ADD = BLOCK_POS + "add(III)" + BLOCK_POS,
        BLOCK_POS_CONSTR_ENTITY = "(" + ENTITY + ")" + BLOCK_POS,
        BLOCK_POS_DOWN = BLOCK_POS + "down()" + BLOCK_POS,
        BLOCK_POS_GETY = BLOCK_POS + "getY()I",
        CHUNK_CONSTRUCT_1 = "<init>(" + WORLD + "II" + ")V",
        CHUNK_GET_ENTITY_LISTS = CHUNK + "getEntityLists()[" + CLASS_INHERITANCE_MULTI_MAP,
        CHUNK_GET_TOP_FILLED_SEGMENT = CHUNK + "getTopFilledSegment()I",
        CHUNK_IS_CHUNK_LOADED = CHUNK + "isChunkLoaded:Z", // field
        CHUNK_IS_MODIFIED = CHUNK + "isModified:Z", // field
        CHUNK_IS_POPULATED = CHUNK + "isPopulated()Z",
        CHUNK_SET_CHUNK_MODIFIED = CHUNK + "setChunkModified()V",
        CHUNK_STORAGE_ARRAYS = CHUNK + "storageArrays:[" + EXTENDED_BLOCK_STORAGE,
        COMMAND_BASE_PARSE_DOUBLE = COMMAND_BASE + "parseDouble(D" + STRING + "IIZ)D",
        COMMAND_TELEPORT_GET_ENTITY = COMMAND_TELEPORT + "getEntity(" + MINECRAFT_SERVER + ICOMMAND_SENDER + STRING + ")" + ENTITY,
        COMMAND_TP_GET_COMMAND_SENDER_AS_PLAYER = COMMAND_TP + "getCommandSenderAsPlayer(" + ICOMMAND_SENDER + ")" + ENTITY_PLAYER_MP,
        COMMAND_TP_GET_ENTITY = COMMAND_TP + "getEntity(" + MINECRAFT_SERVER + ICOMMAND_SENDER + STRING + ")" + ENTITY,
        IBLOCK_ACCESS_GET_BLOCK_STATE = IBLOCK_ACCESS + "getBlockState(" + BLOCK_POS + ")" + IBLOCK_STATE,
        ICOMMAND_SENDER_GET_ENTITY_WORLD = ICOMMAND_SENDER + "getEntityWorld()" + WORLD,
        MATERIAL_WATER = MATERIAL + "WATER:" + MATERIAL,
        MUTABLE_BLOCK_POS_CONSTRUCT = MUTABLE_BLOCK_POS + "<init>(III)V",
        POOLED_MUTABLE_BLOCK_POS_SET_POS_D = POOLED_MUTABLE_BLOCK_POS + "setPos(DDD)" + POOLED_MUTABLE_BLOCK_POS,
        TESSELLATOR_DRAW = TESSELLATOR + "draw()V",
        WORLD_CLIENT_GET_CHUNK_FROM_BLOCK_COORDS = WORLD_CLIENT + "getChunkFromBlockCoords(" + BLOCK_POS + ")" + CHUNK,
        WORLD_GET_LIGHT_WITH_FLAG = WORLD + "getLight(" + BLOCK_POS + "Z)I",
        WORLD_GET_PERSISTENT_CHUNKS = WORLD + "getPersistentChunks()" + IMMUTABLE_SET_MULTIMAP,
        WORLD_IS_AIR_BLOCK = WORLD + "isAirBlock(" + BLOCK_POS + ")Z",
        WORLD_IS_AREA_LOADED = WORLD + "isAreaLoaded(IIIIIIZ)Z",
        WORLD_IS_BLOCK_LOADED = WORLD + "isBlockLoaded(" + BLOCK_POS + ")Z",
        WORLD_IS_BLOCK_LOADED_Z = WORLD + "isBlockLoaded(" + BLOCK_POS + "Z)Z",
        WORLD_IS_CHUNK_LOADED = WORLD + "isChunkLoaded(IIZ)Z",
        WORLD_SERVER_GET_ACTUAL_HEIGHT = WORLD_SERVER + "getActualHeight()I";

    // optifine methods
    public static final String
        OPTIFINE_RENDER_CHUNK_GET_CHUNK = RENDER_CHUNK + "getChunk(" + WORLD + ")" + CHUNK;
    // @formatter:on
}
