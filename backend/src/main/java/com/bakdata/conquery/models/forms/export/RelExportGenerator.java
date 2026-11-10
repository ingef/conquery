package com.bakdata.conquery.models.forms.export;

import java.util.List;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.IndexSelector;
import com.bakdata.conquery.models.forms.util.IndexPlacement;
import com.bakdata.conquery.models.forms.util.ResolutionAndAlignment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RelExportGenerator {

	public static RelativeFormQuery generate(RelativeMode mode) {

		final List<ResolutionAndAlignment>
				resolutionsAndAlignments =
				ExportForm.getResolutionAlignmentMap(mode.getForm().getResolvedResolutions(), mode.getTimeUnit().getAlignment());

		return generate(mode.getForm()
							.getPrerequisite(), mode.getResolvedFeatures(), mode.getIndexSelector(), mode.getIndexPlacement(), mode.getTimeCountBefore(), mode.getTimeCountAfter(), mode.getTimeUnit(), resolutionsAndAlignments);
	}

	public static RelativeFormQuery generate(Query query, ArrayConceptQuery features, IndexSelector indexSelector, IndexPlacement indexPlacement, int timeCountBefore, int timeCountAfter, CalendarUnit timeUnit, List<ResolutionAndAlignment> resolutionsAndAlignments) {

		return new RelativeFormQuery(
				query,
				features,
				indexSelector,
				indexPlacement,
				timeCountBefore,
				timeCountAfter,
				timeUnit,
				resolutionsAndAlignments
		);
	}

}
