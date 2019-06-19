package com.bakdata.eva.models.translation.query.oldmodel.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.eva.models.translation.query.oldmodel.OIQuery;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor

@CPSType(id="CONCEPT_QUERY", base= OIQuery.class)
public class OConceptQuery extends OIQuery {
	
	@Min(0)
	private long version = -1;
	@Valid @NotEmpty @JsonManagedReference
	private List<QGroup> groups = new ArrayList<>();
	// groups verundet und elemente verodert

	@Override
	public IQuery translate(DatasetId dataset) {
		final ConceptQuery concept = new ConceptQuery();

		final CQAnd and = new CQAnd();
		and.setChildren(groups.stream().map(qGroup -> qGroup.translate(dataset)).collect(Collectors.toList()));

		concept.setRoot(and);

		return concept;
	}
}