package com.bakdata.eva.models.config;


import java.net.URL;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.PluginConfig;

import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id = "STATISTIC_SERVER", base = PluginConfig.class)
public class StatisticConfig implements PluginConfig{
	@NotEmpty
	private URL url;
	@NotNull @Valid
	private Duration timeout = Duration.seconds(60);
}
