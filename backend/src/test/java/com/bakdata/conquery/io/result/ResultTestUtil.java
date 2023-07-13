package com.bakdata.conquery.io.result;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class ResultTestUtil {


	@NotNull
	public static List<ResultType> getResultTypes() {
		return List.of(
				ResultType.BooleanT.INSTANCE,
				ResultType.IntegerT.INSTANCE,
				ResultType.NumericT.INSTANCE,
				ResultType.DateT.INSTANCE,
				ResultType.DateRangeT.INSTANCE,
				ResultType.StringT.INSTANCE,
				ResultType.MoneyT.INSTANCE,
				new ResultType.ListT(ResultType.BooleanT.INSTANCE),
				new ResultType.ListT(ResultType.DateRangeT.INSTANCE),
				new ResultType.ListT(ResultType.StringT.INSTANCE)
		);
	}


	public static List<ResultInfo> ID_FIELDS = Stream.of("id1", "id2")
													 .map(n  -> new SimpleResultInfo(n, ResultType.StringT.getINSTANCE(), "", Set.of(new SemanticType.IdT("ID")))).collect(Collectors.toList());

	@NotNull
	public static List<EntityResult> getTestEntityResults() {
		return List.of(
				new SinglelineEntityResult("1", new Object[]{Boolean.TRUE, 2345634, 123423.34, 5646, List.of(345, 534), "test_string", 4521, List.of(true, false), List.of(List.of(345, 534), List.of(1, 2)), List.of("fizz", "buzz")}),
				new SinglelineEntityResult("2", new Object[]{Boolean.FALSE, null, null, null,  null, null, null, List.of(), List.of(List.of(1234, Integer.MAX_VALUE)), List.of()}),
				new SinglelineEntityResult("2", new Object[]{Boolean.TRUE, null, null, null,  null, null, null, List.of(false, false), null, null}),
				new MultilineEntityResult("3", List.of(
						new Object[]{Boolean.FALSE, null,null, null, null, null,  null, List.of(false), null, null},
						new Object[]{Boolean.TRUE, null, null, null, null,  null, null, null, null, null},
						new Object[]{Boolean.TRUE, null, null, null,  null, null, 4, List.of(true, false, true, false), null, null}
				)));
	}



	@NotNull
	public static ManagedQuery getTestQuery() {
		return new ManagedQuery(null, null, null, null) {
			@Override
			public List<ResultInfo> getResultInfos() {
				return getResultTypes().stream()
									   .map(ResultTestUtil.TypedSelectDummy::new)
									   .map(select -> new SelectResultInfo(select, new CQConcept()))
									   .collect(Collectors.toList());
			}

			@Override
			public Stream<EntityResult> streamResults() {
				return getTestEntityResults().stream();
			}
		};
	}

	public static class TypedSelectDummy extends Select {

		private final ResultType resultType;

		public TypedSelectDummy(ResultType resultType) {
			this.setLabel(resultType.toString());
			this.resultType = resultType;
		}

		@Nullable
		@Override
		public List<Column> getRequiredColumns() {
			return Collections.emptyList();
		}

		@Override
		public Aggregator<String> createAggregator() {
			return new Aggregator<>() {

				@Override
				public void init(Entity entity, QueryExecutionContext context) {

				}

				@Override
				public void acceptEvent(Bucket bucket, int event) {
					throw new UnsupportedOperationException();
				}

				@Override
				public String createAggregationResult() {
					throw new UnsupportedOperationException();
				}

				@Override
				public ResultType getResultType() {
					return resultType;
				}

			};
		}

	}
}
