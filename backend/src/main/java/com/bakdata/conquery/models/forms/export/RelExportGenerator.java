package com.bakdata.conquery.models.forms.export;

import java.util.List;

import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.concept.specific.ResultInfoDecorator;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RelExportGenerator {
	
	public static RelativeFormQuery generate(RelativeMode mode) {


		List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments = ExportForm.getResolutionAlignmentMap(mode.getForm().getResolvedResolutions(), mode.getTimeUnit().getAlignment());

		return generate(mode.getForm().getPrerequisite(), mode.getResolvedFeatures(), mode.getResolvedOutcomes(), mode.getIndexSelector(), mode.getIndexPlacement(), mode.getTimeCountBefore(), mode.getTimeCountAfter(), mode.getTimeUnit(), resolutionsAndAlignments);
	}
	
	public static RelativeFormQuery generate(Query query, ArrayConceptQuery features, ArrayConceptQuery outcomes, TemporalSampler indexSelector, IndexPlacement indexPlacement, int timeCountBefore, int timeCountAfter, DateContext.CalendarUnit timeUnit, List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments) {

		return new RelativeFormQuery(
			query, 
			setInfos(
				features,
					FeatureGroup.FEATURE
			),
			setInfos(
				outcomes,
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
