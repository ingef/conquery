package com.bakdata.conquery.models.query.results;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
@CPSType(id="MULTILINE_CONTAINED", base=EntityResult.class)
public class MultilineContainedEntityResult implements ContainedEntityResult {
	
	@Min(0)
	private final int entityId;
	@NotNull
	private final List<Object[]> values;

	//this is needed because of https://github.com/FasterXML/jackson-databind/issues/2024
	public MultilineContainedEntityResult(int entityId, List<Object[]> values) {
		this.entityId = entityId;
		this.values = Objects.requireNonNullElse(values, Collections.emptyList());
	}

	@Override
	public Stream<Object[]> streamValues() {
		return values.stream();
	}

	@Override
	public boolean isFailed() {
		return false;
	}

	@Override
	public boolean isContained() {
		return true;
	}

	@Override
	public int columnCount() {
		// We look at the first result line to determine the number of columns
		return values.get(0).length;
	}

	@Override
	public void modifyResultLinesInplace(UnaryOperator<Object[]> lineModifier) {
		values.replaceAll(lineModifier);
	}

	@Override
	public List<Object[]> listResultLines() {
		return values;
	}
}
