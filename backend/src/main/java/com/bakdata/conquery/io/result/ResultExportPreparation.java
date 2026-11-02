package com.bakdata.conquery.io.result;

import java.util.Locale;
import java.util.OptionalLong;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.IdColumnUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResultExportPreparation {

	public static ResultExportContext prepare(
			Subject subject,
			ManagedExecutionId executionId,
			boolean pretty,
			OptionalLong limit,
			ConqueryConfig config) {
		ConqueryMDC.setLocation(subject.getName());

		final ManagedExecution execution = executionId.resolve();
		ResultUtil.checkSingleTableResult(execution);
		ResultUtil.authorizeExecutable(subject, execution);
		execution.initExecutable();

		final SingleTableResult result = (SingleTableResult) execution;
		final Namespace namespace = result.getNamespace();
		final IdPrinter idPrinter = IdColumnUtil.getIdPrinter(subject, execution, namespace, config.getIdColumns().getIds());
		final Locale locale = I18n.LOCALE.get();
		final PrintSettings settings = new PrintSettings(pretty, locale, config, idPrinter::createId, null);

		return new ResultExportContext(
				execution,
				result,
				subject,
				settings,
				config.getIdColumns().getIdResultInfos(),
				result.collectResultInfos(),
				limit
		);
	}
}
