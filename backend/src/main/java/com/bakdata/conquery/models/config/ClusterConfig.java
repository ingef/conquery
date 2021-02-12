package com.bakdata.conquery.models.config;

import java.net.InetAddress;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;
import io.dropwizard.validation.PortRange;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ClusterConfig extends Configuration {
	@PortRange
	private int port = 16170;
	@Valid @NotNull
	private InetAddress managerURL = InetAddress.getLoopbackAddress();
	@Valid @NotNull
	private MinaConfig mina = new MinaConfig();
	@Min(1)
	private int entityBucketSize = 1000;

	private int backpressure = 100;
}
