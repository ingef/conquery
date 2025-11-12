package com.bakdata.conquery.sql.conquery;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.val;

import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CTConditionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Case;
import org.jooq.CaseConditionStep;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Slf4j
public class SqlMatchingStats {

	public void createFunctionForConcept(TreeConcept concept, SqlFunctionProvider provider) {

		for (ConceptTreeConnector connector : concept.getConnectors()) {
			CTConditionContext context = CTConditionContext.create(connector, provider);
			String name = "resolve_id_%s_%s_%s".formatted(concept.getDataset().getName(), concept.getName(), connector.getName());

			Field<String> forConcept = createForConceptTreeNode(concept, context);

			log.info("{}:\n{}", name, forConcept);
		}
	}


	public Field<String> createForConceptTreeNode(ConceptTreeNode<?> current, CTConditionContext context){
		Field<String> currentId = field(val(current.getId().toString()));

		if (current.getChildren().isEmpty()){
			return currentId;
		}

		Case decode = DSL.decode();
		CaseConditionStep<String> step = null;

		for (ConceptTreeChild child : current.getChildren()) {
			WhereCondition converted = child.getCondition().convertToSqlCondition(context);

			Field<String> result = createForConceptTreeNode(child, context);

			step = step == null ? decode.when(converted.condition(), result)
								: step.when(converted.condition(), result);
		}

		return step.otherwise(currentId);
	}
}
