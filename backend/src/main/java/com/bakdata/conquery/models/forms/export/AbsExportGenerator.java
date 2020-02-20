package com.bakdata.conquery.models.forms.export;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AbsExportGenerator {
	
	private Dataset dataset;
	private User user;
	private Namespaces namespaces;
	
	public static AbsoluteFormQuery generate(Namespaces namespaces, AbsoluteMode mode, DateContextMode resolution) {
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(mode.getForm().getQueryGroup());
	
		// Apply defaults to user concept
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getFeatures(), namespaces);
		
		AbsoluteFormQuery query = new AbsoluteFormQuery(
			(ConceptQuery) prerequisite.getQuery(),
			mode.getDateRange(),
			createSubPlan(mode.getFeatures()),
			resolution
		);
		
		return query;
	}
	
	public ManagedQuery executeQuery(AbsoluteMode mode, DateContextMode resolution) throws JSONException {
		ExecutionManager queryManager = namespaces.get(dataset.getId()).getQueryManager();
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(mode.getForm().getQueryGroup());
		
		// Apply defaults to user concept
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getFeatures(), namespaces);
		
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
