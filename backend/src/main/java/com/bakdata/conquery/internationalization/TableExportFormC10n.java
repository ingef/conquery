package com.bakdata.conquery.internationalization;

import c10n.annotations.De;
import c10n.annotations.En;

/**
 * Cosmopolitan (C10n) internationalization provider for the {@link com.bakdata.conquery.models.query.concept.TableExportForm} form.
 * Used to generate an expressive default name.
 */
public interface TableExportFormC10n {

	@En("Export Tables")
	@De("Tabellenexport")
	String getType();

}
