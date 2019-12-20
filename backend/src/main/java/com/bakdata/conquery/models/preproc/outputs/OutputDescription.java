package com.bakdata.conquery.models.preproc.outputs;

import java.io.Serializable;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;
import java.util.StringJoiner;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.ColumnDescription;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "operation")
@CPSBase
public abstract class OutputDescription implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final List<Object> NULL = Collections.singletonList(null);

	@NotEmpty
	private String name;

	@FunctionalInterface
	public interface Output {
		List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) throws ParsingException;
	}

	protected void assertRequiredHeaders(Object2IntArrayMap<String> actualHeaders, String... headers) {
		StringJoiner missing = new StringJoiner(", ");

		for (String h : headers) {
			if (!actualHeaders.containsKey(h))
				missing.add(h);
		}

		if (missing.length() != 0) {
			throw new InputMismatchException(String.format("Did not find headers `[%s]` in `[%s]`", missing.toString(), actualHeaders.keySet()));
		}
	}

	public abstract Output createForHeaders(Object2IntArrayMap<String> headers);

	@JsonIgnore
	public abstract MajorTypeId getResultType();

	@JsonIgnore
	public ColumnDescription getColumnDescription() {
		return new ColumnDescription(name, getResultType());
	}
}
