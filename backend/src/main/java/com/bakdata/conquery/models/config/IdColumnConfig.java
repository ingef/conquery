package com.bakdata.conquery.models.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.apiv1.query.concept.specific.external.DateFormat;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.query.resultinfo.LocalizedDefaultResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Functions;
import io.dropwizard.validation.ValidationMethod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

/**
 * Class configuring {@link EntityIdMap} and {@link com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal} resolving.
 */
@Getter
@Setter
@With
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class IdColumnConfig {

	/**
	 * List of resolvable and printable ids.
	 *
	 * @apiNote Sort order determines output order of ids with where {@link ColumnConfig#isPrint()} is true in result.
	 */
	@NotEmpty
	@Valid
	private List<ColumnConfig> ids = List.of(
			ColumnConfig.builder()
						.name("ID")
						.field("result")
						.resolvable(true)
						.fillAnon(true)
						.print(true)
						.build()
	);


	@JsonIgnore
	@Setter(AccessLevel.NONE)
	@Getter(lazy = true, value = AccessLevel.PUBLIC)
	private final Map<String, ColumnConfig> idMappers = ids.stream().filter(ColumnConfig::isResolvable)
														   .collect(Collectors.toMap(ColumnConfig::getName, Functions.identity()));


	@ValidationMethod(message = "Duplicate Claims for Mapping Columns.")
	@JsonIgnore
	public boolean isAllColsUnique() {
		Set<String> dupes = new HashSet<>();

		final List<String> candidates = new ArrayList<>();

		ids.stream().map(ColumnConfig::getName).forEach(candidates::add);
		Arrays.stream(DateFormat.values()).map(DateFormat::name).forEach(candidates::add);

		for (String name : candidates) {
			if (dupes.add(name)) {
				continue;
			}

			log.error("Duplicate claims for Name = `{}`", name);
			return false;
		}

		return true;
	}

	@ValidationMethod(message = "Not all Id-Columns have mappings.")
	@JsonIgnore
	public boolean isIdColsHaveMapping() {
		return ids.stream().map(ColumnConfig::getField).allMatch(Objects::nonNull);
	}

	@ValidationMethod(message = "Must have exactly one Column for Pseudomization.")
	@JsonIgnore
	public boolean isExactlyOnePseudo() {
		return ids.stream()
				  .filter(conf -> conf.getField() != null)
				  .filter(ColumnConfig::isFillAnon)
				  .count() == 1;
	}


	/**
	 * Localized header for output CSV.
	 *
	 * @return
	 */
	@JsonIgnore
	public List<ResultInfo> getIdResultInfos() {
		return ids.stream()
				  .filter(ColumnConfig::isPrint)
				  .map(col -> new LocalizedDefaultResultInfo(
						  locale -> col.getLabel().getOrDefault(locale.getLanguage(), col.getField()),
						  locale -> col.getField(),
						  ResultType.StringT.getINSTANCE(),
						  Set.of(new SemanticType.IdT(col.getName()))
				  ))
				  .collect(Collectors.toUnmodifiableList());
	}


}
