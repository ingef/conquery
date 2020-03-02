package com.bakdata.conquery.models.config;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConqueryMetricsConfig {
	private int userActiveHours = 1;
	private int groupTrackingMinSize = 3;
}
