package com.bakdata.conquery.mode;

public sealed interface ValidationMode permits ValidationMode.Local, ValidationMode.Clustered {
	non-sealed interface Local extends ValidationMode {
	}

	non-sealed interface Clustered extends ValidationMode {
	}

}
