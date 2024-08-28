package com.bakdata.conquery.models.query.resultinfo.printers;

import java.util.Set;

import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SecondaryIdResultInfo extends ResultInfo {
	private final SecondaryIdDescription secondaryId;
	private final ResultType type;
	private final Printer printer;


	public SecondaryIdResultInfo(SecondaryIdDescription secondaryId, PrintSettings settings) {
		super(Set.of(new SemanticType.SecondaryIdT(secondaryId)), settings);
		this.secondaryId = secondaryId;
		type = ResultType.Primitive.STRING;

		if (secondaryId.getMapping() == null) {
			printer = settings.getPrinterFactory().getStringPrinter(settings);
		}
		else {
			printer = new ChainingPrinter(new MappedPrinter(secondaryId.getMapping()), settings.getPrinterFactory().getStringPrinter(settings));
		}
	}

	@Override
	public String getDescription() {
		return secondaryId.getDescription();
	}

	@Override
	public String userColumnName() {
		return secondaryId.getLabel();
	}

	@Override
	public String defaultColumnName() {
		return userColumnName();
	}
}
