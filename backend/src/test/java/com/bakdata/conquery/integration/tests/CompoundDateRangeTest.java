package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.ConqueryConstants.EXTENSION_PREPROCESSED;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.preproc.PreprocessedData;
import com.bakdata.conquery.models.preproc.PreprocessedReader;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import org.apache.commons.io.FileUtils;

public class CompoundDateRangeTest implements ProgrammaticIntegrationTest {
	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		//COMPOUND_DATERANGE_TEST
		final StandaloneSupport conquery = testConquery.getSupport(name);
		MetaStorage storage = conquery.getMetaStorage();

		final String testJson = In.resource("/tests/query/COMPOUND_DATERANGE_TEST/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final Namespace namespace = conquery.getNamespace();
		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);


		{
			LoadingUtil.importTableContents(conquery,test.getContent().getTables(),dataset);
			conquery.waitUntilWorkDone();

			RequiredTable rTable = test.getContent().getTables().get(0);
			String tableName = rTable.getName();

			/*
			FileUtils.copyInputStreamToFile(rTable.getCsv().stream(), new File(conquery.getTmpDir(), rTable.getCsv().getName()));

			// create import descriptor
			final File descriptionFile = conquery.getTmpDir().toPath().resolve(tableName + ConqueryConstants.EXTENSION_DESCRIPTION).toFile();
			*/
			final File outFile = conquery.getTmpDir().toPath().resolve(tableName + EXTENSION_PREPROCESSED).toFile();

			/*
			TableImportDescriptor desc = new TableImportDescriptor();

			desc.setName(tableName);
			desc.setTable(tableName);
			TableInputDescriptor input = new TableInputDescriptor();
			{
				input.setPrimary(rTable.getPrimaryColumn().createOutput());
				input.setSourceFile(rTable.getCsv().getName());
				input.setOutput(new OutputDescription[rTable.getColumns().length]);
				for (int i = 0; i < rTable.getColumns().length; i++) {
					input.getOutput()[i] = rTable.getColumns()[i].createOutput();
				}
			}
			desc.setInputs(new TableInputDescriptor[]{input});

			Jackson.MAPPER.writeValue(descriptionFile, desc);

			// preprocess
			conquery.preprocessTmp(conquery.getTmpDir(), List.of(descriptionFile));
			//clear the MDC location from the preprocessor
			ConqueryMDC.clearLocation();
			conquery.waitUntilWorkDone();
			*/

			PreprocessedReader preprocessedReader = new PreprocessedReader(new GZIPInputStream(new FileInputStream(outFile)));
			preprocessedReader.readHeader();
			preprocessedReader.addReplacement(Dataset.PLACEHOLDER.getId(), dataset);
			preprocessedReader.readDictionaries();
			PreprocessedData preprocessedData = preprocessedReader.readData();


           int i = 0;
		}

	}
}

