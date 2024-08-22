package com.bakdata.conquery.models.query.resultinfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.ResultPrinters;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Slf4j
public abstract class ResultInfo {

	private final PrintSettings settings;

	@ToString.Include
	private final Set<SemanticType> semantics = new HashSet<>();

	protected ResultInfo(Collection<SemanticType> semantics, PrintSettings settings) {
		this.settings = settings;
		this.semantics.addAll(semantics);
	}

	public final void addSemantics(SemanticType... incoming) {
		semantics.addAll(Arrays.asList(incoming));
	}

	public abstract String userColumnName();

	public final ColumnDescriptor asColumnDescriptor(UniqueNamer collector) {
		return ColumnDescriptor.builder()
							   .label(collector.getUniqueName(this))
							   .defaultLabel(defaultColumnName())
							   .type(getType().typeInfo())
							   .semantics(getSemantics())
							   .description(getDescription())
							   .build();
	}

	/**
	 * Use default label schema which ignores user labels.
	 */
	public abstract String defaultColumnName();

	@ToString.Include
	public abstract ResultType getType();

	public Set<SemanticType> getSemantics() {
		return ImmutableSet.copyOf(semantics);
	}

	public abstract String getDescription();

	public final String printNullable(Object f) {
		if (f == null) {
			return "";
		}

		return print(f);
	}

	protected String print(Object f) {
		return getPrinter().print(f);
	}

	public abstract ResultPrinters.Printer getPrinter();
}
