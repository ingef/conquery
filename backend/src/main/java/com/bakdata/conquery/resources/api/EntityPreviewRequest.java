package com.bakdata.conquery.resources.api;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class EntityPreviewRequest {
	private String idKind; //TODO I think ID is fallback, but i dont currently know.
	private final String entityId;
	private final Range<LocalDate> time;
	@NsIdRefCollection
	private final List<Connector> sources;

	//TODO I will not merge this now, since this would break preview completely for now
//	@ValidationMethod(message = "Time must be closed.")
//	@JsonIgnore
//	public boolean isTimeClosed() {
//		return !time.isOpen();
//	}
}
