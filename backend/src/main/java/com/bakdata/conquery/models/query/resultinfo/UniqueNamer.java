package com.bakdata.conquery.models.query.resultinfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
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
	
	/**
	 * Is used to track possible name duplicates for column names and provide an index to enumerate these.
	 * This lowers the risk of duplicate column names in the result.
	 */
	@Getter
	private final Multiset<String> ocurrenceCounter = ConcurrentHashMultiset.create();


	@NonNull
	@JsonIgnore
	public final String getUniqueName(ResultInfo info) {
		@NonNull String label = Objects.requireNonNullElse(info.userColumnName(settings), info.defaultColumnName(settings));
		// lookup if prefix is needed and computed it if necessary
		String uniqueName = label;
		synchronized (ocurrenceCounter) {
			int postfix = ocurrenceCounter.add(uniqueName, 1);
			do {

				if (postfix > 0) {
					uniqueName = label + "_" + postfix;
					postfix = ocurrenceCounter.add(uniqueName, 1);
				}

			} while (ocurrenceCounter.count(uniqueName) > 1);
		}
		return uniqueName;
	}
}