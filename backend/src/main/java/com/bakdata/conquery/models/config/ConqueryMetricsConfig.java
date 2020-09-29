package com.bakdata.conquery.models.config;

import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConqueryMetricsConfig {
	private Duration userActiveDuration = Duration.hours(1);
	private int groupTrackingMinSize = 3;
}
