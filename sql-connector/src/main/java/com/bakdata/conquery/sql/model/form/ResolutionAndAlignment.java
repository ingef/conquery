package com.bakdata.conquery.sql.model.form;

public record ResolutionAndAlignment(FormResolution resolution, FormAlignment alignment) {
	public FormResolution getResolution() { return resolution; }
	public FormAlignment getAlignment() { return alignment; }
}
