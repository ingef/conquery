package com.bakdata.conquery.io.result.ResultRender;

import java.net.URL;
import java.util.Collection;
import java.util.Comparator;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.models.execution.ManagedExecution;

public interface ResultRendererProvider extends Comparable<ResultRendererProvider> {

	Comparator<ResultRendererProvider> COMPARATOR = Comparator.comparing(ResultRendererProvider::getPriority).thenComparing(o -> o.getClass().getName());

	/**
	 * The provider can return a result url (or more) if its renderer supports the execution type.
	 * If additionally allProviders is set to true it should output an url.
	 *
	 * @param exec         The execution whose result needs to be rendered.
	 * @param uriBuilder   The pre-configured builder for the url.
	 * @param allProviders A flag that should override internal "hide-this-url" flags.
	 * @return An Optional with the url or an empty optional.
	 */
	Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders);

	/**
	 * The priority determines the order of the result urls from this and other providers in a list.
	 * This list is generated for {@link com.bakdata.conquery.apiv1.QueryProcessor#getDownloadUrls}
	 * The lower the int, the further at the beginning of the list are the result urls.
	 * When priorities clash, the full class name is used for further comparison.
	 *
	 * @return
	 */
	int getPriority();

	@Override
	default int compareTo(ResultRendererProvider o) {
		return COMPARATOR.compare(this, o);
	}
}
