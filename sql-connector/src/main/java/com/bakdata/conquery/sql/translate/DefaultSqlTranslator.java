package com.bakdata.conquery.sql.translate;

import java.util.Optional;

import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.translate.query.FinalQueryStepComposer;
import com.bakdata.conquery.sql.translate.query.ResolvedQueryStepTranslator;
import com.bakdata.conquery.sql.translate.rendering.QueryStepRenderer;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

/** Default translate for framework-neutral, fully resolved concept queries. */
public final class DefaultSqlTranslator implements SqlTranslator {

	private final QueryStepRenderer renderer;
	private final ResolvedQueryStepTranslator translator = new ResolvedQueryStepTranslator();

	public DefaultSqlTranslator(DSLContext dslContext) {
		this.renderer = new QueryStepRenderer(dslContext);
	}

	@Override
	public TranslatedQuery translate(ResolvedQuery query, TranslatorDialect dialect) {
		SqlNameGenerator names = new SqlNameGenerator(dialect.getNameMaxLength());
		ResolvedQueryStepTranslator.TranslatedQuerySteps compiled = translator.translate(query, dialect, names, Optional.empty());
		QueryStep root = compiled.step();
		QueryStep finalStep = FinalQueryStepComposer.compose(root, compiled.externalValues(), query.includeValidityDate(), dialect);
		Select<Record> rendered = renderer.toSelectQuery(finalStep, dialect);
		return new TranslatedQuery(rendered.getSQL(ParamType.INLINED),
				TranslatedColumnUtil.create(query.entitySchema(), query.resultColumns(), rendered.getSelect()));
	}

}
