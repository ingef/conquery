package com.bakdata.conquery.util.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;

@Slf4j
public class SqlTableValidator implements ConstraintValidator<ValidSqlTable, Table> {


	@Override
	public void initialize(ValidSqlTable constraintAnnotation) {
	}

	@Override
	public boolean isValid(Table value, ConstraintValidatorContext context) {

		final Stopwatch stopwatch = Stopwatch.createStarted();
		final LocalNamespace localNamespace = (LocalNamespace) value.getNamespace();
		final DSLContext dslContext = localNamespace.getDslContextWrapper().getDslContext();
		final SqlDialect dialect = localNamespace.getDialect();

		final List<org.jooq.Table<?>> tables = dslContext.meta()
														 .getTables(value.getName());

		log.trace("DONE fetching meta for {} within {}", value, stopwatch.elapsed());


		if (tables.isEmpty()) {
			context.buildConstraintViolationWithTemplate("SQL table does not exist")
				   .addPropertyNode("name")
				   .addConstraintViolation();
			return false;
		}
		else if (tables.size() > 1) {
			context.buildConstraintViolationWithTemplate("Multiple matching SQL table exist")
				   .addPropertyNode("name")
				   .addConstraintViolation();
			return false;
		}

		boolean valid = true;

		final org.jooq.Table<?> table = tables.getFirst();

		final List<Column> columns = new ArrayList<>(Arrays.asList(value.getColumns()));

		columns.add(value.getPrimaryColumn());

		for (int index = 0; index < columns.size(); index++) {
			Column column = columns.get(index);

			Field<?>[] matching = table.fields(column.getName());

			if (matching.length == 0 || matching[0] == null) {
				context.buildConstraintViolationWithTemplate("SQL Column does not exist")
					   .addContainerElementNode("columns", List.class, index)
					   .addConstraintViolation();
				valid = false;
			}
			else if (matching.length > 1) {
				context.buildConstraintViolationWithTemplate("Multiple matching SQL Columns")
					   .addPropertyNode("columns")
					   .addContainerElementNode("columns", List.class, index)
					   .addConstraintViolation();
				valid = false;
			}
			else if (!dialect.isTypeCompatible(matching[0], column.getType())) {
				context.buildConstraintViolationWithTemplate("SQL Column does not match required type")
					   .addPropertyNode("columns")
					   .addContainerElementNode("columns", List.class, index)
					   .addConstraintViolation();
				valid = false;
			}
		}
		return valid;
	}
}
