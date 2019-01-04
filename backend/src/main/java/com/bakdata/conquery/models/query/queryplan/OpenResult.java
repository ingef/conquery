package com.bakdata.conquery.models.query.queryplan;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OpenResult {
	INCLUDED (true) {
		@Override
		public OpenResult and(OpenResult other) {
			return other;
		}

		@Override
		public OpenResult or(OpenResult other) {
			return INCLUDED;
		}
	},
	NOT_INCLUDED (false) {
		@Override
		public OpenResult and(OpenResult other) {
			return NOT_INCLUDED;
		}
		
		@Override
		public OpenResult or(OpenResult other) {
			return other;
		}
	},
	MAYBE (false) {
		@Override
		public OpenResult and(OpenResult other) {
			if(other == NOT_INCLUDED) {
				return OpenResult.NOT_INCLUDED;
			}
			else {
				return OpenResult.MAYBE;
			}
		}
		
		@Override
		public OpenResult or(OpenResult other) {
			if(other == INCLUDED) {
				return OpenResult.INCLUDED;
			}
			else {
				return OpenResult.MAYBE;
			}
		}
	};
	
	private final boolean definite;
	
	public boolean asDefiniteResult() {
		return definite;
	}

	public abstract OpenResult and(OpenResult aggregate);

	public abstract OpenResult or(OpenResult aggregate);
}
