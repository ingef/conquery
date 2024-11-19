package com.bakdata.conquery.models.query.resultinfo;

import java.util.Set;

import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SecondaryIdResultInfo extends ResultInfo {
	private final SecondaryIdDescription secondaryId;
	private final ResultType type;


	public SecondaryIdResultInfo(SecondaryIdDescription secondaryId) {
		super(Set.of(new SemanticType.SecondaryIdT(secondaryId.getId())));
		this.secondaryId = secondaryId;
		type = ResultType.Primitive.STRING;
	}

	@Override
	public String getDescription() {
		return secondaryId.getDescription();
	}

	@Override
	public Printer<?> createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if (secondaryId.getMapping() == null) {
			return printerFactory.getStringPrinter(printSettings);
		}

		return secondaryId.getMapping().resolve().createPrinter(printerFactory, printSettings);
	}

	@Override
	public String userColumnName(PrintSettings printSettings) {
		return secondaryId.getLabel();
	}

	@Override
	public String defaultColumnName(PrintSettings printSettings) {
		return userColumnName(printSettings);
	}
}
