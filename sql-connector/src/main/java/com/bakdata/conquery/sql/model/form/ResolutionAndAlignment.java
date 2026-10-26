package com.bakdata.conquery.sql.model.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ResolutionAndAlignment(
		@NotNull FormResolution resolution,
		@NotNull FormAlignment alignment
) {
	@AssertTrue(message = "alignment is not supported by resolution")
	public boolean isCompatible() {
		if (resolution == null || alignment == null) {
			return true;
		}
		return switch (resolution) {
			case COMPLETE -> alignment == FormAlignment.NO_ALIGN;
			case YEARS -> alignment == FormAlignment.YEAR
					|| alignment == FormAlignment.QUARTER
					|| alignment == FormAlignment.DAY;
			case QUARTERS -> alignment == FormAlignment.QUARTER || alignment == FormAlignment.DAY;
			case DAYS -> alignment == FormAlignment.DAY;
		};
	}
}
