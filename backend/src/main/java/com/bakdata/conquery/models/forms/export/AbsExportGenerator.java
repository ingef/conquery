package com.bakdata.conquery.models.forms.export;

import static com.bakdata.eva.forms.util.ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryManager;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.queries.AbsoluteFormQuery;
import com.bakdata.eva.models.forms.DateContextMode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AbsExportGenerator {
	
	private Dataset dataset;
	private User user;
	private Namespaces namespaces;
	
	public ManagedQuery executeQuery(AbsoluteMode mode, DateContextMode resolution) throws JSONException {
		QueryManager queryManager = namespaces.get(dataset.getId()).getQueryManager();
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(mode.getForm().getQueryGroup());
		
		// Apply defaults to user concept
		DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getFeatures(), namespaces);
		
		AbsoluteFormQuery query = new AbsoluteFormQuery(
			(ConceptQuery) prerequisite.getQuery(),
			mode.getDateRange(),
			createSubPlan(mode.getFeatures()),
			resolution
		);
		
		return queryManager.runQuery(query, user);
	}
	
	public static ArrayConceptQuery createSubPlan(List<CQElement> features) {
		List<ConceptQuery> cqWraps = features.stream().map(ConceptQuery::new).collect(Collectors.toList());
		return createSubPlanFromQueries(cqWraps);
	}
	
	public static ArrayConceptQuery createSubPlanFromQueries(List<ConceptQuery> featureQueries) {
		ArrayConceptQuery subQuery = new ArrayConceptQuery();
		subQuery.setChildQueries(featureQueries);
		return subQuery;
	}
}
