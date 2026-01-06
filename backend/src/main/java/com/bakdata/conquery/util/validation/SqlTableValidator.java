package com.bakdata.conquery.util.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.validation.ConstraintValidatorContext;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidator;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

@Slf4j
public class SqlTableValidator implements HibernateConstraintValidator<ValidSqlTable, Table> {


	@Override
	public boolean isValid(Table value, ConstraintValidatorContext context) {


		final Stopwatch stopwatch = Stopwatch.createStarted();
		final LocalNamespace localNamespace = (LocalNamespace) value.getNamespace();
		final DSLContext dslContext = localNamespace.getDslContextWrapper().getDslContext();
		final SqlDialect dialect = localNamespace.getDialect();

		final Result<Record> result;
		try {
			// We don't use DSL.meta here because that can be excessively slow.
			result = dslContext.select(DSL.asterisk())
							   .from(DSL.name(value.getName()))
							   .limit(0)
							   .fetch();
		}
		catch (DataAccessException e) {
			context.buildConstraintViolationWithTemplate("SQL table %s does not exist".formatted(value.getName()))
				   .addPropertyNode("name")
				   .addConstraintViolation();
			return false;
		}

		log.trace("DONE fetching meta for {} within {}", value, stopwatch.elapsed());


		boolean valid = true;

		final List<Column> columns = new ArrayList<>(Arrays.asList(value.getColumns()));

		if (value.getPrimaryColumn() != null) {
			// If null, it's provided by config.
			columns.add(value.getPrimaryColumn());
		}

		for (Column column : columns) {
			final Field<?> field = result.field(column.getName());

			if (field == null) {
				// NOTE: The Path to the property is not factually correct, but the error messages are much more readable that way.
				context.buildConstraintViolationWithTemplate("SQL Column `%s.%s` does not exist".formatted(value.getName(), column.getName()))
					   .addPropertyNode("columns")
					   .addPropertyNode(column.getName())
					   .addConstraintViolation();
				valid = false;
			}
			else if (!dialect.isTypeCompatible(field, column.getType())) {
				context.buildConstraintViolationWithTemplate("SQL Column `%s %s.%s` does not match required type %s".formatted(
							   field.getDataType(),
							   value.getName(),
							   column.getName(),
							   column.getType().name()
					   ))
					   .addPropertyNode("columns")
					   .addPropertyNode(column.getName())
					   .addConstraintViolation();
				valid = false;
			}
		}
		return valid;
	}
}
