package com.bakdata.conquery.quarkus.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.EXISTING_PROPERTY,
		property = "type",
		visible = true,
		defaultImpl = QuerySubmissionPayload.GenericQuerySubmissionPayload.class
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = QuerySubmissionPayload.ConceptQuerySubmissionPayload.class, name = "CONCEPT_QUERY"),
		@JsonSubTypes.Type(value = QuerySubmissionPayload.SecondaryIdQuerySubmissionPayload.class, name = "SECONDARY_ID_QUERY"),
		@JsonSubTypes.Type(value = QuerySubmissionPayload.GenericQuerySubmissionPayload.class, name = "FORM_QUERY")
})
@Schema(
		description = "Query submission payload, discriminated by `type`.",
		discriminatorProperty = "type",
		oneOf = {
				QuerySubmissionPayload.ConceptQuerySubmissionPayload.class,
				QuerySubmissionPayload.SecondaryIdQuerySubmissionPayload.class,
				QuerySubmissionPayload.GenericQuerySubmissionPayload.class
		},
		discriminatorMapping = {
				@DiscriminatorMapping(value = "CONCEPT_QUERY", schema = QuerySubmissionPayload.ConceptQuerySubmissionPayload.class),
				@DiscriminatorMapping(value = "SECONDARY_ID_QUERY", schema = QuerySubmissionPayload.SecondaryIdQuerySubmissionPayload.class),
				@DiscriminatorMapping(value = "FORM_QUERY", schema = QuerySubmissionPayload.GenericQuerySubmissionPayload.class)
		}
)
public abstract class QuerySubmissionPayload {
	public String type;

	public static final class ConceptQuerySubmissionPayload extends QuerySubmissionPayload {
		@Schema(description = "Query root node. Polymorphic and discriminated by field `type`.")
		@Valid
		public QueryNode root;

		public ConceptQuerySubmissionPayload() {
			this.type = "CONCEPT_QUERY";
		}
	}

	public static final class SecondaryIdQuerySubmissionPayload extends QuerySubmissionPayload {
		public String secondaryId;
		@Schema(description = "Query root node. Polymorphic and discriminated by field `type`.")
		@Valid
		public QueryNode root;

		public SecondaryIdQuerySubmissionPayload() {
			this.type = "SECONDARY_ID_QUERY";
		}
	}

	public static final class GenericQuerySubmissionPayload extends QuerySubmissionPayload {
		public Map<String, Object> values;

