package com.bakdata.conquery.resources.api;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class EntityPreviewRequest {
	private String idKind; //TODO I think ID is fallback, but i dont currently know.
	private final String entityId;
	private final Range<LocalDate> time;
	@NotEmpty
	private final List<ConnectorId> sources;

	//TODO uncomment, when frontend is adapted to support this
	//	@ValidationMethod(message = "Time must be closed.")
	//	@JsonIgnore
	//	public boolean isTimeClosed() {
	//		return !time.isOpen();
	//	}
}
