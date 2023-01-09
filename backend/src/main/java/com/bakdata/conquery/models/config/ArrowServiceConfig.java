package com.bakdata.conquery.models.config;

import javax.validation.constraints.Min;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.service.AbstractResultProviderConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@CPSType(base = PluginConfig.class, id = "ARROW")
public class ArrowServiceConfig extends AbstractResultProviderConfig {

	@Min(1)
	private int batchSize = 1000;

	public ArrowServiceConfig() {
		super(true, 0);
	}

	public ArrowServiceConfig(boolean hidden, int priority, int batchSize) {
		super(hidden, priority);
		this.batchSize = batchSize;
	}
}
