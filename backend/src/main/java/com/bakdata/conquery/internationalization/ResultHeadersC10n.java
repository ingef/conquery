package com.bakdata.conquery.internationalization;

import c10n.annotations.De;
import c10n.annotations.En;

public interface ResultHeadersC10n {
	
	@En("resolution")
	@De("beob_zeitraum")
	String resolution();

	@En("date_range")
	@De("datumsbereich")
	String dateRange();

	@En("dates")
	@De("datumswerte")
	String dates();
	
	@En("event_date")
	@De("indexdatum")
	String eventDate();
	
	@En("feature_date_range")
	@De("vorbetrachtung_datumsbereich")
	String featureDateRange();
	
	@En("outcome_date_range")
	@De("nachbetrachtung_datumsbereich")
	String outcomeDateRange();


}
