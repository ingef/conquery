package com.bakdata.conquery.models.forms.export;

import java.util.List;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.apiv1.forms.FeatureGroup;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
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

		List<DateContext.Resolution> resolutions = null;
		if(mode.getForm().isAlsoCreateCoarserSubdivisions()) {
			if(mode.getForm().getResolution().size() != 1) {
				throw new IllegalStateException("Abort Form creation, because coarser subdivision are requested and multiple resolutions are given. With 'alsoCreateCoarserSubdivisions' set to true, provide only one resolution.");
			}
			resolutions = mode.getForm().getResolution().get(0).getThisAndCoarserSubdivisions();
		}
		else {
			resolutions = mode.getForm().getResolution();
		}

		List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments = ExportForm.getResolutionAlignmentMap(resolutions, mode.getForm().getAlignmentHint());

		return generate(mode.getForm().getPrerequisite(), mode.getFeatures(), mode.getOutcomes(), mode.getIndexSelector(), mode.getIndexPlacement(), mode.getTimeCountBefore(), mode.getTimeCountAfter(), mode.getTimeUnit(), namespaces, resolutionsAndAlignments);
	}
	
	public static RelativeFormQuery generate(IQuery query, List<CQElement> features, List<CQElement> outcomes, TemporalSampler indexSelector, IndexPlacement indexPlacement, int timeCountBefore, int timeCountAfter, DateContext.Resolution timeUnit, DatasetRegistry namespaces, List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments) {
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(features, namespaces);
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(outcomes, namespaces);

		return new RelativeFormQuery(
			query, 
			setInfos(
				AbsExportGenerator.createSubQuery(features),
				FeatureGroup.FEATURE
			),
			setInfos(
				AbsExportGenerator.createSubQuery(outcomes),
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
