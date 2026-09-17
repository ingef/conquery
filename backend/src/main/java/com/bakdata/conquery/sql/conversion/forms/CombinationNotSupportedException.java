package com.bakdata.conquery.sql.conversion.forms;

import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.Resolution;

class CombinationNotSupportedException extends RuntimeException {

	public CombinationNotSupportedException(ExportForm.ResolutionAndAlignment resolutionAndAlignment) {
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
