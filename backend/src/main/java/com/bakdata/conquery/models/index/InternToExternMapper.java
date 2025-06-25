package com.bakdata.conquery.models.index;

import java.util.Collection;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.OneToManyMappingPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.OneToOneMappingPrinter;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import lombok.Setter;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public abstract class InternToExternMapper extends NamespacedIdentifiable<InternToExternMapperId> {

	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	@JacksonInject(useInput = OptBoolean.TRUE)
	private DatasetId dataset;

	@Override
	public InternToExternMapperId createId() {
		return new InternToExternMapperId(getDataset(), getName());
	}

	public abstract boolean initialized();

	public abstract void init();

	public abstract String external(String key);

	public abstract Collection<String> externalMultiple(String key);

	public Printer<String> createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if (isAllowMultiple()) {
			return new OneToManyMappingPrinter(this).andThen(printerFactory.getListPrinter(printerFactory.getStringPrinter(printSettings), printSettings));
		}

		return new OneToOneMappingPrinter(this, printerFactory.getStringPrinter(printSettings));
	}

	public abstract boolean isAllowMultiple();
}
