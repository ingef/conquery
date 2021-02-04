package com.bakdata.conquery.models.query.concept.specific;

/**
 * Marker interface for classes whose createExists may be forced from outside. Check {@linkplain com.bakdata.conquery.apiv1.forms.export_form.ExportForm} for usage.
 */
public interface ForcedExists {
	void setCreateExists(boolean value);
}
