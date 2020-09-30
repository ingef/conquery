package com.bakdata.conquery.models.forms.export;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
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

@AllArgsConstructor
public class AbsExportGenerator {
	

	public static AbsoluteFormQuery generate(DatasetRegistry namespaces, AbsoluteMode mode, UserId userId, DatasetId submittedDataset) {
		
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
		return generate(namespaces, resolutions, userId, submittedDataset, mode.getFeatures(), mode.getForm().getPrerequisite(), mode.getDateRange());
	}
	
	public static AbsoluteFormQuery generate(DatasetRegistry namespaces, List<DateContextMode> resolutions, UserId userId, DatasetId submittedDataset, List<CQElement> features, IQuery queryGroup, Range<LocalDate> dateRange) {
		
		// Apply defaults to user concept
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(features, namespaces);
		
		AbsoluteFormQuery query = new AbsoluteFormQuery(
			queryGroup,
			dateRange,
			createSubQuery(features),
			resolutions
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
