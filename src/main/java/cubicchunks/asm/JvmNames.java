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

public class JvmNames {
	private JvmNames() {}

	// @formatter:off
	public static final String
		BLOCK_POS = "Lnet/minecraft/util/math/BlockPos;",
		WORLD = "Lnet/minecraft/world/World;",
		WORLD_CLIENT = "Lnet/minecraft/client/multiplayer/WorldClient;",
		ENUM_SKY_BLOCK = "Lnet/minecraft/world/EnumSkyBlock;",
		CHUNK_CACHE = "Lnet/minecraft/world/ChunkCache;",
		IMMUTABLE_SET_MULTIMAP = "Lcom/google/common/collect/ImmutableSetMultimap;",
		IBLOCK_STATE = "Lnet/minecraft/block/state/IBlockState;",
		COMMAND_BASE = "Lnet/minecraft/command/CommandBase;",
		COMMAND_TELEPORT = "Lnet/minecraft/command/server/CommandTeleport;",
		COMMAND_TP = "Lnet/minecraft/command/CommandTP;",
		MINECRAFT_SERVER = "Lnet/minecraft/server/MinecraftServer;",
		ICOMMAND_SENDER = "Lnet/minecraft/command/ICommandSender;",
		STRING = "Ljava/lang/String;",
		ENTITY = "Lnet/minecraft/entity/Entity;",
		ENTITY_PLAYER_MP = "Lnet/minecraft/entity/player/EntityPlayerMP;",
		STRUCTURE_BOUNDING_BOX = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;",
		CHUNK = "Lnet/minecraft/world/chunk/Chunk;",
		CLASS_INHERITANCE_MULTI_MAP = "Lnet/minecraft/util/ClassInheritanceMultiMap;",
		DEDICATED_SERVER = "Lnet/minecraft/server/dedicated/DedicatedServer;",
		GUI_OVERLAY_DEBUG = "Lnet/minecraft/client/gui/GuiOverlayDebug;";

	public static final String
		BLOCK_POS_GETY = BLOCK_POS + "getY()I",
		WORLD_IS_AREA_LOADED = WORLD + "isAreaLoaded(IIIIIIZ)Z",
		WORLD_GET_PERSISTENT_CHUNKS = WORLD + "getPersistentChunks()" + IMMUTABLE_SET_MULTIMAP,
		WORLD_GET_LIGHT_WITH_FLAG = WORLD + "getLight(" + BLOCK_POS + "Z)I",
		WORLD_GET_LIGHT_FOR = WORLD + "getLightFor(" + ENUM_SKY_BLOCK + BLOCK_POS + ")I",
		CHUNK_CACHE_GET_BLOCK_STATE = CHUNK_CACHE + "getBlockState(" + BLOCK_POS + ")" + IBLOCK_STATE,
		COMMAND_TELEPORT_GET_ENTITY = COMMAND_TELEPORT + "getEntity(" + MINECRAFT_SERVER + ICOMMAND_SENDER + STRING + ")" + ENTITY,
		COMMAND_TP_GET_ENTITY = COMMAND_TP + "getEntity(" + MINECRAFT_SERVER + ICOMMAND_SENDER + STRING + ")" + ENTITY,
		COMMAND_TP_GET_COMMAND_SENDER_AS_PLAYER = COMMAND_TP + "getCommandSenderAsPlayer(" + ICOMMAND_SENDER + ")" + ENTITY_PLAYER_MP,
		COMMAND_BASE_PARSE_DOUBLE = COMMAND_BASE + "parseDouble(D" + STRING + "IIZ)D",
		WORLD_CLIENT_GET_CHUNK_FROM_BLOCK_COORDS = WORLD_CLIENT + "getChunkFromBlockCoords(" + BLOCK_POS + ")" + CHUNK,
		CHUNK_GET_ENTITY_LISTS = CHUNK + "getEntityLists()[" + CLASS_INHERITANCE_MULTI_MAP,
		DEDICATED_SERVER_IS_ANNOUNCING_PLAYER_ACHIEVEMENTS = DEDICATED_SERVER + "isAnnouncingPlayerAchievements()Z",
		GUI_OVERLAY_DEBUG_CALL = GUI_OVERLAY_DEBUG + "call()Ljava/util/List;";
	// @formatter:on
}
