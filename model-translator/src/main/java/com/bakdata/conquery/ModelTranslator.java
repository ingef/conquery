package com.bakdata.conquery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.bakdata.conquery.models.preproc.Input;
import com.bakdata.conquery.models.preproc.outputs.AutoOutput;
import com.bakdata.conquery.models.preproc.outputs.Output;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.bakdata.conquery.oldmodel.Description;
import com.bakdata.conquery.oldmodel.Description.OAutoOutput;
import com.bakdata.conquery.oldmodel.Description.OInput;
import com.bakdata.conquery.oldmodel.Description.OOutput;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.MoreCollectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class ModelTranslator {
	
	private static final DefaultPrettyPrinter PRINTER = new DefaultPrettyPrinter()
			.withObjectIndenter(new DefaultIndenter("\t", "\n"))
			.withArrayIndenter( new DefaultIndenter("\t", "\n"));

	private static final ObjectReader DESCRIPTION_READER = Jackson.MAPPER.readerFor(Description.class);
	private static final Dataset DATASET = new Dataset();
	static {
		DATASET.setName("dataset");
	}
	private static final ObjectReader CONCEPT_READER = DATASET.injectInto(
		new SingletonNamespaceCollection(mockRegistry())
		.injectInto(
			Jackson.MAPPER.readerFor(Concept.class)
		)
	);
	private static final ObjectWriter TABLE_WRITER = Jackson.MAPPER
		.registerModule(new ModelTranslatorModule())
		.writerFor(Table.class)
		.with(PRINTER);
	private static final ObjectWriter IMPORT_WRITER = Jackson.MAPPER
		.registerModule(new ModelTranslatorModule())
		.writerFor(ImportDescriptor.class)
		.with(PRINTER);
	private static final ObjectWriter WRITER = Jackson.MAPPER
		.registerModule(new ModelTranslatorModule())
		.writerFor(JsonNode.class)
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
			if(f.isDirectory()) {
				for(File c:f.listFiles()) {
					translate(c);
				}
			}
			else if (f.getName().equals("structure.json")) {
				log.info("\tstructure.json no longer supported");
			}
			else if(f.getName().endsWith(".description.json")) {
				Description descr = DESCRIPTION_READER.readValue(f);
				translateDescriptionToTable(f, descr);
				translateDescriptionToImport(f, descr);
			}
			else if(f.getName().endsWith(".json")) {
				translateConcept(f);
			}
			else {
				log.info("\tunkown file type");
			}
		} catch(Exception e) {
			String message = "\t"+ExceptionUtils.getStackTrace(e);
			message = message.replace("\n", "\n\t");
			log.error(message);
		}
	}
	
	private void translateConcept(File f) throws IOException {
		JsonNode n = Jackson.MAPPER.readTree(f);

		Concept<?> res = CONCEPT_READER.readValue(n);

		
		File result = targetRoot.toPath().resolve(sourceRoot.toPath().relativize(f.toPath())).toFile();
		result = new File(result.getParentFile(), res.getName()+".concept.json");
		result.getParentFile().mkdirs();
		WRITER.writeValue(result, n);
	}

	private void translateDescriptionToImport(File f, Description descr) throws JsonGenerationException, JsonMappingException, IOException {
		ImportDescriptor imp = new ImportDescriptor();
		imp.setName(descr.getName()+"_"+this.tag);
		imp.setTable(descr.getName());
		imp.setLabel(descr.getName());
		imp.setInputs(Arrays.stream(descr.getInputs())
			.map(input -> {
				Input res = new Input();
				res.setFilter(input.getFilter());
				res.setSourceFile(new File(input.getSourceFile()));
				OOutput primary;
				if(input.getAutoOutput() != null) {
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
				
				if(input.getOutput() != null) {
					res.setOutput(Arrays
						.stream(input.getOutput())
						.filter(o->o!=primary)
						.map(this::newOutput)
						.toArray(Output[]::new)
					);
				}
				else {
					res.setAutoOutput(newAutoOutput(input.getAutoOutput()));
				}
				
				return res;
			})
			.toArray(Input[]::new)
		);
		
		File result = targetRoot.toPath().resolve(sourceRoot.toPath().relativize(f.toPath())).toFile();
		result = new File(result.getParentFile().getParentFile(), "imports/"+imp.getName()+".import.json");
		result.getParentFile().mkdirs();
		IMPORT_WRITER.writeValue(result, imp);
	}

	private void translateDescriptionToTable(File f, Description description) throws IOException {
		OInput input = description.getInputs()[0];
		Table t = new Table();
		t.setName(description.getName());
		OOutput[] outputs = input.getAutoOutput()!=null?input.getAutoOutput().getIdentifiers():input.getOutput();
			
		OOutput primary = Arrays.stream(outputs)
				.filter(o -> Boolean.TRUE.equals(o.getUnknownFields().get("primary")))
				.collect(MoreCollectors.onlyElement());
		t.setPrimaryColumn(newColumn(primary));
	
		t.setColumns(
			Arrays
				.stream(outputs)
				.filter(o -> o!=primary)
				.map(this::newColumn)
				.toArray(Column[]::new)
		);
		
		File result = targetRoot.toPath().resolve(sourceRoot.toPath().relativize(f.toPath())).toFile();
		result = new File(result.getParentFile().getParentFile(), "tables/"+t.getName()+".table.json");
		result.getParentFile().mkdirs();
		TABLE_WRITER.writeValue(result, t);
	}

	private Column newColumn(OOutput o) {
		Column c = new Column();
		c.setName(o.getName());
		if(o.getLabel()!=null)
			c.setLabel(o.getLabel());
		c.setType(getType(o));
		return c;
	}

	private MajorTypeId getType(OOutput o) {
		switch(o.getOperation()) {
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
	
	private Output newOutput(OOutput primary) {
		try {
			ObjectNode n = Jackson.MAPPER.valueToTree(primary);
			n.remove("primary");
			return Jackson.MAPPER.treeToValue(n, Output.class);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private AutoOutput newAutoOutput(OAutoOutput primary) {
		try {
			ObjectNode n = Jackson.MAPPER.valueToTree(primary);
			ArrayNode identifiers = (ArrayNode) n.get("identifiers");
			for(int i=0;i<identifiers.size();i++) {
				identifiers.set(i, Jackson.MAPPER.valueToTree(newOutput(primary.getIdentifiers()[i])));
			}
			return Jackson.MAPPER.treeToValue(n, AutoOutput.class);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static CentralRegistry mockRegistry() {
		CentralRegistry registry = mock(CentralRegistry.class, inv->{throw new UnsupportedOperationException(inv.getMethod().toString());});
		doAnswer(new IdentifiableMocker<>()).when(registry).resolve(any());
		doAnswer(inv -> Optional.of(new IdentifiableMocker<>().answer(inv))).when(registry).getOptional(any());
		
		return registry;
	}
	
	private static class IdentifiableMocker<T> implements Answer<T> {
		@Override
		public T answer(InvocationOnMock invocation) throws Throwable {
			IId<?> id = invocation.getArgument(0);
			String[] parts = StringUtils.split(id.toString(), '.');
			if(id instanceof DatasetId) {
				Dataset d = mockAnswer(Dataset.class, id);
				//doReturn(parts[parts.length-1]).when(d).getName();
				return (T)d;
			}
			else if(id instanceof ColumnId) {
				return (T) mockAnswer(Column.class, id);
			}
			else if(id instanceof TableId) {
				return (T) mockAnswer(Table.class, id);
			}
			else
				throw new IllegalStateException("Unknown id type "+id.getClass());
		}

		private static <X extends Identifiable> X mockAnswer(Class<X> cl, IId id) {
			X x = mock(cl, inv->{throw new UnsupportedOperationException(inv.getMethod().toString());});
			doReturn(id).when(x).getId();
			doReturn(id.toString()).when(x).toString();
			return x;
		}
	}
}
