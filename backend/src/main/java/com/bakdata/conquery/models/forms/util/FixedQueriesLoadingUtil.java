package com.bakdata.conquery.models.forms.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.powerlibraries.io.In;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FixedQueriesLoadingUtil {


	/**
	 * Loads the JSON containing the fixed sub query templates for this form from
	 * the specified file.
	 */
	public static String loadFixedQueries(String resourcePath) {
		try {
			return Objects.requireNonNull(
				new String(
					In.resource(resourcePath)
					.asStream()
					.readAllBytes()));
		}
		catch (IOException e) {
			throw new IllegalStateException(String.format("Failed to get resource: %s", resourcePath), e);
		}
	}


	/**
	 * Parses the query template with the supplied dataset.
	 */
	public static List<ConceptQuery> parseQueriesWithDataset(String queryString, DatasetId datasetId, Namespaces namespaces) {
		queryString = queryString.replace("${dataset}", datasetId.toString());
		ObjectReader reader = namespaces.injectInto(Jackson.MAPPER.copy().readerFor(ConceptQuery[].class));
		try {
			return new ArrayList<>(Arrays.asList(reader.readValue(queryString)));
		}
		catch (IOException e) {
			throw new IllegalStateException(String.format("Could not parse the fixed queries from:\n%s", queryString), e);
		}
	}
	
	/**
	 * Parses the query template with the supplied dataset.
	 */
	public static List<CQElement> parseFeaturesWithDataset(String featureString, DatasetId datasetId, Namespaces namespaces) {
		featureString = featureString.replace("${dataset}", datasetId.toString());
		ObjectReader reader = namespaces.injectInto(Jackson.MAPPER.copy().readerFor(CQElement[].class));
		try {
			return new ArrayList<>(Arrays.asList(reader.readValue(featureString)));
		}
		catch (IOException e) {
			throw new IllegalStateException(String.format("Could not parse CQElements from:\n%s", featureString), e);
		}
	}
	
	/**
	 * Parses the query template with the supplied dataset.
	 */
	public static List<FilterValue<?>> parseFiltersWithDataset(String featureString, DatasetId datasetId, Namespaces namespaces) {
		featureString = featureString.replace("${dataset}", datasetId.toString());
		ObjectReader reader = namespaces.injectInto(Jackson.MAPPER.copy().readerFor(FilterValue[].class));
		try {
			return new ArrayList<>(Arrays.asList(reader.readValue(featureString)));
		}
		catch (IOException e) {
			throw new IllegalStateException(String.format("Could not parse Filters from:\n%s", featureString), e);
		}
	}
}
