package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import static com.bakdata.conquery.models.types.ResultType.Primitive.STRING;
import static com.bakdata.conquery.models.types.ResultType.resolveResultType;
import static org.jooq.impl.DSL.*;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import jakarta.validation.Valid;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SelectContext;
import com.bakdata.conquery.sql.conversion.model.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import org.jooq.Field;

@Getter
public abstract class MappableSingleColumnSelect extends SingleColumnSelect {

	/**
	 * If a mapping was provided the mapping changes the aggregator result before it is processed by a {@link ResultRendererProvider}.
	 */
	@Valid
	@Nullable
	@View.ApiManagerPersistence
	private final InternToExternMapperId mapping;

	@Nullable
	@Valid
	private final Range.IntegerRange substringRange;


	public MappableSingleColumnSelect(ColumnId column, @Nullable InternToExternMapperId mapping, @Nullable Range.IntegerRange substringRange) {
		super(column);
		this.mapping = mapping;
		this.substringRange = substringRange;
	}

	public static SingleColumnSqlSelect getSubstringSelect(
			Column column, Range.IntegerRange substringRange, SelectContext<ConnectorSqlTables> selectContext,
			String alias) {
		Field<String> field;

		if (substringRange == null || substringRange.isAll()) {
			field = field(name(selectContext.getTables().getRootTable(), column.getName()), String.class);
		}
		else {
			field = field(name(selectContext.getTables().getRootTable(), column.getName()), String.class);
			if (substringRange.isAtLeast()) {
				field = substring(field, 1 + substringRange.getMin());
			}
			else if (substringRange.isAtMost()) {
				field = substring(field, 1, substringRange.getMax());
			}
			else {
				field = substring(field, 1 + substringRange.getMin(), substringRange.getMax() - substringRange.getMin());
			}
		}

		if (alias != null) {
			field = field.as(alias);
		}

		return new FieldWrapper<>(field, column.getName());
	}

	@Override
	public Printer<?> createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if (mapping == null) {
			return super.createPrinter(printerFactory, printSettings);
		}

		final InternToExternMapper resolvedMapping = mapping.resolve();

		return resolvedMapping.createPrinter(printerFactory, printSettings);
	}

	@Override
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {

		if (isCategorical()) {
			return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.CategoricalT()));
		}
		return new SelectResultInfo(this, cqConcept, Collections.emptySet());

	}

	@Override
	public ResultType getResultType() {
		if (mapping == null) {
			return resolveResultType(getColumn().resolve().getType());
		}

		InternToExternMapper resolved = mapping.resolve();

		if (resolved.isAllowMultiple()) {
			return new ResultType.ListT<>(STRING);
		}

		return STRING;
	}

	public void loadMapping() {
		if (mapping != null) {
			mapping.resolve().init();
		}
	}

	@JsonIgnore
	@ValidationMethod(message = "Selects using Substrings must be based on STRING columns.")
	public boolean isStringIfSubstring() {
		if (getSubstringRange() == null) {
			return true;
		}

		return getColumn().resolve().getType().equals(MajorTypeId.STRING);
	}

	@JsonIgnore
	@ValidationMethod(message = "Selects using Mappings must be based on STRING columns.")
	public boolean isStringIfMapping() {
		if (getMapping() == null) {
			return true;
		}

		return getColumn().resolve().getType().equals(MajorTypeId.STRING);
	}

	@JsonIgnore
	@ValidationMethod(message = "Substrings must start at 0.")
	public boolean isMinPositive() {
		if (getSubstringRange() == null) {
			return true;
		}
		if (getSubstringRange().getMin() == null) {
			return true;
		}

		return getSubstringRange().getMin() >= 0;
	}

	@Override
	public ResultSetProcessor.Reader<?> createResultSetReader(ResultSetProcessor processor) {
		if (mapping != null) {
			return processor::getString;
		}

		return ResultSetProcessor.readerForType(getResultType(), processor);
	}

}
