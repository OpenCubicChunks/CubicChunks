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

import com.google.common.base.Throwables;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.objectweb.asm.Type.*;

public class Mappings {
	private static boolean IS_DEV;
	//since srg field and method names are guarranted not to collide -  we can store them in one map
	private static final Map<String, String> srgToMcp = new HashMap<>();

	static {
		String location = System.getProperty("net.minecraftforge.gradle.GradleStart.srg.srg-mcp");
		IS_DEV = location != null;
		if(IS_DEV) {
			initMappings(location);
		}
	}

	//classes
	public static final String WORLD = "net/minecraft/world/World";
	public static final String WORLD_CLIENT = "net/minecraft/client/multiplayer/WorldClient";
	public static final String VIEW_FRUSTUM = "net/minecraft/client/renderer/ViewFrustum";
	public static final String RENDER_CHUNK = "net/minecraft/client/renderer/chunk/RenderChunk";
	public static final String BLOCK_POS = "net/minecraft/util/BlockPos";
	public static final String CHUNK_CACHE = "net/minecraft/world/ChunkCache";
	public static final String RENDER_GLOBAL = "net/minecraft/client/renderer/RenderGlobal";
	public static final String RG_CONTAINER_LOCAL_RENDER_INFORMATION = "net/minecraft/client/renderer/RenderGlobal$ContainerLocalRenderInformation";
	public static final String I_BLOCK_STATE = "net/minecraft/block/state/IBlockState";
	public static final String REGION_RENDER_CACHE = "net/minecraft/client/renderer/RegionRenderCache";
	public static final String CLASS_INHERITANCE_MULTI_MAP = "net/minecraft/util/ClassInheritanceMultiMap";
	public static final String ENTITY = "net/minecraft/entity/Entity";
	public static final String MINECRAFT = "net/minecraft/client/Minecraft";
	public static final String WORLD_SETTINGS = "net/minecraft/world/WorldSettings";
	public static final String INTEGRATED_SERVER = "net/minecraft/server/integrated/IntegratedServer";
	public static final String WORLD_TYPE = "net/minecraft/world/WorldType";
	public static final String ITEM_BLOCK = "net/minecraft/item/ItemBlock";
	public static final String MATH_HELPER = "net/minecraft/util/MathHelper";

	//methods
	public static final String WORLD_IS_VALID = getNameFromSrg("func_175701_a");
	public static final String WORLD_GET_LIGHT = getNameFromSrg("func_175699_k");
	public static final String WORLD_GET_LIGHT_CHECK = getNameFromSrg("func_175721_c");//the one with additional boolean argument
	public static final String WORLD_GET_LIGHT_FOR = getNameFromSrg("func_175642_b");
	public static final String WORLD_GET_LIGHT_FROM_NEIGHBORS_FOR = getNameFromSrg("func_175705_a");
	public static final String WORLD_IS_AREA_LOADED_IIIIIIZ = getNameFromSrg("func_175663_a");
	public static final String WORLD_UPDATE_ENTITY_WITH_OPTIONAL_FORCE = getNameFromSrg("func_72866_a");

	public static final String VIEW_FRUSTUM_SET_COUNT_CHUNKS = getNameFromSrg("func_178159_a");
	public static final String VIEW_FRUSTUM_GET_RENDER_CHUNK = getNameFromSrg("func_178161_a");
	public static final String VIEW_FRUSTUM_UPDATE_CHUNK_POSITIONS = getNameFromSrg("func_178163_a");

	public static final String CHUNK_CACHE_GET_BLOCK_STATE = getNameFromSrg("func_180495_p");
	public static final String CHUNK_CACHE_GET_LIGHT_FOR_EXT = getNameFromSrg("func_175629_a");
	public static final String CHUNK_CACHE_GET_LIGHT_FOR = getNameFromSrg("func_175628_b");

	public static final String RENDER_GLOBAL_GET_RENDER_CHUNK_OFFSET = getNameFromSrg("func_174973_a");
	public static final String RENDER_GLOBAL_RENDER_ENTITIES = getNameFromSrg("func_180446_a");

	public static final String REGION_RENDER_CACHE_GET_BLOCK_STATE_RAW = getNameFromSrg("func_175631_c");

	public static final String VEC_3_I_GET_X = getNameFromSrg("func_177958_n");

	public static final String ENTITY_ON_ENTITY_UPDATE = getNameFromSrg("func_70030_z");

	public static final String WORLD_SETTINGS_GET_TERRAIN_TYPE = getNameFromSrg("func_77165_h");

	public static final String ITEM_BLOCK_ON_ITEM_USE = getNameFromSrg("func_180614_a");

	public static final String MATH_HELPER_FLOOR_DOUBLE = getNameFromSrg("func_76128_c");

	//fields
	public static final String ENTITY_POS_Y = getNameFromSrg("field_70163_u");
	public static final String ENTITY_WORLD_OBJ = getNameFromSrg("field_70170_p");

