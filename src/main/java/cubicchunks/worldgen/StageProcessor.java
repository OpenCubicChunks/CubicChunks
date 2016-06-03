package cubicchunks.worldgen;

import cubicchunks.util.processor.QueueProcessor;

public class StageProcessor {

	public QueueProcessor<Long> processor;
	public float share;

	public StageProcessor(QueueProcessor<Long> processor) {
		this.processor = processor;
		this.share = 0f;
	}
}
