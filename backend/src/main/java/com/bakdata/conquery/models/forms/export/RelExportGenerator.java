package com.bakdata.conquery.models.forms.export;

import static com.bakdata.eva.forms.util.ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryManager;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.ResultInfoDecorator;
import com.bakdata.eva.forms.queries.RelativeFormQuery;
import com.bakdata.eva.models.forms.FeatureGroup;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RelExportGenerator {
	
	private Dataset dataset;
	private User user;
	private Namespaces namespaces;
	
	public RelativeFormQuery generate(RelativeMode mode) {
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(mode.getForm().getQueryGroup());
		DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getFeatures(), namespaces);
		DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getOutcomes(), namespaces);
		
		return new RelativeFormQuery(
			(ConceptQuery) prerequisite.getQuery(),
			setInfos(
				AbsExportGenerator.createSubPlan(mode.getFeatures()),
				FeatureGroup.FEATURE
			),
			setInfos(
				AbsExportGenerator.createSubPlan(mode.getOutcomes()),
				FeatureGroup.OUTCOME
			),
			mode.getIndexSelector(),
			mode.getIndexPlacement(),
			mode.getTimeCountBefore(),
			mode.getTimeCountAfter(),
			mode.getTimeUnit()
		);
	}
	
	private ArrayConceptQuery setInfos(ArrayConceptQuery arrayQuery, FeatureGroup group) {
		for(ConceptQuery childQueries : arrayQuery.getChildQueries()) {
			
			 ResultInfoDecorator decorator = new ResultInfoDecorator(
				ImmutableClassToInstanceMap.of(FeatureGroup.class, group),
				childQueries.getRoot()
			);
			childQueries.setRoot(decorator);
		}
		return arrayQuery;
	}

	public ManagedQuery execute(RelativeMode relativeMode, boolean printUserFriendly) throws JSONException {
		QueryManager queryManager = namespaces.get(dataset.getId()).getQueryManager();
		return queryManager.runQuery(generate(relativeMode), user);
	}
}
