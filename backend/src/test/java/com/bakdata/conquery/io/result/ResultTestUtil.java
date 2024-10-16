package com.bakdata.conquery.io.result;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.resultinfo.ExternalResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class ResultTestUtil {

	public static final DatasetId DATASET = new DatasetId("test_dataset");
	private static final TreeConcept CONCEPT;

	static {
		CONCEPT = new TreeConcept();
		CONCEPT.setName("concept");
		CONCEPT.setDataset(DATASET);
	}

	public static List<ResultInfo> getIdFields() {
		return Stream.of("id1", "id2").map(name -> {
			ExternalResultInfo info = new ExternalResultInfo(name, ResultType.Primitive.STRING);
			info.addSemantics(new SemanticType.IdT("ID"));
			return info;
		}).collect(Collectors.toList());
	}

	@NotNull
	public static ManagedQuery getTestQuery() {
		return new ManagedQuery(null, new UserId("test_user"), DATASET, null, null) {
			@Override
			public List<ResultInfo> getResultInfos() {
				return getResultTypes().stream()
									   .map(TypedSelectDummy::new)
									   .map(select -> new SelectResultInfo(select, new CQConcept(), Collections.emptySet()))
									   .collect(Collectors.toList());
			}

			@Override
			public Stream<EntityResult> streamResults(OptionalLong maybeLimit) {
				return getTestEntityResults().stream();
			}
		};
	}

	@NotNull
	public static List<ResultType> getResultTypes() {
		return List.of(ResultType.Primitive.BOOLEAN,
					   ResultType.Primitive.INTEGER,
					   ResultType.Primitive.NUMERIC,
					   ResultType.Primitive.DATE,
					   ResultType.Primitive.DATE_RANGE,
					   ResultType.Primitive.STRING,
					   ResultType.Primitive.MONEY,
					   new ResultType.ListT(ResultType.Primitive.BOOLEAN),
					   new ResultType.ListT(ResultType.Primitive.DATE_RANGE),
					   new ResultType.ListT(ResultType.Primitive.STRING)
		);
	}

	@NotNull
	public static List<EntityResult> getTestEntityResults() {
		return List.of(new SinglelineEntityResult("1", new Object[]{
							   Boolean.TRUE,
							   2345634,
							   123423.34,
							   5646,
							   List.of(345, 534),
							   "test_string",
							   new BigDecimal("45.21"),
							   List.of(true, false),
							   List.of(List.of(345, 534), List.of(1, 2)),
							   List.of("fizz", "buzz")
					   }),
					   new SinglelineEntityResult("2", new Object[]{
							   Boolean.FALSE, null, null, null, null, null, null, List.of(), List.of(List.of(1234, Integer.MAX_VALUE)), List.of()
					   }),
					   new SinglelineEntityResult("2", new Object[]{Boolean.TRUE, null, null, null, null, null, null, List.of(false, false), null, null}),
					   new MultilineEntityResult("3",
												 List.of(new Object[]{Boolean.FALSE, null, null, null, null, null, null, List.of(false), null, null},
														 new Object[]{Boolean.TRUE, null, null, null, null, null, null, null, null, null},
														 new Object[]{
																 Boolean.TRUE,
																 null,
																 null,
																 null,
																 null,
																 null,
																 new BigDecimal("4.00"),
																 List.of(true, false, true, false),
																 null,
																 null
														 }
												 )
					   )
		);
	}

	public static class TypedSelectDummy extends Select {

		@Getter
		private final ResultType resultType;

		public TypedSelectDummy(ResultType resultType) {
			setLabel(resultType.toString());
			setHolder(CONCEPT);
			this.resultType = resultType;
		}

		@Nullable
		@Override
		public List<ColumnId> getRequiredColumns() {
			return Collections.emptyList();
		}

		@Override
		public Aggregator<String> createAggregator() {
			return new Aggregator<>() {

				@Override
				public void init(Entity entity, QueryExecutionContext context) {

				}

				@Override
				public void consumeEvent(Bucket bucket, int event) {
					throw new UnsupportedOperationException();
				}

				@Override
				public String createAggregationResult() {
					throw new UnsupportedOperationException();
				}

			};
		}

	}
}
