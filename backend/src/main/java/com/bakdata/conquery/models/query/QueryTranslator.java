package com.bakdata.conquery.models.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryTranslator {

	public <T extends IQuery> T replaceDataset(Namespaces namespaces, T element, DatasetId target) {
		if(element instanceof ConceptQuery) {
			CQElement root = replaceDataset(namespaces, ((ConceptQuery) element).getRoot(), target);
			return (T) new ConceptQuery(root);
		}
		throw new IllegalStateException(String.format("Can't translate non ConceptQuery IQueries: %s", element.getClass()));
	}
	
	public <T extends CQElement> T replaceDataset(Namespaces namespaces, T element, DatasetId target) {
		try {
			String value = Jackson.MAPPER.writeValueAsString(element);
			
			NamespacedIdCollector collector = new NamespacedIdCollector();
			
			element.visit(collector);
	
			Pattern[] patterns = collector.getIds()
				.stream()
				.map(NamespacedId::getDataset)
				.map(DatasetId::toString)
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
				.readValue(value, CQElement.class);
		}
		catch(Exception e) {
			throw new RuntimeException("Failed to translate element "+element+" to dataset "+target, e);
		}
	}
}
