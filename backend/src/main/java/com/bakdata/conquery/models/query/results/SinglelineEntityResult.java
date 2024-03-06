package com.bakdata.conquery.models.query.results;

import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter @AllArgsConstructor(onConstructor_=@JsonCreator)
@CPSType(id="SINGLE_LINE", base= EntityResult.class)
public class SinglelineEntityResult implements EntityResult {

	private final String entityId;
	private Object[] values;

	@Override
	public Stream<Object[]> streamValues() {
		return Stream.ofNullable(values);
	}

	@Override
	public int columnCount() {
		return values.length;
	}

	@Override
	public void modifyResultLinesInplace(UnaryOperator<Object[]> lineModifier) {
		values = lineModifier.apply(values);
	}

	@Override
	public List<Object[]> listResultLines() {
		return Collections.singletonList(values);
	}
}
