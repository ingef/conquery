package com.bakdata.eva.models.translation.query.oldmodel.concept;

import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@CPSType(id = "QUERY", base = QElement.class)
@Getter @Setter
@NoArgsConstructor

public class QReusedQuery extends QElement {
	private UUID id;

	@Override
	public CQElement translate(DatasetId dataset) {
		return new CQReusedQuery(new ManagedExecutionId(dataset, id));
	}
}
