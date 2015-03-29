package cubicchunks.world;

import cubicchunks.lighting.LightingManager;


public interface WorldContext {
	CubeCache getCubeCache();
	LightingManager getLightingManager();
}
