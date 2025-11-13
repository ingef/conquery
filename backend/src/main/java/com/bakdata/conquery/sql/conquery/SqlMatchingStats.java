package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.val;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.Case;
import org.jooq.CaseConditionStep;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Slf4j
public class SqlMatchingStats {

	@NotNull
	private static Field<String> idField(Identifiable<?, ?> current) {
		return field(val(current.getId().toString()));
	}

	public void createFunctionForConcept(TreeConcept concept, SqlFunctionProvider provider) {

		for (ConceptTreeConnector connector : concept.getConnectors()) {
			CTConditionContext context = new CTConditionContext("value", provider);
			String name = "resolve_id_%s_%s_%s".formatted(concept.getDataset().getName(), concept.getName(), connector.getName());

			Set<String> auxiliaryColumns = getAuxiliaryColumns(concept);
			Field<String> forConcept = forNode(idField(concept), concept.getChildren(), context);

			log.info("{}:{}\n{}", name, auxiliaryColumns, forConcept);
		}
	}

	@NotNull
	private Set<String> getAuxiliaryColumns(TreeConcept concept) {
		return concept.getChildren().stream()
					  .map(this::collectAuxiliaryColumns)
					  .flatMap(Collection::stream)
					  .collect(Collectors.toSet());
	}

	public Field<String> createForConceptTreeNode(ConceptTreeChild current, CTConditionContext context) {
		Field<String> currentId = idField(current);

		return forNode(currentId, current.getChildren(), context);
	}

	private Set<String> collectAuxiliaryColumns(ConceptTreeChild current) {
		Set<String> auxiliaryColumns = new HashSet<>();
		if (current.getCondition() != null) {
			auxiliaryColumns.addAll(current.getCondition().auxiliaryColumns());
		}

		for (ConceptTreeChild child : current.getChildren()) {
			auxiliaryColumns.addAll(collectAuxiliaryColumns(child));
		}

		return auxiliaryColumns;
	}

	private Field<String> forNode(Field<String> currentId, List<ConceptTreeChild> children, CTConditionContext context) {
		if (children.isEmpty()) {
			return currentId;
		}

		Case decode = DSL.decode();
		CaseConditionStep<String> step = null;

		for (ConceptTreeChild child : children) {
			WhereCondition converted = child.getCondition().convertToSqlCondition(context);

			Field<String> result = createForConceptTreeNode(child, context);

			step = step == null ? decode.when(converted.condition(), result)
								: step.when(converted.condition(), result);
		}

		return step.otherwise(currentId);
	}
}
