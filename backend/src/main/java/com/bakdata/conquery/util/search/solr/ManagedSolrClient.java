package com.bakdata.conquery.util.search.solr;

import java.io.IOException;

import io.dropwizard.lifecycle.Managed;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.util.NamedList;

/**
 * Wrapper for Dropwizard's lifecycle
 */
@RequiredArgsConstructor
public class ManagedSolrClient extends SolrClient implements Managed {

	@Delegate(excludes = DelegateExclude.class)
	public final SolrClient delegate;

	@Override
	public void stop() throws Exception {
		delegate.close();
	}

	@Override
	public NamedList<Object> request(SolrRequest<?> request, String collection) throws SolrServerException, IOException {
		return delegate.request(request,collection);
	}

	/**
	 * Little workaround. See: <a href="https://github.com/projectlombok/lombok/issues/941">...</a>
	 */
	interface DelegateExclude {
		NamedList<Object> request(final SolrRequest<?> request);
	}
}
