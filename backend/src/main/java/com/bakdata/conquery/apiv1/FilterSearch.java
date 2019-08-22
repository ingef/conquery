package com.bakdata.conquery.apiv1;

import static com.zigurs.karlis.utils.search.QuickSearch.MergePolicy.INTERSECTION;
import static com.zigurs.karlis.utils.search.QuickSearch.UnmatchedPolicy.IGNORE;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.Namespaces;
import com.zigurs.karlis.utils.search.QuickSearch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterSearch {

	private static Map<String, QuickSearch<FilterSearchItem>> search = new HashMap<>();

	public static ExecutorService init(Namespaces namespaces, Collection<Dataset> datasets) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		datasets
			.stream()
			.flatMap(ds -> namespaces.get(ds.getId()).getStorage().getAllConcepts().stream())
			.flatMap(c -> c.getConnectors().stream())
			.flatMap(co -> co.collectAllFilters().stream())
			.filter(f -> f instanceof AbstractSelectFilter && f.getTemplate() != null)
			.forEach(f -> executor.submit(()->createSourceSearch((AbstractSelectFilter<?>) f)));
		executor.shutdown();
		return executor;
	}

	private static void createSourceSearch(AbstractSelectFilter<?> filter) {
		FilterTemplate template = filter.getTemplate();

		List<String> columns = template.getColumns();
		columns.add(template.getColumnValue());

		File file = new File(template.getFilePath());
		String key = String.join("_", columns) + "_" + file.getName();

		QuickSearch<FilterSearchItem> quick = search.get(key);
		if (quick != null) {
			log.info("Reference list '{}' already exists ...", file.getAbsolutePath());
			filter.setSourceSearch(quick);
			return;
		}

		log.info("Processing reference list '{}' ...", file.getAbsolutePath());
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
			log.info("Processed reference list '{}' in {} ms", file.getAbsolutePath(), System.currentTimeMillis() - time);
		} catch (Exception e) {
			log.error("Failed to process reference list '"+file.getAbsolutePath()+"'", e);
		}
	}
}
