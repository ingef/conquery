package com.bakdata.conquery.apiv1;

import static com.zigurs.karlis.utils.search.QuickSearch.MergePolicy.INTERSECTION;
import static com.zigurs.karlis.utils.search.QuickSearch.UnmatchedPolicy.IGNORE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.ConceptsUtils;
import com.zigurs.karlis.utils.search.QuickSearch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterSearch {

	private static Map<String, QuickSearch> search = new HashMap<>();

	public static void init(Collection<Dataset> datasets) {
		datasets
			.stream()
			.flatMap(ds -> ConceptsUtils.getAllConnectors(ds.getConcepts()).stream())
			.flatMap(co -> co.collectAllFilters().stream())
			.filter(f -> f instanceof AbstractSelectFilter && f.getTemplate() != null)
			.forEach(f -> createSourceSearch((AbstractSelectFilter) f));
	}

	private static void createSourceSearch(AbstractSelectFilter filter) {
		FilterTemplate template = filter.getTemplate();

		List<String> columns = template.getColumns();
		columns.add(template.getColumnValue());

		File file = new File(template.getFilePath());
		String key = String.join("_", columns) + "_" + file.getName();

		QuickSearch quick = search.get(key);
		if (quick != null) {
			log.info("Reference List '{}' is already exists ...", file.getAbsolutePath());
			filter.setSourceSearch(quick);
			return;
		}

		log.info("Processing Reference List '{}' ...", file.getAbsolutePath());
		long time = System.currentTimeMillis();

		quick = new QuickSearch.QuickSearchBuilder()
			.withUnmatchedPolicy(IGNORE)
			.withMergePolicy(INTERSECTION)
			.build();

		try {
			Iterator<String[]> it = CSVReader.readRaw(file).iterator();
			String[] header = it.next();

			for (String[] row : (Iterable<String[]>) (() -> it)) {
				FilterSearchItem item = new FilterSearchItem();

				for (int i = 0; i < header.length; i++) {
					String column = header[i];
					if (columns.contains(column)) {
						item.setLabel(template.getValue());
						item.setOptionValue(template.getOptionValue());
						item.getTemplateValues().put(column, row[i]);

						quick.addItem(item, row[i]);

						if (column.equals(template.getColumnValue())) {
							item.setValue(row[i]);
						}
					}
				}
			}

			filter.setSourceSearch(quick);
			search.put(key, quick);
			log.info("Processed Reference List '{}' in {} ms", file.getAbsolutePath(), System.currentTimeMillis() - time);
		} catch (IOException ex) {
			log.error(ex.getMessage());
		}
	}
}
