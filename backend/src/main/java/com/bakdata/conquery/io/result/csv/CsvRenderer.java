package com.bakdata.conquery.io.result.csv;

import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import kotlin.jvm.functions.Function2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class CsvRenderer {

	private final CsvWriter writer;
	private final PrintSettings cfg;

	/**
	 * "Printer-Mapping" that combine a potentially nested {@link ResultType}
	 * with an element valueMapper (see {@link com.bakdata.conquery.models.datasets.concepts.select.Select#toExternalRepresentation} and {@link SelectResultInfo#getValueMapper()}.
	 */
	private final Map<Class<? extends ResultType>, Function2<ResultType, Function<Object, Object>, Function<Object, String>>> FIELD_MAP = Map.of(
			ResultType.ListT.class, this::createListPrinter,
			ResultType.StringT.class, this::createStringPrinter
	);


	public void toCSV(List<ResultInfo> idHeaders, List<ResultInfo> infos, Stream<EntityResult> resultStream) {

		UniqueNamer uniqNamer = new UniqueNamer(cfg);
		final String[] headers = Stream.concat(idHeaders.stream(), infos.stream()).map(uniqNamer::getUniqueName).toArray(String[]::new);

		writer.writeHeaders(headers);

		createCSVBody(cfg, infos, resultStream);
	}

	private void createCSVBody(PrintSettings cfg, List<ResultInfo> infos, Stream<EntityResult> results) {
		final List<Function<Object, String>> printers = infos.stream().map(info -> {
			final Function2<ResultType, Function<Object, Object>, Function<Object, String>>
					printerCreator =
					FIELD_MAP.getOrDefault(info.getType().getClass(), this::createDefaultPrinter);
			final Function<Object, Object> valueMapper = info.getValueMapper().orElse(Function.identity());

			return printerCreator.invoke(info.getType(), valueMapper);
		}).collect(Collectors.toList());

		results
				.map(result -> Pair.of(cfg.getIdMapper().map(result), result))
				.sorted(Map.Entry.comparingByKey())
				.forEach(res -> res
						.getValue()
						.streamValues()
						.forEach(result -> printLine(cfg, res.getKey(), printers, result)));
	}


	public void printLine(PrintSettings cfg, EntityPrintId entity, List<Function<Object, String>> printers, Object[] value) {
		// Cast here to Object[] so it is clear to intellij that the varargs call is intended
		writer.addValues((Object[]) entity.getExternalId());
		try {
			for (int i = 0; i < printers.size(); i++) {
				final Function<Object, String> printer = printers.get(i);
				final String printValue = printer.apply(value[i]);
				writer.addValue(printValue);
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to print line " + Arrays.deepToString(value), e);
		}

		writer.writeValuesToRow();
	}

	private Function<Object, String> createListPrinter(ResultType type, Function<Object, Object> mapper) {
		final ResultType elementType = ((ResultType.ListT) type).getElementType();
		final Function2<ResultType, Function<Object, Object>, Function<Object, String>>
				elementPrinterCreator =
				FIELD_MAP.getOrDefault(elementType.getClass(), this::createDefaultPrinter);

		final Function<Object, String> elementPrinter = elementPrinterCreator.invoke(elementType, mapper);

		final LocaleConfig.ListFormat listFormat = cfg.getListFormat();

		return (values) -> {
			if (values == null) {
				return type.printNullable(cfg, null);
			}

			// Jackson deserializes collections as lists instead of an array, if the type is not given
			if (!(values instanceof List)) {
				throw new IllegalStateException(String.format("Expected a List got %s (Type: %s, as string: %s)", values, values.getClass().getName(), values));
			}
			// Not sure if this escaping is enough
			StringJoiner joiner = listFormat.createListJoiner();
			for (Object obj : (List<?>) values) {
				final String printValue = elementPrinter.apply(obj);
				joiner.add(listFormat.escapeListElement(printValue));
			}
			return joiner.toString();
		};
	}

	private Function<Object, String> createStringPrinter(ResultType resultType, Function<Object, Object> mapper) {
		return (o) -> resultType.printNullable(cfg, mapper.apply(o));
	}


	private Function<Object, String> createDefaultPrinter(ResultType resultType, Function<Object, Object> mapper) {
		return (o) -> resultType.printNullable(cfg, o);
	}
}
