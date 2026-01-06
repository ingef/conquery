package com.bakdata.conquery.models.config.search.solr;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.search.SearchConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.search.solr.SolrProcessor;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.util.Duration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.request.HealthCheckRequest;
import org.apache.solr.common.util.NamedList;

@CPSType(id = "SOLR", base = SearchConfig.class)
@Data
@Slf4j
public class SolrConfig implements SearchConfig {

	/**
	 * Url to solr. Typically ends on <code>/solr</code>
	 */
	@NotNull
	private final String baseSolrUrl;
	private Duration connectionTimeout = Duration.seconds(60);
	private Duration requestTimeout = Duration.seconds(60);
	private Duration commitWithin = Duration.seconds(5);
	@Nullable
	private final String username;
	@Nullable
	private final String password;

	@NotNull
	@NotNull
	private FilterValueConfig filterValue = new FilterValueConfig();

	@Min(1)
	private int indexerClientThreads = Runtime.getRuntime().availableProcessors();
	@Min(1)
	private int indexerClientQueueSize = 10;

	@Override
	public SolrProcessor createSearchProcessor(Environment environment, DatasetId datasetId) {
		SolrProcessor solrProcessor = new SolrProcessor(() -> createSearchClient(datasetId.getName()),() -> createIndexClient(datasetId.getName()), commitWithin, filterValue);

		try {
			solrProcessor.start();
			return solrProcessor;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
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

        return builder.build();
	}

	public SolrClient createIndexClient(@Nullable String collection) {
		log.info("Creating solr index client. Base url: {}, Collection: {})", baseSolrUrl, collection);

		Http2SolrClient.Builder http2Builder = new Http2SolrClient.Builder(baseSolrUrl)
				.withConnectionTimeout(connectionTimeout.toSeconds(), TimeUnit.SECONDS)
				.withRequestTimeout(requestTimeout.toSeconds(), TimeUnit.SECONDS);

		if (username != null) {
			http2Builder.withBasicAuthCredentials(username, password);
		}

		Http2SolrClient http2Client = http2Builder.build();

		ConcurrentUpdateHttp2SolrClient.Builder concurrentClientBuilder = new ConcurrentUpdateHttp2SolrClient.Builder(baseSolrUrl, http2Client)
				.withDefaultCollection(collection)
				.withQueueSize(indexerClientQueueSize)
				.withThreadCount(indexerClientThreads);


		return concurrentClientBuilder.build();
	}

	public HealthCheck createHealthCheck(SolrClient client) {
		return new HealthCheck() {
			@Override
			protected Result check() throws Exception {
				NamedList<Object> response = client.request(new HealthCheckRequest());
				String status = (String) response.get("status");
				if ("healthy".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status)) {
					return Result.builder().healthy().build();
				}
				else {
					return Result.unhealthy(status);
				}
			}
		};
	}
}
