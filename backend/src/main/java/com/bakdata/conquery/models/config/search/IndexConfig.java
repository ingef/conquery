package com.bakdata.conquery.models.config.search;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.models.index.IndexKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class IndexConfig  {

	/**
	 * Base url under which reference files are access if the url in
	 * {@link FilterTemplate} only have a path element (no schema or authority).
	 * Pay attentions that the base url end with a slash <code>/</code>.
	 *
	 * @implNote See {@link IndexKey#getCsv()} for URI<->URL details.
	 */
	@Nullable
	private URI baseUrl;

	@NotNull
	private String emptyLabel = "No Value";

	@JsonIgnore
	@ValidationMethod(message = "Specified baseUrl is not valid")
	public boolean isValidUrl() {
		if (baseUrl == null) {
			// It is okay if no base is specified. Every template needs to specify its full path then.
			return true;
		}
		try {
			// We just try to convert it to an url and discard the return value
			URL _ignore = baseUrl.toURL();
			return true;
		}
		catch (MalformedURLException e) {
			log.error("URL validation error.", e);
			return false;
		}

	}
}
