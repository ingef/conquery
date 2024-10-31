package com.bakdata.conquery.models.index;

import java.util.Collection;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.printers.Printer;
import com.bakdata.conquery.models.query.resultinfo.printers.PrinterFactory;
import com.bakdata.conquery.models.query.resultinfo.printers.common.MappedMultiPrinter;
import com.bakdata.conquery.models.query.resultinfo.printers.common.MappedPrinter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public interface InternToExternMapper extends NamespacedIdentifiable<InternToExternMapperId>, Named<InternToExternMapperId> {

	boolean initialized();

	void init();

	String external(String key);

	Collection<String> externalMultiple(String key);

	@Override
	InternToExternMapperId getId();

	default Printer<String> createPrinter(PrinterFactory printerFactory, PrintSettings printSettings) {
		if (isAllowMultiple()) {
			return new MappedMultiPrinter(this)
					.andThen(printerFactory.getListPrinter(printerFactory.getStringPrinter(printSettings), printSettings));
		}

		return new MappedPrinter(this)
				.andThen(printerFactory.getStringPrinter(printSettings));
	}

	boolean isAllowMultiple();
}
