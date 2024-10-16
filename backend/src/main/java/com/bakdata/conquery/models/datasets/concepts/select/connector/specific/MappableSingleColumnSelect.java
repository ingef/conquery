package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import jakarta.validation.Valid;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.MappedPrinter;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Getter;

@Getter
public abstract class MappableSingleColumnSelect extends SingleColumnSelect {

	/**
	 * If a mapping was provided the mapping changes the aggregator result before it is processed by a {@link com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider}.
	 */
	@Valid
	@Nullable
	@View.ApiManagerPersistence
	private final InternToExternMapperId mapping;


	public MappableSingleColumnSelect(ColumnId column, @Nullable InternToExternMapperId mapping) {
		super(column);
		this.mapping = mapping;
	}

	@Override
	public Printer<?> createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if (mapping == null) {
			return super.createPrinter(printerFactory, printSettings);
		}

		return new MappedPrinter(mapping.resolve());
	}

	@Override
	public ResultType getResultType() {
		if(mapping == null){
			return ResultType.resolveResultType(getColumn().resolve().getType());
		}
		return ResultType.Primitive.STRING;
	}

	@Override
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {

		if (!isCategorical()) {
			return new SelectResultInfo(this, cqConcept, Collections.emptySet());
		}

		return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.CategoricalT()));
	}

	public void loadMapping() {
		if (mapping != null) {
			mapping.resolve().init();
		}
	}
}
