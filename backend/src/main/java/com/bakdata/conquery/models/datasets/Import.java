package com.bakdata.conquery.models.datasets;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.events.generation.ClassGenerator;
import com.bakdata.conquery.models.events.generation.SafeJavaString;
import com.bakdata.conquery.models.events.generation.SafeName;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.util.ConqueryJavaEscape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.github.powerlibraries.io.In;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @NoArgsConstructor @Slf4j
public class Import extends NamedImpl<ImportId> {

	@Valid @NotNull
	private TableId table;
	@JsonManagedReference @NotNull
	private ImportColumn[] columns = new ImportColumn[0];
	private long numberOfEntries;
	private String suffix;
	@JsonIgnore
	private transient BlockFactory blockFactory;
	
	
	@Override
	public ImportId createId() {
		return new ImportId(table, getName());
	}
	
	@JsonIgnore
	public int getNullWidth() {
		//count the columns which can not store null
		return Ints.checkedCast(Arrays.stream(columns).
				filter(col -> col.getType().requiresExternalNullStore()
						&& col.getType().getNullLines() < col.getType().getLines())
				.count());
	}

	public void loadExternalInfos(NamespacedStorage storage) {
		for(ImportColumn col:columns) {
			col.getType().loadExternalInfos(storage);
		}
	}

	@JsonIgnore
	public synchronized BlockFactory getBlockFactory() {
		if(blockFactory == null) {
			generateClasses();
		}
		
		return blockFactory;
	}
	
	public synchronized String getSuffix() {
		if(suffix == null) {
			suffix = UUID.randomUUID().toString().replace('-', '_')+"_"+ConqueryJavaEscape.escape(table.getTable())+"_"+ConqueryJavaEscape.escape(getName());
		}
		return suffix;
	}
	
	private void generateClasses() {
		String bucketSource = null;
		String factorySource = null;
		try {
			ClassGenerator gen = new ClassGenerator();
			
			addClass(
				gen,
				"com.bakdata.conquery.models.events.generation.Bucket_"+getSuffix(),
				"BucketTemplate.ftl"
			);
			addClass(
				gen,
				"com.bakdata.conquery.models.events.generation.BlockFactory_"+getSuffix(),
				"BlockFactoryTemplate.ftl"
			);
			gen.compile();
			
			blockFactory = (BlockFactory) gen
				.getClassByName("com.bakdata.conquery.models.events.generation.BlockFactory_"+getSuffix())
				.getConstructor()
				.newInstance();
		} catch (Exception e) {
			log.error("Failed to generate classes for {}:\n{}\n{}", this, bucketSource, factorySource);
			throw new IllegalStateException("Failed to generate Bucket classes", e);
		}
	}

	private void addClass(ClassGenerator gen, String className, String template) throws ClassNotFoundException, IOException {
		String source = applyTemplate(template);
		log.trace("Generating class {} for {}:\n{}", className, this, source);
		gen.addForCompile(className, source);
	}

	private String applyTemplate(String templateName) {
		try(Reader reader = In.resource(BlockFactory.class, templateName).withUTF8().asReader();
				StringWriter writer = new StringWriter()) {
			
			Configuration cfg = Freemarker.createForJavaTemplates();
	
			new Template("template_"+templateName, reader, cfg)
				.process(
					ImmutableMap
						.builder()
						.put("suffix", getSuffix())
						.put("imp", this)
						.put("types", MajorTypeId.values())
						.put("safeName", SafeName.INSTANCE)
						.put("safeJavaString", SafeJavaString.INSTANCE)
						.put("bucketSize", ConqueryConfig.getInstance().getCluster().getEntityBucketSize())
						.build(),
					writer
			);
			
			writer.close();
			return writer.toString();
		} catch (TemplateException | IOException e) {
			throw new IllegalStateException("Failed to generate class "+templateName+" for "+this, e);
		}
	}

	public static Import createForPreprocessing(String table, String tag, PPColumn[] columns) {
		Import imp = new Import();
		imp.setTable(new TableId(new DatasetId("preprocessing"), table));
		imp.setName(tag);
		ImportColumn[] impCols = new ImportColumn[columns.length];
		for(int c = 0; c < impCols.length; c++) {
			ImportColumn col = new ImportColumn();
			col.setName(columns[c].getName());
			col.setParent(imp);
			col.setPosition(c);
			col.setType(columns[c].getType());
			impCols[c] = col;
		}
		imp.setColumns(impCols);
		
		return imp;
	}
	
	public long estimateMemoryConsumption() {
		long mem = 0;
		for(ImportColumn col:columns) {
			mem+=col.getType().estimateMemoryConsumption();
		}
		return mem;
	}
}
