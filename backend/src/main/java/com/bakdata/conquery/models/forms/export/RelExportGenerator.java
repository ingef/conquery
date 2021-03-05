package com.bakdata.conquery.models.forms.export;

import java.util.List;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.ResultInfoDecorator;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RelExportGenerator {
	
	public static RelativeFormQuery generate(DatasetRegistry namespaces, RelativeMode mode, UserId userId, DatasetId submittedDataset) {


		List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments = ExportForm.getResolutionAlignmentMap(mode.getForm().getResolvedResolutions(), mode.getTimeUnit().getAlignment());

		return generate(mode.getForm().getPrerequisite(), mode.getFeatures(), mode.getOutcomes(), mode.getIndexSelector(), mode.getIndexPlacement(), mode.getTimeCountBefore(), mode.getTimeCountAfter(), mode.getTimeUnit(), namespaces, resolutionsAndAlignments);
	}
	
	public static RelativeFormQuery generate(IQuery query, List<CQElement> features, List<CQElement> outcomes, TemporalSampler indexSelector, IndexPlacement indexPlacement, int timeCountBefore, int timeCountAfter, DateContext.CalendarUnit timeUnit, DatasetRegistry namespaces, List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments) {
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(features, namespaces);
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(outcomes, namespaces);

		return new RelativeFormQuery(
			query, 
			setInfos(
					ArrayConceptQuery.createFromFeatures(features),
					FeatureGroup.FEATURE
			),
			setInfos(
					ArrayConceptQuery.createFromFeatures(outcomes),
					FeatureGroup.OUTCOME
			),
			indexSelector, 
			indexPlacement, 
			timeCountBefore, 
			timeCountAfter, 
			timeUnit,
			resolutionsAndAlignments);
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
