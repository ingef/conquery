package com.bakdata.conquery.sql.compiler;

import java.util.Optional;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.FinalQueryStepComposer;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.compiler.rendering.QueryStepRenderer;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

/** Default compiler for framework-neutral, fully resolved concept queries. */
public final class DefaultSqlCompiler implements SqlCompiler {

	private final QueryStepRenderer renderer;
	private final ResolvedQueryStepCompiler stepCompiler = new ResolvedQueryStepCompiler();

	public DefaultSqlCompiler(DSLContext dslContext) {
		this.renderer = new QueryStepRenderer(dslContext);
	}

	@Override
	public CompiledQuery compile(ResolvedQuery query, CompilerDialect dialect) {
		SqlNameGenerator names = new SqlNameGenerator(dialect.getNameMaxLength());
		ResolvedQueryStepCompiler.CompiledQuerySteps compiled = stepCompiler.compile(query, dialect, names, Optional.empty());
		QueryStep root = compiled.step();
		QueryStep finalStep = FinalQueryStepComposer.compose(root, compiled.externalValues(), query.includeValidityDate(), dialect);
		Select<Record> rendered = renderer.toSelectQuery(finalStep, dialect);
		return new CompiledQuery(rendered.getSQL(ParamType.INLINED),
				CompiledQueryColumns.create(query.entitySchema(), query.resultColumns(), rendered.getSelect()));
	}

}
