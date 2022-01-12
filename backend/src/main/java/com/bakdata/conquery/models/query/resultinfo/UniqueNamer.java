package com.bakdata.conquery.models.query.resultinfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class to generate unique column names from {@link ResultInfo}s for {@link com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider}
 * and their renderer.
 *
 * @implNote Use a {@link ResultInfo} only once per instance, since it tracks only the generated name from an info not the seen infos themselves.
 */
@RequiredArgsConstructor
@Slf4j
public class UniqueNamer {

	private final PrintSettings settings;

	// TODO Take a copy of this
	private final List<ResultInfo> allInfos;

	private Map<ResultInfo,String> uniqueLabels;

	/**
	 * Is used to track possible name duplicates for column names and provide an index to enumerate these.
	 * This lowers the risk of duplicate column names in the result.
	 */
	@Getter
	private final Multimap<String, ResultInfo> ocurrenceCounter = ArrayListMultimap.create();

	private Map<ResultInfo,String> generateUniqueNames() {
		final Map<ResultInfo,String> uniqueLabels = new HashMap<>();
		final Multimap<String, ResultInfo> occurrenceMap = ArrayListMultimap.create();

		// Init occurenceMap
		for (ResultInfo info : allInfos) {
			@NonNull String label = Objects.requireNonNullElse(info.userColumnName(settings), info.defaultColumnName(settings));
			uniqueLabels.put(info, label);
			occurrenceMap.put(label, info);
		}

		boolean namesCanEscalate = true;
		// First Stage: try to remove duplicates by escalading the label of selects
		while (namesCanEscalate) {
			namesCanEscalate = false;

			// Find duplicates
			final Multiset<String> keys = occurrenceMap.keys();
			final List<String> duplicateLabels = keys.stream().filter(elem -> keys.count(elem) > 1).collect(Collectors.toList());

			final List<SelectResultInfo> selectDupes = duplicateLabels.stream()
																	  .flatMap(l -> occurrenceMap.get(l).stream())
																	  .distinct()
																	  .filter(SelectResultInfo.class::isInstance)
																	  .map(SelectResultInfo.class::cast)
																	  .collect(Collectors.toList());

			for (SelectResultInfo selectDupe : selectDupes) {
				final boolean escalated = selectDupe.escalateNameMode();
				if (!escalated) {
					// The label is maxed out
					continue;
				}

				// Flag escalation, so a new duplication check is triggered
				namesCanEscalate = true;

				// Remove old duplicate mapping
				occurrenceMap.remove(uniqueLabels.get(selectDupe), selectDupe);

				// Generate escalated column name
				String label = Objects.requireNonNullElse(selectDupe.userColumnName(settings), selectDupe.defaultColumnName(settings));

				// Add new mapping
				occurrenceMap.put(label, selectDupe);
				uniqueLabels.put(selectDupe, label);
			}
		}

		// Second Stage: All remaining duplicates get a number appended
		final Multiset<String> occurrenceCounter = ConcurrentHashMultiset.create();
		for (ResultInfo info : allInfos) {
			String label = uniqueLabels.get(info);
			String uniqueName = label;
			ocurrenceCounter.put(uniqueName, info);
			int postfix = occurrenceCounter.add(uniqueName, 1);
			do {

				if (postfix > 0) {
					uniqueName = label + "_" + postfix;
					postfix = occurrenceCounter.add(uniqueName, 1);
					uniqueLabels.put(info, uniqueName);
				}

			} while (occurrenceCounter.count(uniqueName) > 1);
		}


		return uniqueLabels;
	}


	@NonNull
	@JsonIgnore
	public final String getUniqueName(ResultInfo info) {

		if (uniqueLabels == null) {
			uniqueLabels = generateUniqueNames();
		}

		String label = uniqueLabels.get(info);
		if (label == null) {
			// This should not happen, since all result infos must be provided upon creation of this object
			log.warn("Cannot create unique label from unknown result info {}. Defaulting to verbose name", info);
			if (info instanceof SelectResultInfo) {
				SelectResultInfo selectInfo = (SelectResultInfo) info;

				while(selectInfo.escalateNameMode()) {};

				label = Objects.requireNonNullElse(selectInfo.userColumnName(settings), selectInfo.defaultColumnName(settings));
			}
		}
		return label;
	}
}