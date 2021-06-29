package com.bakdata.conquery.models.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdentifiableCollector;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryTranslator {

	public <T extends Query> T replaceDataset(DatasetRegistry namespaces, T element, Dataset target) {
		if(element instanceof ConceptQuery) {
			CQElement root = replaceDataset(namespaces, ((ConceptQuery) element).getRoot(), target.getId());
			return (T) new ConceptQuery(root, ((ConceptQuery) element).getDateAggregationMode());
		}
		throw new IllegalStateException(String.format("Can't translate non ConceptQuery IQueries: %s", element.getClass()));
	}
	
	public <T extends Visitable> T replaceDataset(DatasetRegistry namespaces, T element, DatasetId target) {
		try {
			String value = Jackson.MAPPER.writeValueAsString(element);
			
			NamespacedIdentifiableCollector collector = new NamespacedIdentifiableCollector();
			
			element.visit(collector);

			Pattern[] patterns =
					collector.getIdentifiables()
							 .stream()
							 .map(NamespacedIdentifiable::getDataset)
							 .map(Dataset::getId)
							 .map(IId::toString)
							 // ?<= -- non-capturing assertion, to start with "
							 // ?= --  non-capturing assertion to end with [."]
							 .map(n -> Pattern.compile("(?<=(\"))" + Pattern.quote(n) + "(?=([.\"]))"))
							 .toArray(Pattern[]::new);
	
			String replacement = Matcher.quoteReplacement(target.toString());
			for (Pattern pattern : patterns) {
				value = pattern.matcher(value).replaceAll(replacement);
			}
	
			return (T)namespaces
				.injectInto(Jackson.MAPPER.copy())
				.readValue(value, element.getClass());
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to translate element "+element+" to dataset "+target, e);
		}
	}
}
