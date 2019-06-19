package com.bakdata.eva.models.translation.query.oldmodel.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@CPSType(id = "CONCEPT", base = QElement.class)
@CPSType(id = "CONCEPT_LIST", base = QElement.class)
@Getter
@Setter
@NoArgsConstructor

public class QConcepts extends QElement {

	@Valid
	@NotNull
	private List<String> ids = new ArrayList<>();

	public void setId(String id) {
		ids.add(id);
	}

	@NotEmpty
	private String label;

	@Valid
	@JsonManagedReference
	@NotEmpty
	private List<QTable> tables;

	@Override
	public CQElement translate(DatasetId dataset) {
		final CQConcept cqConcept = new CQConcept();
		cqConcept.setLabel(label);

		cqConcept.setIds(
			ids.stream()
				.map(cid -> ConceptElementId.Parser.INSTANCE.parse(dataset.getName() + "." + cid))
				.collect(Collectors.toList())
		);

		cqConcept.setTables(tables.stream()
			.map(table -> table.translate(dataset, null))
			.collect(Collectors.toList()));

		return cqConcept;
	}
}