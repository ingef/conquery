package com.bakdata.conquery.internationalization;

import c10n.annotations.De;
import c10n.annotations.En;

public interface ResultHeadersC10n {
	
	@En("resolution")
	@De("Zeiteinhait")
	String resolution();

	@En("date_range")
	@De("Zeitraum")
	String dateRange();

	@En("dates")
	@De("Datumswerte")
	String dates();
	
	@En("event_date")
	@De("Indexdatum")
	String eventDate();
	
	@En("feature_date_range")
	@De("Vorbeobachtungszeitraum")
	String featureDateRange();
	
	@En("outcome_date_range")
	@De("Nachbeobachtungszeitraum")
	String outcomeDateRange();


}
