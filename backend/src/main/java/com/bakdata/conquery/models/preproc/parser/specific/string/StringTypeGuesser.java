package com.bakdata.conquery.models.preproc.parser.specific.string;

import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.preproc.parser.specific.StringParser;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Abstract class to Guess the optimal {@link ColumnStore} class {@link StringParser}.
 */
public abstract class StringTypeGuesser {
	
	public abstract Guess createGuess();
	
	@AllArgsConstructor @Getter
	public class Guess implements Comparable<Guess> {

		public StringTypeGuesser getGuesser() {
			return StringTypeGuesser.this;
		}

		private final StringStore type;
		private final long memoryEstimate;
		private final long typeMemoryEstimate;
		
		public long estimate() {
			return memoryEstimate + typeMemoryEstimate;
		}

		@Override
		public int compareTo(Guess o) {
			return Long.compare(estimate(), o.estimate());
		}
	}
	
}