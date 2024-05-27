package com.bakdata.conquery.metrics.prometheus;

import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.metrics.exporter.servlet.jakarta.PrometheusMetricsServlet;
import io.prometheus.metrics.simpleclient.bridge.SimpleclientCollector;
import org.eclipse.jetty.servlet.ServletHolder;

public class PrometheusBundle implements ConfiguredBundle<ConqueryConfig> {

	@Override
	public void initialize(Bootstrap<?> bootstrap) {
		// Init simple_client https://www.robustperception.io/exposing-dropwizard-metrics-to-prometheus/
		CollectorRegistry.defaultRegistry.register(new DropwizardExports(bootstrap.getMetricRegistry()));
		// Init client_java bridge: https://prometheus.github.io/client_java/migration/simpleclient/
		SimpleclientCollector.builder().register();
	}

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {

		environment.getAdminContext().addServlet(new ServletHolder(new PrometheusMetricsServlet()), "/metrics-prometheus");
	}
}
