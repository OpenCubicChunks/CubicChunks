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

package cubicchunks.worldgen;

import cubicchunks.util.processor.CubeProcessor;
import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GeneratorPipeline implements Iterable<GeneratorStage> {

	private List<GeneratorStage> stages;

	private Map<String, GeneratorStage> stageMap;


	public static void checkStages(@Nonnull GeneratorPipeline pipeline) {
		for (GeneratorStage stage : pipeline.stages) {
			if (!stage.isLastStage()) {
				if (stage.getProcessor() == null) {
					throw new Error("Generator pipeline configured incorrectly! Stage " + stage.getName() +
							" is null! Fix your WorldServerContext constructor!");
				}
			}
		}

		// TODO: Move to WorldGenerator?
		pipeline.addStage(GeneratorStage.LIVE, null);
	}


	public GeneratorPipeline() {
		this.stages = new ArrayList<>();
		this.stageMap = new HashMap<>();
		this.stageMap.put(GeneratorStage.LIVE.getName(), GeneratorStage.LIVE);
	}

	public void addStage(@Nonnull GeneratorStage stage, @Nonnull CubeProcessor processor) {
		stage.setOrdinal(this.stages.size());
		stage.setProcessor(processor);
		this.stages.add(stage);
		this.stageMap.put(stage.getName(), stage);

		// Link the previous stage to its new successor.
		if (this.stages.size() > 1) {
			this.stages.get(this.stages.size() - 2).setNextStage(stage);
		}
	}

	@Nonnull
	public Iterator<GeneratorStage> iterator() {
		return this.stages.iterator();
	}

	public int stageCount() {
		// The stage LIVE does not count as a proper stage.
		return this.stages.size() - 1;
	}

	@Nullable
	public GeneratorStage getStage(@Nonnull String name) {
		return this.stageMap.get(name);
	}

	@Nonnull
	public GeneratorStage getFirstStage() {
		return this.stages.get(0);
	}

}
