package com.bakdata.conquery.models.query.statistics;

import c10n.annotations.De;
import c10n.annotations.En;

public interface StatisticsLabels {
	@En("Minimum")
	@De("Minimum")
	public String min();

	@En("Maximum")
	@De("Maximum")
	public String max();

	@En("Mean")
	@De("Mittelwert")
	public String mean();

	@En("Median")
	@De("Median")
	public String median();

	@En("Sum")
	@De("Summe")
	public String sum();

	@En("Standard Deviation")
	@De("Standartabweichung")
	public String std();

	@En("3rd Quartile")
	@De("3. Quartil")
	public String p75();

	@En("1st Quartile")
	@De("1. Quartil")
	public String p25();

	@En("Total")
	@De("Anzahl")
	public String count();

	@En("Missing")
	@De("Fehlende Einträge")
	public String missing();

	@En("{0} additional Values")
	@De("{0} weitere Werte")
	public String remainingValues(long count);

	@En("{0} entries")
	@De("{0} Einträge")
	public String remainingEntries(long count);


}
