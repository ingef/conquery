package com.bakdata.conquery.internationalization;

import c10n.annotations.En;
/**
 * Cosmopolitan (C10n) internationalization provider for the export form.
 * Used to generate an expressive default name.
 */
public interface ExportFormC10n {
	
	@En("Export")
	String getType();

}
