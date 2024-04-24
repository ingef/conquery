package com.bakdata.conquery.sql.conversion.forms;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;

class CombinationNotSupportedException extends RuntimeException {

	public CombinationNotSupportedException(ExportForm.ResolutionAndAlignment resolutionAndAlignment) {
		super("Alignment %s does not fit the resolution %s.".formatted(
				resolutionAndAlignment.getAlignment(),
				resolutionAndAlignment.getResolution()
		));
	}

}
