package com.bakdata.conquery.models.forms.arx;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.execution.Shareable;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.criteria.KAnonymity;

@Slf4j
@CPSType(base = ManagedExecution.class, id = "ARX_EXECUTION")
public class ArxExecution extends ManagedInternalForm implements SingleTableResult {

	@JsonIgnore
	private PrintSettings printSettings;

	@JsonIgnore
	ARXResult result;

	public ArxExecution(Form form, User user, Dataset submittedDataset) {
		super(form, user, submittedDataset);
	}

	@Override
	public void doInitExecutable(@NonNull DatasetRegistry datasetRegistry, ConqueryConfig config) {
		super.doInitExecutable(datasetRegistry, config);
		printSettings = new PrintSettings(true, Locale.ROOT, datasetRegistry, config, null);
	}

	@Override
	protected void finish(MetaStorage storage, ExecutionState executionState) {
		/*
			This method is called after all shard results have been collected (or an error occurred),
			We intercept the finishing here and apply the arx deidentifier to the data and hold it in memory
			After that we mark the execution as finished.
		 */

		if (executionState != ExecutionState.DONE) {
			// Internal execution failed end execution immediately
			super.finish(storage, executionState);
			return;
		}

		Shareable.log.trace("Query finished. Starting anonymization");

		// Convert to ARX data format
		Data.DefaultData data = Data.create();

		// Write header
		final List<ResultInfo> resultInfos = super.getResultInfos();
		final String[] headers = resultInfos.stream().map(info -> info.defaultColumnName(printSettings)).toArray(String[]::new);
		data.add(headers);

		// Add data. Convert everything into a string
		super.streamResults()
			 .flatMap(row -> row.listResultLines().stream()
								.map(line -> {
									String[] stringData = new String[resultInfos.size()];
									for (int cellIdx = 0; cellIdx < resultInfos.size(); cellIdx++) {
										final ResultInfo resultInfo = resultInfos.get(cellIdx);
										stringData[cellIdx] = resultInfo.getType().printNullable(printSettings, line[cellIdx]);
									}
									return stringData;
								}))
			 .forEach(data::add);

		// Define attributes for the column
		Arrays.stream(headers).forEach(header -> data.getDefinition().setAttributeType(header, AttributeType.INSENSITIVE_ATTRIBUTE));
		data.getDefinition().setAttributeType("sex", AttributeType.Hierarchy.create(List.of(
				new String[]{"f", "*"},
				new String[]{"m", "*"}
		)));

		// Configure ARX
		ARXConfiguration config = ARXConfiguration.create();
		config.addPrivacyModel(new KAnonymity(2));
		config.setSuppressionLimit(0.02d);

		// Run ARX
		ARXAnonymizer anonymizer = new ARXAnonymizer();
		anonymizer.setMaximumSnapshotSizeDataset(0.2);
		anonymizer.setMaximumSnapshotSizeSnapshot(0.2);
		anonymizer.setHistorySize(200);

		try {
			result = anonymizer.anonymize(data, config);

			if (!result.isResultAvailable()) {
				setError(new ConqueryError.ExecutionProcessingContextError("Unable to create anonymized result", Map.of(), null));
				super.finish(storage, ExecutionState.FAILED);
				return;
			}
		}
		catch (IOException | IllegalArgumentException e) {
			log.error("Unable to anonymize", e);
			setError(new ConqueryError.ExecutionProcessingError());
			super.finish(storage, ExecutionState.FAILED);
			return;
		}

		super.finish(storage, executionState);
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		// After the anonymization everything is a String for now
		return super.getResultInfos()
					.stream()
					.map(resultInfo -> new SimpleResultInfo(resultInfo.defaultColumnName(printSettings), ResultType.StringT.INSTANCE))
					.collect(Collectors.toList());
	}

	@Override
	public Stream<EntityResult> streamResults() {
		// Rebuild the EntityResult stream from the ARXresult (with a constant entity for now)
		final int ENTITIY_ID_DUMMY = 0;
		final DataHandle output = result.getOutput();
		final int numRows = output.getNumRows();
		final int numColumns = output.getNumColumns();

		return IntStream.range(0, numRows).mapToObj(
				rowIdx -> {
					Object[] line = new Object[numColumns];
					for (int column = 0; column < numColumns; column++) {
						line[column] = output.getValue(rowIdx, column);
					}
					return new SinglelineEntityResult(ENTITIY_ID_DUMMY, line);
				}
		);
	}
}
