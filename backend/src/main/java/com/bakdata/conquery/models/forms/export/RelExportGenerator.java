package com.bakdata.conquery.models.forms.export;

import java.util.List;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.ResultInfoDecorator;
import com.bakdata.conquery.models.worker.Namespaces;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RelExportGenerator {
	
	public static RelativeFormQuery generate(Namespaces namespaces, RelativeMode mode, UserId userId, DatasetId submittedDataset) {
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(mode.getForm().getQueryGroup());
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getFeatures(), namespaces);
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getOutcomes(), namespaces);
		
		List<DateContextMode> resolutions = null;
		if(mode.getForm().isAlsoCreateCoarserSubdivisions()) {
			if(mode.getForm().getResolution().size() != 1) {
				throw new IllegalStateException("Abort Form creation, because coarser subdivision are requested and multiple resolutions are given. With 'alsoCreateCoarserSubdivisions' set to true, provide only one resolution.");
			}
			resolutions = mode.getForm().getResolution().get(0).getThisAndCoarserSubdivisions();
		}
		else {
			resolutions = mode.getForm().getResolution();
		}
		
		RelativeFormQuery query = new RelativeFormQuery(
			(ConceptQuery) prerequisite.getQuery(),
			setInfos(
				AbsExportGenerator.createSubQuery(mode.getFeatures()),
				FeatureGroup.FEATURE
			),
			setInfos(
				AbsExportGenerator.createSubQuery(mode.getOutcomes()),
				FeatureGroup.OUTCOME
			),
			mode.getIndexSelector(),
			mode.getIndexPlacement(),
			mode.getTimeCountBefore(),
			mode.getTimeCountAfter(),
			mode.getTimeUnit(),
			resolutions
		);
		return query;
	}
	
	/**
	 * Wraps the feature/outcome query with the corresponding info, if it is a feature or outcome
	 * do generate a ColumnDescriptor later on.
	 */
	private static ArrayConceptQuery setInfos(ArrayConceptQuery arrayQuery, FeatureGroup group) {
		for(ConceptQuery childQueries : arrayQuery.getChildQueries()) {
			
			 ResultInfoDecorator decorator = new ResultInfoDecorator(
				ImmutableClassToInstanceMap.of(FeatureGroup.class, group),
				childQueries.getRoot()
			);
			childQueries.setRoot(decorator);
		}
		return arrayQuery;
	}
}
