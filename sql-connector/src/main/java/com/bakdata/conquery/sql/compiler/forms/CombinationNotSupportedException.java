package com.bakdata.conquery.sql.compiler.forms;

import com.bakdata.conquery.sql.model.form.FormCalendarUnit;
import com.bakdata.conquery.sql.model.form.FormIndexPlacement;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;

class CombinationNotSupportedException extends RuntimeException {

	public CombinationNotSupportedException(ResolutionAndAlignment resolutionAndAlignment) {
		super("Alignment %s does not fit the resolution %s.".formatted(
				resolutionAndAlignment.getAlignment(),
				resolutionAndAlignment.getResolution()
		));
	}

	public CombinationNotSupportedException(FormIndexPlacement indexPlacement, FormCalendarUnit timeUnit) {
		super("Combination of index placement %s and time unit %s not supported".formatted(indexPlacement, timeUnit));
	}

	public CombinationNotSupportedException(FormCalendarUnit timeUnit, FormResolution resolution) {
		super("Combination of time unit %s and resolution %s not supported".formatted(timeUnit, resolution));
	}

}
