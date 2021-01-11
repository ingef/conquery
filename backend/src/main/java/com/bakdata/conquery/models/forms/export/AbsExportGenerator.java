package com.bakdata.conquery.models.forms.export;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@AllArgsConstructor
public class AbsExportGenerator {
	

	public static AbsoluteFormQuery generate(DatasetRegistry namespaces, AbsoluteMode mode, UserId userId, DatasetId submittedDataset, DateContext.Alignment alignmentHint) {

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

		List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments = ExportForm.getResolutionAlignmentMap(resolutions, mode.getAlignmentHint());

		return generate(namespaces, userId, submittedDataset, mode.getFeatures(), mode.getForm().getPrerequisite(), mode.getDateRange(), resolutionsAndAlignments);
	}

	public static AbsoluteFormQuery generate(DatasetRegistry namespaces, UserId userId, DatasetId submittedDataset, List<CQElement> features, IQuery queryGroup, Range<LocalDate> dateRange, List<ExportForm.ResolutionAndAlignment> resolutionAndAlignment) {
		
		// Apply defaults to user concept
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(features, namespaces);
		
		AbsoluteFormQuery query = new AbsoluteFormQuery(
			queryGroup,
			dateRange,
			createSubQuery(features),
			resolutionAndAlignment
		);
		
		return query;
	}
	
	public static ArrayConceptQuery createSubQuery(List<CQElement> features) {
		List<ConceptQuery> cqWraps = features.stream()
			.map(ConceptQuery::new)
			.collect(Collectors.toList());
		return new ArrayConceptQuery(cqWraps);
	}
}
