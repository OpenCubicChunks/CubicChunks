/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.lighting;

import java.util.List;

import cuchaz.cubicChunks.CubeProvider;

public interface LightCalculator
{
	int processBatch( List<Long> addresses, List<Long> deferredAddresses, CubeProvider cubeProvider );
}
