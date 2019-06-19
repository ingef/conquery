package com.bakdata.eva.models.translation.query.oldmodel.concept;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.google.common.base.Strings;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter
@NoArgsConstructor

@Slf4j
public class QTable {

	@Valid @NotNull
	private String id;
	@Valid @NotNull
	private List<QFilter> filters = Collections.emptyList();
	@JsonBackReference
	private QConcepts concept;

	public CQTable translate(DatasetId dataset, ConceptId conceptId) {
		final CQTable cqTable = new CQTable();

		if(Strings.isNullOrEmpty(id)){
			log.warn("Id of QTable is null");
			return null;
		}

		cqTable.setId(ConnectorId.Parser.INSTANCE.parse(dataset.getName() + "." + id));

		cqTable.setFilters(
			(List<FilterValue<?>>) filters.stream()
				.map(filter -> (FilterValue<?>) filter.translate(dataset, conceptId, cqTable.getId()))
				.collect(Collectors.toList())
		);

		return cqTable;
	}
}