	public static final String VIEW_FRUSTUM_WORLD = getNameFromSrg("field_178167_b");
	public static final String CHUNK_CACHE_WORLD_OBJ = getNameFromSrg("field_72815_e");
	public static final String RENDER_GLOBAL_THE_WORLD = getNameFromSrg("field_72769_h");
	public static final String RG_CLRI_RENDER_CHUNK = getNameFromSrg("field_178036_a");

	//classes referenced from asm
	public static final String WORLD_METHODS = "cubicchunks/asm/WorldMethods";
	public static final String WORLD_METHODS_GET_HEIGHT_DESC = getMethodDescriptor(INT_TYPE, getObjectType(WORLD));
	public static final String WORLD_METHODS_IS_TALL_WORLD_DESC =
			getMethodDescriptor(BOOLEAN_TYPE, getObjectType(WORLD));
	public static final String WORLD_METHODS_GET_MAX_HEIGHT_WORLD_TYPE_DESC =
			getMethodDescriptor(INT_TYPE, getObjectType(WORLD_TYPE));
	public static final String RENDER_METHODS = "cubicchunks/asm/RenderMethods";
	public static final String RENDER_METHODS_GET_RENDER_CHUNK_DESC =
			getMethodDescriptor(getObjectType(RENDER_CHUNK), getObjectType(VIEW_FRUSTUM), getObjectType(BLOCK_POS));
	public static final String RENDER_METHODS_UPDATE_CHUNK_POSITIONS_DESC =
			getMethodDescriptor(BOOLEAN_TYPE, getObjectType(VIEW_FRUSTUM));
	public static final String RENDER_METHODS_BLOCK_FROM_CACHE_DESC =
			getMethodDescriptor(getObjectType(I_BLOCK_STATE), getObjectType(REGION_RENDER_CACHE), getObjectType(BLOCK_POS));
	public static final String RENDER_METHODS_GET_ENTITY_LIST_DESC =
			getMethodDescriptor(getObjectType(CLASS_INHERITANCE_MULTI_MAP), getObjectType(RENDER_GLOBAL), getObjectType(RENDER_CHUNK));

	//other
	public static final String WORLD_FIELD_DESC = getObjectType(WORLD).getDescriptor();
	public static final String WORLD_CLIENT_FIELD_DESC = getObjectType(WORLD_CLIENT).getDescriptor();
	public static final String RENDER_CHUNK_FIELD_DESC = getObjectType(RENDER_CHUNK).getDescriptor();
	public static final String CONSTR_INTEGRATED_SERVER =
			getMethodDescriptor(VOID_TYPE, getObjectType(MINECRAFT), getType(String.class), getType(String.class), getObjectType(WORLD_SETTINGS));
	public static final String WORLD_SETTINGS_GET_TERRAIN_TYPE_DESC = getMethodDescriptor(getObjectType(WORLD_TYPE));
	public static final String WORLD_IS_AREA_LOADED_IIIIIIZ_DESC = getMethodDescriptor(BOOLEAN_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE);
	public static String getNameFromSrg(String srgName) {
		if(IS_DEV) {
			return srgToMcp.get(srgName);
		}
		return srgName;
	}

	private static void initMappings(String property) {
		try(Scanner scanner = new Scanner(new File(property))) {
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				parseLine(line);
			}
		} catch (FileNotFoundException e) {
			throw Throwables.propagate(e);
		}
	}

	private static void parseLine(String line) {
		if(line.startsWith("FD: ")) {
			parseField(line.substring("FD: ".length()));
		}
		if(line.startsWith("MD: ")) {
			parseMethod(line.substring("MD: ".length()));
		}
	}

	private static void parseMethod(String substring) {
		String[] s = substring.split(" ");

		final int SRG_NAME = 0, SRG_DESC = 1, MCP_NAME = 2, MCP_DESC = 3;

		int lastIndex = s[SRG_NAME].lastIndexOf('/') + 1;
		if(lastIndex < 0) lastIndex = 0;

		s[SRG_NAME] = s[SRG_NAME].substring(lastIndex);

		lastIndex = s[MCP_NAME].lastIndexOf("/") + 1;
		if(lastIndex < 0) lastIndex = 0;

		s[MCP_NAME] = s[MCP_NAME].substring(lastIndex);

		srgToMcp.put(s[SRG_NAME], s[MCP_NAME]);
	}

	private static void parseField(String str) {
		if(!str.contains(" ")) {
			return;
		}
		String[] s = str.split(" ");
		assert s.length == 2;

		int lastIndex = s[0].lastIndexOf('/') + 1;
		if(lastIndex < 0) lastIndex = 0;

		s[0] = s[0].substring(lastIndex);

		lastIndex = s[1].lastIndexOf("/") + 1;
		if(lastIndex < 0) lastIndex = 0;

		s[1] = s[1].substring(lastIndex);

		srgToMcp.put(s[0], s[1]);
	}
}
