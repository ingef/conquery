package com.bakdata.conquery.models.config.search;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.search.SearchProcessor;
import com.bakdata.conquery.util.search.solr.ManagedSolrClient;
import com.bakdata.conquery.util.search.solr.SolrProcessor;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.util.Duration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.request.HealthCheckRequest;
import org.apache.solr.common.util.NamedList;

@CPSType(id = "SOLR", base = SearchConfig.class)
@Data
@Slf4j
public class SolrConfig implements SearchConfig {

	private final String baseSolrUrl;
	private Duration connectionTimeout = Duration.seconds(60);
	private Duration requestTimeout = Duration.seconds(60);
	private final String username;
	private final String password;

	@Override
	public SearchProcessor createSearchProcessor(Environment environment, DatasetId datasetId) {
		try {
			SolrClient client = createManagedSearchClient(environment, datasetId.getName());
			return new SolrProcessor(client);
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized ManagedSolrClient createManagedSearchClient(Environment environment, String collection) throws MalformedURLException {
		ManagedSolrClient managedSolrClient = new ManagedSolrClient(createSearchClient(collection));
		environment.lifecycle().manage(managedSolrClient);
		return managedSolrClient;
	}

	public SolrClient createSearchClient(@Nullable String collection) {
		log.info("Creating solr search client. Base url: {}, Collection: {})", baseSolrUrl, collection);

		HttpJdkSolrClient.Builder builder = new HttpJdkSolrClient.Builder(baseSolrUrl)
				.withConnectionTimeout(connectionTimeout.toSeconds(), TimeUnit.SECONDS)
				.withRequestTimeout(requestTimeout.toSeconds(), TimeUnit.SECONDS);

		if (collection != null) {
			builder.withDefaultCollection(collection);
		}

		if (username != null) {
			builder.withBasicAuthCredentials(username, password);
		}

		SolrClient client = builder.build();

		return client;
	}

	public HealthCheck createHealthCheck(SolrClient client) {
		return new HealthCheck() {
			@Override
			protected Result check() throws Exception {
				NamedList<Object> response = client.request(new HealthCheckRequest());
				String status = (String) response.get("status");
				if ("healthy".equals(status)) {
					return Result.builder().healthy().build();
				}
				else {
					return Result.unhealthy(status);
				}
			}
		};
	}
}
