package com.bakdata.conquery.models.query.resultinfo;

import java.util.Collections;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.ConceptIdPrinter;
import com.bakdata.conquery.models.types.ResultType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ColumnResultInfo extends ResultInfo {

	private final Column column;
	private final ResultType type;
	private final String description;
	private final Concept<?> concept;


	public ColumnResultInfo(Column column, ResultType type, String description, Concept<?> concept) {
		super(Collections.emptySet());
		this.column = column;
		this.type = type;
		this.description = description;
		this.concept = concept;
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return column.getTable().getLabel() + " " + column.getLabel();
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return userColumnName(printSettings);
	}

	@Override
	public Printer createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if(concept != null){
			return new ConceptIdPrinter(concept, printSettings);
		}
		return printerFactory.printerFor(type, printSettings);
	}

}
