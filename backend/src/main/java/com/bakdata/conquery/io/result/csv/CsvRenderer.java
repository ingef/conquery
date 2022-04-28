package com.bakdata.conquery.io.result.csv;

import com.bakdata.conquery.io.result.arrow.ArrowUtil;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.univocity.parsers.csv.CsvWriter;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class CsvRenderer {

	private final CsvWriter writer;
	private final PrintSettings cfg;

	private final Map<Class<? extends ResultType>, Function3<ResultType, Object, Function<Object, String>, String>> FIELD_MAP = Map.of(
			ResultType.ListT.class, this::printList,
			ResultType.StringT.class, this::printString
	);


	public void toCSV(List<ResultInfo> idHeaders, List<ResultInfo> infos, Stream<EntityResult> resultStream) {

		UniqueNamer uniqNamer = new UniqueNamer(cfg);
		final String[] headers = Stream.concat(idHeaders.stream(), infos.stream()).map(uniqNamer::getUniqueName).toArray(String[]::new);

		writer.writeHeaders(headers);

		createCSVBody(cfg, infos, resultStream);
	}

	private void createCSVBody(PrintSettings cfg, List<ResultInfo> infos, Stream<EntityResult> results) {
		results
				.map(result -> Pair.of(cfg.getIdMapper().map(result), result))
				.sorted(Map.Entry.comparingByKey())
				.forEach(res -> res
								.getValue()
								.streamValues()
								.forEach(result -> printLine(cfg, res.getKey(), infos, result)));
	}


	public void printLine(PrintSettings cfg, EntityPrintId entity, List<ResultInfo> infos, Object[] value) {
		// Cast here to Object[] so it is clear to intellij that the varargs call is intended
		writer.addValues((Object[]) entity.getExternalId());
		try {
			for (int i = 0; i < infos.size(); i++) {
				final ResultInfo info = infos.get(i);
				if (info instanceof SelectResultInfo) {
					SelectResultInfo selectResultInfo = (SelectResultInfo) info;

					final String
							printValue =
							FIELD_MAP.getOrDefault(selectResultInfo.getType().getClass(), this::printDefault)
									 .invoke(info.getType(), value[i], selectResultInfo.getValueMapper().orElse(null));
					writer.addValue(printValue);
					continue;
				}
				writer.addValue(info.getType().printNullable(cfg, value[i]));
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to print line " + Arrays.deepToString(value) + " with result infos " + infos, e);
		}

		writer.writeValuesToRow();
	}

	private String printList(ResultType type, Object values, Function<Object, String> mapper) {
		if (values == null) {
			return type.printNullable(cfg, null);
		}

		// Jackson deserializes collections as lists instead of an array, if the type is not given
		if (!(values instanceof List)) {
			throw new IllegalStateException(String.format("Expected a List got %s (Type: %s, as string: %s)", values, values.getClass().getName(), values));
		}
		// Not sure if this escaping is enough
		String listDelimEscape = cfg.getListElementEscaper() + cfg.getListFormat().getSeparator();
		StringJoiner joiner = new StringJoiner(cfg.getListFormat().getSeparator(), cfg.getListFormat().getStart(), cfg.getListFormat().getEnd());
		for (Object obj : (List<?>) values) {
			final ResultType elementType = ((ResultType.ListT) type).getElementType();
			joiner.add(FIELD_MAP.getOrDefault(elementType.getClass(), this::printDefault).invoke(elementType, obj, mapper));
		}
		return joiner.toString();
	}

	private String printString(ResultType resultType, Object o, Function<Object, String> mapper) {
		if (mapper != null) {
			return mapper.apply(o);
		}
		return resultType.printNullable(cfg, o);
	}


	private String printDefault(ResultType resultType, Object o, Function<Object, String> mapper) {
		return resultType.printNullable(cfg, o);
	}
}
