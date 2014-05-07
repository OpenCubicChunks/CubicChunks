package cuchaz.cubicChunks.lighting;

import java.util.List;

import cuchaz.cubicChunks.CubeProvider;

public interface LightCalculator
{
	int processBatch( List<Long> addresses, List<Long> deferredAddresses, CubeProvider cubeProvider );
}
