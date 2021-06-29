package com.bakdata.conquery.models.forms.export;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AbsExportGenerator {

	public static AbsoluteFormQuery generate(AbsoluteMode mode) {

		List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments =
				ExportForm.getResolutionAlignmentMap(mode.getForm().getResolvedResolutions(), mode.getAlignmentHint());

		return generate(mode.getFeatures(), mode.getForm()
												.getPrerequisite(), mode.getDateRange(), resolutionsAndAlignments);
	}

	public static AbsoluteFormQuery generate(List<CQElement> features, Query queryGroup, Range<LocalDate> dateRange, List<ExportForm.ResolutionAndAlignment> resolutionAndAlignment) {

		// Apply defaults to user concepts
		ExportForm.DefaultSelectSettable.enable(features);


		AbsoluteFormQuery query = new AbsoluteFormQuery(
				queryGroup,
				dateRange,
				ArrayConceptQuery.createFromFeatures(features),
				resolutionAndAlignment
		);

		return query;
	}


}
