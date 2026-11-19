package com.bakdata.conquery.sql.conversion.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.model.result.ResultColumn;
import com.bakdata.conquery.sql.translate.ColumnRole;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SqlQuery {

	private final String sql;
	private final List<ResultInfo> resultInfos;

	public static SqlQuery fromCompiled(TranslatedQuery translatedQuery, List<ResultInfo> resultInfos) {
		return new SqlQuery(translatedQuery.sql(), List.copyOf(resultInfos));
	}

	public static SqlQuery fromCompiled(
			TranslatedQuery translatedQuery,
			List<ResultColumn> requestedColumns,
			List<ResultInfo> resultInfos
	) {
		if (requestedColumns.size() != resultInfos.size()) {
			throw new IllegalStateException("Resolved result columns and backend result descriptors differ in size");
		}
		final Map<String, ResultInfo> resultInfoByOutputId = new LinkedHashMap<>();
		for (int index = 0; index < requestedColumns.size(); index++) {
			resultInfoByOutputId.put(requestedColumns.get(index).outputId(), resultInfos.get(index));
		}
		final List<ResultInfo> ordered = translatedQuery.columns().stream()
				.filter(column -> column.role() == ColumnRole.RESULT)
				.map(column -> {
					final ResultInfo resultInfo = resultInfoByOutputId.get(column.outputId());
					if (resultInfo == null) {
						throw new IllegalStateException(
								"Translator returned unknown result output ID " + column.outputId());
					}
					return resultInfo;
				})
				.toList();
		if (ordered.size() != resultInfos.size()) {
			throw new IllegalStateException(
					"Translator returned %d correlated result columns for %d backend result descriptors"
							.formatted(ordered.size(), resultInfos.size()));
		}
		return new SqlQuery(translatedQuery.sql(), ordered);
	}

}
