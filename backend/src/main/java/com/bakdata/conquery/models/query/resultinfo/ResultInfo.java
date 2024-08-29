package com.bakdata.conquery.models.query.resultinfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
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

	@ToString.Include
	private final Set<SemanticType> semantics = new HashSet<>();

	protected ResultInfo(Collection<SemanticType> semantics) {
		this.semantics.addAll(semantics);
	}

	public final void addSemantics(SemanticType... incoming) {
		semantics.addAll(Arrays.asList(incoming));
	}

	public abstract String userColumnName(PrintSettings printSettings);

	public final ColumnDescriptor asColumnDescriptor(UniqueNamer collector, PrintSettings printSettings) {
		return ColumnDescriptor.builder()
							   .label(collector.getUniqueName(this, printSettings))
							   .defaultLabel(defaultColumnName(printSettings))
							   .type(getType().typeInfo())
							   .semantics(getSemantics())
							   .description(getDescription())
							   .build();
	}

	/**
	 * Use default label schema which ignores user labels.
	 * @param printSettings
	 */
	public abstract String defaultColumnName(PrintSettings printSettings);

	@ToString.Include
	public abstract ResultType getType();

	public Set<SemanticType> getSemantics() {
		return ImmutableSet.copyOf(semantics);
	}

	public abstract String getDescription();

	public abstract Printer createPrinter(PrintSettings printSettings);
}