		public GenericQuerySubmissionPayload() {
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({
			@JsonSubTypes.Type(value = AndNode.class, name = "AND"),
			@JsonSubTypes.Type(value = OrNode.class, name = "OR"),
			@JsonSubTypes.Type(value = NegationNode.class, name = "NEGATION"),
			@JsonSubTypes.Type(value = DateRestrictionNode.class, name = "DATE_RESTRICTION"),
			@JsonSubTypes.Type(value = ConceptNode.class, name = "CONCEPT"),
			@JsonSubTypes.Type(value = SavedQueryNode.class, name = "SAVED_QUERY"),
			@JsonSubTypes.Type(value = TemporalNode.class, name = "TEMPORAL"),
			@JsonSubTypes.Type(value = ExternalResolvedNode.class, name = "EXTERNAL_RESOLVED")
	})
	@Schema(
			description = "Base query node. Concrete schema is selected via discriminator `type`.",
			discriminatorProperty = "type",
			oneOf = {
					AndNode.class,
					OrNode.class,
					NegationNode.class,
					DateRestrictionNode.class,
					ConceptNode.class,
					SavedQueryNode.class,
					TemporalNode.class,
					ExternalResolvedNode.class
			},
			discriminatorMapping = {
					@DiscriminatorMapping(value = "AND", schema = AndNode.class),
					@DiscriminatorMapping(value = "OR", schema = OrNode.class),
					@DiscriminatorMapping(value = "NEGATION", schema = NegationNode.class),
					@DiscriminatorMapping(value = "DATE_RESTRICTION", schema = DateRestrictionNode.class),
					@DiscriminatorMapping(value = "CONCEPT", schema = ConceptNode.class),
					@DiscriminatorMapping(value = "SAVED_QUERY", schema = SavedQueryNode.class),
					@DiscriminatorMapping(value = "TEMPORAL", schema = TemporalNode.class),
					@DiscriminatorMapping(value = "EXTERNAL_RESOLVED", schema = ExternalResolvedNode.class)
			}
	)
	public abstract static class QueryNode {
	}

	@Schema(name = "AndQueryNode")
	public static class AndNode extends QueryNode {
		public List<@Valid QueryNode> children;
	}

	@Schema(name = "OrQueryNode")
	public static class OrNode extends QueryNode {
		public List<@Valid QueryNode> children;
	}

	@Schema(name = "NegationQueryNode")
	public static class NegationNode extends QueryNode {
		@Valid
		public QueryNode child;
	}

	@Schema(name = "DateRestrictionQueryNode")
	public static class DateRestrictionNode extends QueryNode {
		@Valid
		public DateRangePayload dateRange;
		@Valid
		public QueryNode child;
	}

	@Schema(name = "ConceptQueryNode")
	public static class ConceptNode extends QueryNode {
		public List<String> ids;
		public String label;
		public Boolean excludeFromTimeAggregation;
		public Boolean excludeFromSecondaryId;
		public List<@Valid TableConfigPayload> tables;
		public List<String> selects;
	}

	@Schema(name = "SavedQueryNode")
	public static class SavedQueryNode extends QueryNode {
		public String query;
	}

	@Schema(name = "TemporalQueryNode")
	public static class TemporalNode extends QueryNode {
		public TemporalModePayload mode;
		public QueryNode index;
		public String indexSelector;
		public QueryNode compare;
		public String compareSelector;
	}

	@Schema(name = "ExternalResolvedQueryNode")
	public static class ExternalResolvedNode extends QueryNode {
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
	@JsonSubTypes({
			@JsonSubTypes.Type(value = BeforeTemporalModePayload.class, name = "BEFORE"),
			@JsonSubTypes.Type(value = AfterTemporalModePayload.class, name = "AFTER"),
			@JsonSubTypes.Type(value = WhileTemporalModePayload.class, name = "WHILE")
	})
	@Schema(
			description = "Temporal mode, discriminated by `type`.",
			discriminatorProperty = "type",
			oneOf = {
					BeforeTemporalModePayload.class,
					AfterTemporalModePayload.class,
					WhileTemporalModePayload.class
			},
			discriminatorMapping = {
					@DiscriminatorMapping(value = "BEFORE", schema = BeforeTemporalModePayload.class),
					@DiscriminatorMapping(value = "AFTER", schema = AfterTemporalModePayload.class),
					@DiscriminatorMapping(value = "WHILE", schema = WhileTemporalModePayload.class)
			}
	)
	public abstract static class TemporalModePayload {
		public TemporalModeType type;

		protected TemporalModePayload() {
		}

		protected TemporalModePayload(TemporalModeType type) {
			this.type = type;
		}
	}

	public static final class BeforeTemporalModePayload extends TemporalModePayload {
		public DayRangePayload days;

		public BeforeTemporalModePayload() {
			super(TemporalModeType.BEFORE);
		}

		public BeforeTemporalModePayload(DayRangePayload days) {
			this();
			this.days = days;
		}
	}

	public static final class AfterTemporalModePayload extends TemporalModePayload {
		public DayRangePayload days;

		public AfterTemporalModePayload() {
			super(TemporalModeType.AFTER);
		}

		public AfterTemporalModePayload(DayRangePayload days) {
			this();
			this.days = days;
		}
	}

	public static final class WhileTemporalModePayload extends TemporalModePayload {
		public WhileTemporalModePayload() {
			super(TemporalModeType.WHILE);
		}
	}

	public enum TemporalModeType {
		BEFORE,
		AFTER,
		WHILE
	}

	public static class DayRangePayload {
		public Integer min;
		public Integer max;
	}

	public static class DateRangePayload {
		public LocalDate min;
		public LocalDate max;
	}

	public static class TableConfigPayload {
		public String id;
		@Valid
		public DateColumnConfigPayload dateColumn;
		public List<String> selects;
		public List<@Valid FilterValue> filters;
	}

	public static class DateColumnConfigPayload {
		public String value;
	}

}
