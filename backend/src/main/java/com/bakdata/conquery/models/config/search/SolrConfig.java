package com.bakdata.conquery.models.config.search;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
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

	@NotNull
	private final String baseSolrUrl;
	private Duration connectionTimeout = Duration.seconds(60);
	private Duration requestTimeout = Duration.seconds(60);
	private Duration commitWithin = Duration.seconds(5);
	@Nullable
	private final String username;
	@Nullable
	private final String password;
	@Min(1)
	private int updateChunkSize = 100;

	/**
	 * Label for the special empty value to filter for empty entries.
	 */
	@NotNull
	private String emptyLabel = "No Value";

	/**
	 * Effectively the query that is sent to solr after we split the users search phrase into terms on whitespaces and join them together again after template resolving.
	 * Joining involves a boolean operator, so parentheses might be needed.
	 * The format string only gets a single argument, so refer to the argument using <code>%1$s</code> if you want to use it multiple times.
	 */
	@NotEmpty
	private String queryTemplate = "( %1$s^3 *%1$s*^2 %1$s~^1 )";

	/**
	 * By default, for each value in a column a solr document is created. The id of this solr-document uses the column id and the column value.
	 * This can cause a large number of documents, many referring to the same <code>value</code>.
	 * When this flag is <code>true</code>, not the column id but its name is used to form the document id, hence abstracting over all columns of the same name.
	 * <p/>
	 * Beware that you need to ensure that equally named columns used in filters are also based on the same set of values. Otherwise, you might encounter unexpected
	 * query results.
	 */
	private boolean combineEquallyNamedColumns = false;

	@Override
	public SolrProcessor createSearchProcessor(Environment environment, DatasetId datasetId) {
		try {
			SolrClient client = createManagedSearchClient(environment, datasetId.getName());
			return new SolrProcessor(client, commitWithin, updateChunkSize, queryTemplate, combineEquallyNamedColumns, emptyLabel);
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

        return builder.build();
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
