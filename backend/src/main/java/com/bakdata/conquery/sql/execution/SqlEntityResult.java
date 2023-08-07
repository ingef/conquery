package com.bakdata.conquery.sql.execution;

import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@AllArgsConstructor(onConstructor_=@JsonCreator)
@CPSType(id="SQL_RESULT", base= EntityResult.class)
public class SqlEntityResult implements EntityResult {

	private final int entityId;
	private final String id;
	private Object[] values;

	public String getId() {
		return id;
	}

	@Override
	public int getEntityId() {
		return entityId;
	}

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
