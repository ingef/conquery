package com.bakdata.conquery.models.events.parser.specific.string;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.specific.string.StringType;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface TypeGuesser{
	
	Guess createGuess();
	
	@AllArgsConstructor @Getter
	static class Guess implements Comparable<Guess> {
		private final TypeGuesser guesser;
		private final StringType type;
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