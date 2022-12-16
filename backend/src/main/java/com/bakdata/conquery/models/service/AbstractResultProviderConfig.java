package com.bakdata.conquery.models.service;

import com.bakdata.conquery.models.config.PluginConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public abstract class AbstractResultProviderConfig implements PluginConfig {

	private boolean hidden = false;

	private int priority = 0;
}
