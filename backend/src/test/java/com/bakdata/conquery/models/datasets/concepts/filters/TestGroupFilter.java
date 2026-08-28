package com.bakdata.conquery.models.datasets.concepts.filters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.QueryContextResolvable;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

@CPSType(id = "TEST_GROUP_FILTER", base = Filter.class)
@Getter
@Setter
@Slf4j
public class TestGroupFilter extends EventFilter<TestGroupFilter.GroupFilterValue> implements GroupFilter {

	@Valid
	@NotNull
	private ColumnId column;

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(getColumn());
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		final Column resolved = getColumn().resolve();
		final boolean acceptable = getAcceptedColumnTypes().contains(resolved.getType());

		if (!acceptable) {
			log.error(
				"Column[{}] is of Type[{}]. Not one of [{}]",
				resolved.getId(),
				resolved.getType(),
				getAcceptedColumnTypes());
		}

		return acceptable;
	}

	@JsonIgnore
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.allOf(MajorTypeId.class);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.GROUP);
		f.setFilters(getFEFilter());
	}


	@Override
	public EventFilterNode<?> createFilterNode(GroupFilterValue compoundFilterValue) {
		return new MultiSelectFilterNode(getColumn().resolve(), Set.of(compoundFilterValue.getResolvedValues()));
	}

	private Map<String, FrontendFilterConfiguration.Nested> getFEFilter() {
		// TODO there is not yet a mismatch check between FEFilter and GroupedValues
		return Map.of(
			GroupFilterValue.Fields.strings,
			FrontendFilterConfiguration.Nested.builder()
				.label("Elements")
				.type(
					FrontendFilterType.Fields.MULTI_SELECT)
				.build(),
			GroupFilterValue.Fields.repetitions,
			FrontendFilterConfiguration.Nested.builder()
				.label("Maximum Repetitions")
				.type(
					FrontendFilterType.Fields.INTEGER)
				.build()
		);
	}

	@Override
	public JavaType getFilterValueType(TypeFactory tf) {
		return tf.constructSimpleType(TestGroupFilter.GroupFilterValue.class, null);
	}

	@Getter
	@AllArgsConstructor
	@FieldNameConstants
	public static class GroupFilterValue implements QueryContextResolvable {
		@NotEmpty
		private String[] strings;

		@Min(1)
		private long repetitions;


		@JsonView(View.InternalCommunication.class)
		private String[] resolvedValues;

		@Override
		public void resolve(QueryResolveContext context) {
			ArrayList<String> values = new ArrayList<>();
			for (String string : strings) {
				LongStream.range(1, repetitions + 1)
					.mapToInt(Math::toIntExact)
					.mapToObj(
						string::repeat)
					.sequential()
					.collect(Collectors.toCollection(() -> values));
			}
			resolvedValues = values.toArray(String[]::new);
		}
	}
}
