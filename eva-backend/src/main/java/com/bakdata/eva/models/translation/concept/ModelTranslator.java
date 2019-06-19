package com.bakdata.eva.models.translation.concept;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.concepts.filters.specific.BigMultiSelectFilter;
import com.bakdata.conquery.models.concepts.filters.specific.CountFilter;
import com.bakdata.conquery.models.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.concepts.filters.specific.DurationSumFilter;
import com.bakdata.conquery.models.concepts.filters.specific.MultiSelectFilter;
import com.bakdata.conquery.models.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter;
import com.bakdata.conquery.models.concepts.filters.specific.QuartersInYearFilter;
import com.bakdata.conquery.models.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.models.concepts.select.connector.FirstValueSelect;
import com.bakdata.conquery.models.concepts.select.connector.specific.CountQuartersSelect;
import com.bakdata.conquery.models.concepts.select.connector.specific.CountSelect;
import com.bakdata.conquery.models.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.models.concepts.select.connector.specific.DurationSumSelect;
import com.bakdata.conquery.models.concepts.select.connector.specific.QuartersInYearSelect;
import com.bakdata.conquery.models.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.Input;
import com.bakdata.conquery.models.preproc.outputs.AutoOutput;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.eva.models.translation.IdentifiableMocker;
import com.bakdata.eva.models.translation.concept.oldmodel.Description;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.eva.query.fiters.SlidingAverageFilter;
import com.bakdata.eva.query.fiters.SlidingSumFilter;
import com.bakdata.eva.query.selects.SlidingAverageSelect;
import com.bakdata.eva.query.selects.SlidingSumSelect;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.powerlibraries.io.Out;
import com.google.common.collect.MoreCollectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ModelTranslator {

	private static final DefaultPrettyPrinter PRINTER = new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("\t", "\n"))
		.withArrayIndenter(new DefaultIndenter("\t", "\n"));

	private static final ObjectReader DESCRIPTION_READER = Jackson.MAPPER.readerFor(Description.class);
	private static final Dataset DATASET = new Dataset();

	static {
		DATASET.setName("ILLEGAL_DATASET_NAME");
	}

	private static final ObjectReader CONCEPT_READER = DATASET.injectInto(new SingletonNamespaceCollection(new IdentifiableMocker.MockRegistry()).injectInto(
		Jackson.MAPPER.readerFor(Concept.class)));
	private static final ObjectWriter TABLE_WRITER = Jackson.MAPPER.registerModule(new ModelTranslatorModule())
		.writerFor(Table.class)
		.with(PRINTER);
	private static final ObjectWriter IMPORT_WRITER = Jackson.MAPPER.registerModule(new ModelTranslatorModule())
		.writerFor(ImportDescriptor.class)
		.with(PRINTER);
	private static final ObjectWriter WRITER = Jackson.MAPPER.registerModule(new ModelTranslatorModule())
		.writerFor(Concept.class)
		.with(PRINTER);

	public static void main(String[] args) throws IOException {
		File source = new File(args[0]).getCanonicalFile();
		File target = new File(args[1]).getCanonicalFile();

		new ModelTranslator(source, target, args[2]).translate(source);
	}

	private final File sourceRoot;
	private final File targetRoot;
	private final String tag;

	private void translate(File f) throws IOException {
		log.info("translating {}", f);
		try {
			if (f.isDirectory()) {
				for (File c : f.listFiles()) {
					translate(c);
				}
			}
			else if (f.getName().equals("structure.json")) {
				log.info("\tstructure.json no longer supported");
			}
			else if (f.getName().endsWith(".description.json")) {
				Description descr = DESCRIPTION_READER.readValue(f);
				translateDescriptionToTable(f, descr);
				translateDescriptionToImport(f, descr);
			}
			else if (f.getName().endsWith(".json")) {
				translateConcept(f);
			}
			else {
				log.info("\tunkown file type");
			}
		}
		catch (Exception e) {
			String message = "\t" + ExceptionUtils.getStackTrace(e);
			message = message.replace("\n", "\n\t");
			log.error(message);
		}
	}

	private Select createFilterSelect(Filter<?> filter) {
		/*
		 * AbstractSelectFilter
		 * BigMultiSelectFilter
		 * CountFilter
		 * CountQuartersFilter
		 * DateDistanceFilter
		 * DurationSumFilter
		 * ISelectFilter
		 * MultiSelectFilter
		 * NumberFilter
		 * PrefixTextFilter
		 * QuartersInYearFilter
		 * SelectFilter
		 * SumFilter
		 */

		// filters that need a FilterValue are temporarily implemented by counting distinct values.
		if (filter instanceof BigMultiSelectFilter || filter instanceof MultiSelectFilter || filter instanceof SelectFilter) {
			return new FirstValueSelect(((AbstractSelectFilter<?>) filter).getColumn());
		}

		if (filter instanceof CountFilter) {
			final CountSelect countSelect = new CountSelect();
			countSelect.setColumn(((CountFilter) filter).getColumn());
			countSelect.setDistinct(((CountFilter) filter).isDistinct());

			return countSelect;		}

		if (filter instanceof CountQuartersFilter) {
			return new CountQuartersSelect(((CountQuartersFilter) filter).getColumn());
		}

		if (filter instanceof DateDistanceFilter) {
			final DateDistanceSelect select = new DateDistanceSelect(((DateDistanceFilter) filter).getColumn());
			select.setTimeUnit(((DateDistanceFilter) filter).getTimeUnit());
			return select;
		}

		if (filter instanceof DurationSumFilter) {
			return new DurationSumSelect(((DurationSumFilter) filter).getColumn());
		}

		if (filter instanceof PrefixTextFilter) {
			return new FirstValueSelect(((PrefixTextFilter) filter).getColumn());
		}

		if (filter instanceof QuartersInYearFilter) {
			return new QuartersInYearSelect(((QuartersInYearFilter) filter).getColumn());
		}

		if (filter instanceof SumFilter) {
			return new SumSelect(((SumFilter) filter).isDistinct(), ((SumFilter) filter).getColumn(), ((SumFilter) filter).getSubtractColumn());
		}

		if (filter instanceof NumberFilter) {
			return new FirstValueSelect(((NumberFilter) filter).getColumn());
		}

		if (filter instanceof SlidingSumFilter) {
			return new SlidingSumSelect(
				((SlidingSumFilter) filter).getMaximumDaysColumn(),
				((SlidingSumFilter) filter).getDateRangeColumn(),
				((SlidingSumFilter) filter).getValueColumn());
		}

		if (filter instanceof SlidingAverageFilter) {
			return new SlidingAverageSelect(
				((SlidingAverageFilter) filter).getMaximumDaysColumn(),
				((SlidingAverageFilter) filter).getDateRangeColumn(),
				((SlidingAverageFilter) filter).getValueColumn());
		}

		throw new IllegalArgumentException(String.format("No Select defined for Filter '%s'", filter));
	}

	private void translateConcept(File f) throws IOException {
		JsonNode n = Jackson.MAPPER.readTree(f);

		Concept<?> res = CONCEPT_READER.readValue(n);

		if (res instanceof TreeConcept) {
			final TreeConcept treeConcept = (TreeConcept) res;

			final ExistsSelect select = new ExistsSelect();
			select.setName(res.getName()+"_exists");
			select.setDefault(true);

			treeConcept.getSelects().add(select);
		}
		else if (res instanceof VirtualConcept) {
			final VirtualConcept virtualConcept = (VirtualConcept) res;

			for (VirtualConceptConnector connector : virtualConcept.getConnectors()) {
				if (connector.getFilter() == null){
					final ExistsSelect existsSelect = new ExistsSelect();
					existsSelect.setHolder(virtualConcept);
					existsSelect.setName(virtualConcept.getName() + "_exists");
					existsSelect.setLabel(virtualConcept.getLabel());
					existsSelect.setDefault(true);

					virtualConcept.getSelects().add(existsSelect);
					continue;
				}

//				{
//					final Select filterSelect = createFilterSelect(connector.getFilter());
//
//					filterSelect.setLabel(connector.getFilter().getLabel() + "_select");
//					filterSelect.setHolder(connector);
//					filterSelect.setDescription("Automatisch erzeugter Zusatzwert.");
//					filterSelect.setDefault(true);
//
//					connector.getSelects().add(filterSelect);
//				}

				{
					final Select filterSelect = createFilterSelect(connector.getFilter());

					filterSelect.setLabel(connector.getFilter().getLabel() + "_select");
					filterSelect.setHolder(virtualConcept);
					filterSelect.setDescription("Automatisch erzeugter Zusatzwert.");
					filterSelect.setDefault(true);

					virtualConcept.getSelects().add(filterSelect);
				}
				//TODO auf connector oder auf concept ebene?s
			}


		}

		File result = targetRoot.toPath().resolve(sourceRoot.toPath().relativize(f.toPath())).toFile();
		result = new File(result.getParentFile(), res.getName() + ".concept.json");
		result.getParentFile().mkdirs();

		final String cleaned = WRITER.writeValueAsString(res).replace(String.format("%s.", DATASET.getName()), "");

		Out.file(result).withUTF8().write(cleaned);
	}

	private void translateDescriptionToImport(File f, Description descr) throws JsonGenerationException, JsonMappingException, IOException {
		ImportDescriptor imp = new ImportDescriptor();
		imp.setName(descr.getName() + "_" + this.tag);
		imp.setTable(descr.getName());
		imp.setLabel(descr.getName());
		imp.setInputs(Arrays.stream(descr.getInputs()).map(input -> {
			Input res = new Input();
			res.setFilter(input.getFilter());
			res.setSourceFile(new File(input.getSourceFile()));
			Description.OOutput primary;

			if (input.getAutoOutput() != null) {
				primary = Arrays.stream(input.getAutoOutput().getIdentifiers())
					.filter(o -> Boolean.TRUE.equals(o.getUnknownFields().get("primary")))
					.collect(MoreCollectors.onlyElement());

				res.setPrimary(newOutput(primary));
			}

			else {
				primary = Arrays.stream(input.getOutput())
					.filter(o -> Boolean.TRUE.equals(o.getUnknownFields().get("primary")))
					.collect(MoreCollectors.onlyElement());
				res.setPrimary(newOutput(primary));
			}

			if (input.getOutput() != null) {
				res.setOutput(Arrays.stream(input.getOutput()).filter(o -> o != primary).map(this::newOutput).toArray(Output[]::new));
			}
			else {
				res.setAutoOutput(newAutoOutput(input.getAutoOutput()));
			}

			return res;
		}).toArray(Input[]::new));

		File result = targetRoot.toPath().resolve(sourceRoot.toPath().relativize(f.toPath())).toFile();
		result = new File(result.getParentFile().getParentFile(), "imports/" + imp.getName() + ".import.json");
		result.getParentFile().mkdirs();
		IMPORT_WRITER.writeValue(result, imp);
	}

	private void translateDescriptionToTable(File f, Description description) throws IOException {
		Description.OInput input = description.getInputs()[0];
		Table table = new Table();
		table.setName(description.getName());
		Description.OOutput[] outputs = input.getAutoOutput() != null ? input.getAutoOutput().getIdentifiers() : input.getOutput();

		Description.OOutput primary = Arrays.stream(outputs)
			.filter(o -> Boolean.TRUE.equals(o.getUnknownFields().get("primary")))
			.collect(MoreCollectors.onlyElement());
		table.setPrimaryColumn(newColumn(primary));

		Column[] columns = Arrays.stream(outputs).filter(o -> o != primary).map(this::newColumn).toArray(Column[]::new);

		if (input.getAutoOutput() != null && (input.getAutoOutput().getType().equals("INGEF_DAYS_IN_RANGE") || input.getAutoOutput()
			.getType()
			.equals("DAYS_IN_RANGE"))) {
			/*
			case 0: return new ColumnDescription("date", MajorTypeId.DATE_RANGE);
			case 1: return new ColumnDescription("dayType", MajorTypeId.STRING);
			case 2: return new ColumnDescription("date_start", MajorTypeId.DATE);
			case 3: return new ColumnDescription("date_end", MajorTypeId.DATE);
			*/

			input.getAutoOutput().setType("INGEF_DAYS_IN_RANGE");

			final Column date = new Column();
			date.setTable(table);
			date.setName("date");
			date.setLabel("date");
			date.setType(MajorTypeId.DATE_RANGE);

			final Column dayType = new Column();
			dayType.setTable(table);
			dayType.setName("dayType");
			dayType.setLabel("dayType");
			dayType.setType(MajorTypeId.STRING);

			final Column dateStart = new Column();
			dateStart.setTable(table);
			dateStart.setName("date_start");
			dateStart.setLabel("date_start");
			dateStart.setType(MajorTypeId.DATE);

			final Column dateEnd = new Column();
			dateEnd.setTable(table);
			dateEnd.setName("date_end");
			dateEnd.setLabel("date_end");
			dateEnd.setType(MajorTypeId.DATE);

			final Column[] outputColumns = Arrays.stream(input.getAutoOutput().getIdentifiers())
				.filter(oop -> !oop.equals(primary))
				.map(this::newColumn)
				.toArray(Column[]::new);

			final Column[] DIRColumns = ArrayUtils.addAll(new Column[] { date, dayType, dateStart, dateEnd }, outputColumns);

			columns = DIRColumns;
		}

		table.setColumns(columns);

		File result = targetRoot.toPath().resolve(sourceRoot.toPath().relativize(f.toPath())).toFile();
		result = new File(result.getParentFile().getParentFile(), "tables/" + table.getName() + ".table.json");
		result.getParentFile().mkdirs();
		TABLE_WRITER.writeValue(result, table);
	}

	private Column newColumn(Description.OOutput o) {
		Column c = new Column();
		c.setName(o.getName());
		if (o.getLabel() != null)
			c.setLabel(o.getLabel());
		c.setType(getType(o));
		return c;
	}

	private MajorTypeId getType(Description.OOutput o) {
		switch (o.getOperation()) {
			case "CONCAT":
				return MajorTypeId.STRING;
			case "COPY":
			case "UNPIVOT":
				return o.getInputType();
			case "DATE_RANGE":
			case "QUARTER_TO_RANGE":
				return MajorTypeId.DATE_RANGE;
			case "QUARTER_TO_FIRST_DAY":
				return MajorTypeId.DATE;
			default:
				throw new UnsupportedOperationException(o.getOperation());
		}
	}

	private Output newOutput(Description.OOutput primary) {
		try {
			ObjectNode n = Jackson.MAPPER.valueToTree(primary);
			n.remove("primary");
			return Jackson.MAPPER.treeToValue(n, Output.class);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private AutoOutput newAutoOutput(Description.OAutoOutput primary) {

		try {
			ObjectNode n = Jackson.MAPPER.valueToTree(primary);
			ArrayNode identifiers = (ArrayNode) n.get("identifiers");

			int primaryIndex = -1;

			for (int i = 0; i < identifiers.size(); i++) {
				if(Boolean.TRUE.equals(primary.getIdentifiers()[i].getUnknownFields().get("primary")))
					primaryIndex = i;

				identifiers.set(i, Jackson.MAPPER.valueToTree(newOutput(primary.getIdentifiers()[i])));
			}

			if(primaryIndex != -1)
				identifiers.remove(primaryIndex);

			return Jackson.MAPPER.treeToValue(n, AutoOutput.class);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
