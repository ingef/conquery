package com.bakdata.conquery.models.preproc.parser.specific;

import java.util.Objects;

public class StringColumnValues extends ListColumnValues<String> {

	public void addString(String value) {
		Objects.requireNonNull(value, "value");

		prepareAdd(false);
		objects.add(value);
	}
}
