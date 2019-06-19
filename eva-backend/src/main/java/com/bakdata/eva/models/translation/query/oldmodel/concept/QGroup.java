package com.bakdata.eva.models.translation.query.oldmodel.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.specific.CQDateRestriction;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QGroup {

	@JsonBackReference
	@NotNull
	private OConceptQuery query;

	@JsonManagedReference
	@Valid
	@NotEmpty
	private List<QElement> elements = new ArrayList<>();

	private boolean exclude = false;

	@Valid
	private DateRange dateRange;

	public CQElement translate(DatasetId dataset) {
		final CQDateRestriction dateRestriction = new CQDateRestriction();
		dateRestriction.setDateRange(dateRange == null ? null : dateRange.translate());

		final CQOr or = new CQOr();
		or.setChildren(elements.stream().map(qElement -> qElement.translate(dataset)).collect(Collectors.toList()));

		dateRestriction.setChild(or);

		return dateRestriction;
	}
}