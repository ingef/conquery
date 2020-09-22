package com.bakdata.conquery.models.types.parser.specific.string;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.specific.AStringType;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface TypeGuesser{
	
	Guess createGuess();
	
	@AllArgsConstructor @Getter
	static class Guess implements Comparable<Guess> {
		private final TypeGuesser guesser;
		private final AStringType<Number> type;
		private final Transformer<Integer, ?> transformer;
		private final long memoryEstimate;
		private final long typeMemoryEstimate;
		
		public long estimate() {
			int instances = ConqueryConfig.getInstance().getStandalone().getNumberOfShardNodes() + 1;
			return memoryEstimate + instances * typeMemoryEstimate;
		}

		@Override
		public int compareTo(Guess o) {
			return Long.compare(estimate(), o.estimate());
		}
	}
	
}