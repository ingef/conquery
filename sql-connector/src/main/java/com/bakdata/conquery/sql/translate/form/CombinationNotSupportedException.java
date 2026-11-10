package com.bakdata.conquery.sql.translate.form;

import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.IndexPlacement;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.forms.util.ResolutionAndAlignment;

class CombinationNotSupportedException extends RuntimeException {

	public CombinationNotSupportedException(ResolutionAndAlignment resolutionAndAlignment) {
		super("Alignment %s does not fit the resolution %s.".formatted(
				resolutionAndAlignment.getAlignment(),
				resolutionAndAlignment.getResolution()
		));
	}

	public CombinationNotSupportedException(IndexPlacement indexPlacement, CalendarUnit timeUnit) {
		super("Combination of index placement %s and time unit %s not supported".formatted(indexPlacement, timeUnit));
	}

	public CombinationNotSupportedException(CalendarUnit timeUnit, Resolution resolution) {
		super("Combination of time unit %s and resolution %s not supported".formatted(timeUnit, resolution));
	}

}
