package com.bakdata.conquery.sql.translate.form;

import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.forms.util.ResolutionAndAlignment;
import com.bakdata.conquery.sql.model.form.FormCalendarUnit;
import com.bakdata.conquery.sql.model.form.FormIndexPlacement;

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

	public CombinationNotSupportedException(FormCalendarUnit timeUnit, Resolution resolution) {
		super("Combination of time unit %s and resolution %s not supported".formatted(timeUnit, resolution));
	}

}
