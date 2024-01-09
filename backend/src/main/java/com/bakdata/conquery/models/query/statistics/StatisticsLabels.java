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
	@De("Standard Abweichung")
	public String std();

	@En("75th percentile")
	@De("75te Percentile")
	public String p75();

	@En("25th percentile")
	@De("25te Percentile")
	public String p25();

	@En("Total")
	public String count();

	@En("Missing")
	@De("Fehlend")
	public String missing();

	@En("{} additional Values")
	@De("{} zusätzliche Werte")
	public String remainingNodes(int count);

}
