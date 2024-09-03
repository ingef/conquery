package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.MappedPrinter;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import jakarta.validation.Valid;
import lombok.Getter;

public abstract class MappableSingleColumnSelect extends SingleColumnSelect {

	/**
	 * If a mapping was provided the mapping changes the aggregator result before it is processed by a {@link com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider}.
	 */
	@Getter
	@Valid
	@Nullable
	@View.ApiManagerPersistence
	@NsIdRef
	private final InternToExternMapper mapping;


	public MappableSingleColumnSelect(Column column, @Nullable InternToExternMapper mapping) {
		super(column);
		this.mapping = mapping;
	}

	public List<String> print(List<String> values) {
		if(mapping == null){
			return values;
		}

		final List<String> out = new ArrayList<>();

		for (String value : values) {
			out.addAll(List.of(mapping.external(value)));
		}

		return out;
	}

	@Override
	public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if (mapping == null) {
			return super.createPrinter(printerFactory, printSettings);
		}

		return new MappedPrinter(getMapping());
	}

	@Override
	public SelectResultInfo getResultInfo(CQConcept cqConcept) {

		if (!isCategorical()) {
			return new SelectResultInfo(this, cqConcept, Collections.emptySet());
		}

		return new SelectResultInfo(this, cqConcept, Set.of(new SemanticType.CategoricalT()));
	}

	@Override
	public ResultType getResultType() {
		if(mapping == null){
			return ResultType.resolveResultType(getColumn().getType());
		}
		return ResultType.Primitive.STRING;
	}

	public void loadMapping() {
		if (mapping != null) {
			mapping.init();
		}
	}
}
