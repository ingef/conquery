package com.bakdata.conquery.models.forms.export;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AbsExportGenerator {
	
	public static AbsoluteFormQuery generate(Namespaces namespaces, AbsoluteMode mode, DateContextMode resolution, boolean alsoCreateCoarserSubdivisions, UserId userId, DatasetId submittedDataset) {
		return generate(namespaces, mode, resolution.getThisAndCoarserSubdivisions(), userId, submittedDataset);
	}
	
	public static AbsoluteFormQuery generate(Namespaces namespaces, AbsoluteMode mode, List<DateContextMode> resolutions, UserId userId, DatasetId submittedDataset) {
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(mode.getForm().getQueryGroup());
	
		// Apply defaults to user concept
		ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(mode.getFeatures(), namespaces);
		
		AbsoluteFormQuery query = new AbsoluteFormQuery(
			(ConceptQuery) prerequisite.getQuery(),
			mode.getDateRange(),
			createSubQuery(mode.getFeatures()),
			resolutions
		);
		
		return query;
	}
	
	public static ArrayConceptQuery createSubQuery(List<CQElement> features) {
		List<ConceptQuery> cqWraps = features.stream().map(ConceptQuery::new).collect(Collectors.toList());
		return new ArrayConceptQuery(cqWraps);
	}
}
