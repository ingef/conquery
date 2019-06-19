package com.bakdata.eva.models.translation.query.oldmodel.constant;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.eva.models.translation.query.oldmodel.OIQuery;
import com.google.common.collect.Lists;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="CONSTANT_QUERY", base= OIQuery.class)
public class ConstantQuery extends OIQuery {

	private CQFormat format;
	private List<String> values;
	private int columns;
	private long version;


	@Override
	public com.bakdata.conquery.models.query.IQuery translate(DatasetId dataset) {
		final ConceptQuery query = new ConceptQuery();

		//TODO
		final CQExternal cqExternal = new CQExternal(
			null,
			Lists.partition(values, columns).stream().map(val -> val.toArray(new String[0])).toArray(String[][]::new));

		query.setRoot(cqExternal);

		return query;
	}

	@Getter @AllArgsConstructor
	private static enum CQFormat {
		FULL_EXPORT, 
		IMPORT_DATED, 
		IMPORT_SHORT;
	}
	
	
}
