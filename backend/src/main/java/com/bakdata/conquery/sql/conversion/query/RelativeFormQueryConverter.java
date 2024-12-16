package com.bakdata.conquery.sql.conversion.query;

import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.forms.FormType;
import com.bakdata.conquery.sql.conversion.forms.StratificationTableFactory;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RelativeFormQueryConverter implements NodeConverter<RelativeFormQuery> {

	private final FormConversionHelper formHelper;

	@Override
	public Class<? extends RelativeFormQuery> getConversionClass() {
		return RelativeFormQuery.class;
	}

	@Override
	public ConversionContext convert(RelativeFormQuery form, ConversionContext context) {

		QueryStep convertedPrerequisite = formHelper.convertPrerequisite(form.getQuery(), context);
		StratificationTableFactory tableFactory = new StratificationTableFactory(convertedPrerequisite, context);
		QueryStep stratificationTable = tableFactory.createRelativeStratificationTable(form);

		return formHelper.convertForm(
				FormType.RELATIVE,
				stratificationTable,
				form.getFeatures(),
				form.getResultInfos(),
				context
		);
	}

}
