package com.bakdata.conquery.io.result.parquet;

import static com.bakdata.conquery.io.result.ResultTestUtil.*;
import static org.apache.parquet.schema.LogicalTypeAnnotation.stringType;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.result.ResultTestUtil;
import com.bakdata.conquery.io.result.arrow.ArrowResultGenerationTest;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.io.DelegatingSeekableInputStream;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import org.junit.jupiter.api.Test;

@Slf4j
public class ParquetResultGenerationTest {

	public static final ConqueryConfig CONFIG = new ConqueryConfig();
	public static final UniqueNamer
			UNIQUE_NAMER =
			new UniqueNamer(new PrintSettings(false, Locale.ROOT, null, CONFIG, null, (selectInfo) -> selectInfo.getSelect().getLabel()));

	@Test
	void generateSchema() {
		List<ResultInfo> resultInfos = getResultTypes().stream().map(ResultTestUtil.TypedSelectDummy::new)
													   .map(select -> new SelectResultInfo(select, new CQConcept())).collect(Collectors.toList());

		final MessageType messageType = EntityResultWriteSupport.generateSchema(ResultTestUtil.ID_FIELDS, resultInfos, UNIQUE_NAMER);

		assertThat(messageType).isEqualTo(
				Types.buildMessage()
					 .optional(BINARY).as(stringType()).named("id1")
					 .optional(BINARY).as(stringType()).named("id2")
					 .optional(BOOLEAN).named("BOOLEAN")
					 .optional(INT32).as(LogicalTypeAnnotation.intType(32, true)).named("INTEGER")
					 .optional(DOUBLE).named("NUMERIC")
					 .optional(INT32).as(LogicalTypeAnnotation.dateType()).named("DATE")
					 .optionalGroup()
					 .optional(INT32).as(LogicalTypeAnnotation.dateType()).named("min")
					 .optional(INT32).as(LogicalTypeAnnotation.dateType()).named("max")
					 .named("DATE_RANGE")
					 .optional(BINARY).as(stringType()).named("STRING")
					 .optional(INT32).as(LogicalTypeAnnotation.intType(32, true)).named("MONEY")
					 .optionalGroup().as(LogicalTypeAnnotation.listType())
					 .repeatedGroup()
					 .optional(BOOLEAN).named("element")
					 .named("list")
					 .named("LIST[BOOLEAN]")
					 .optionalGroup().as(LogicalTypeAnnotation.listType())
					 .repeatedGroup()
					 .optionalGroup()
					 .optional(INT32).as(LogicalTypeAnnotation.dateType()).named("min")
					 .optional(INT32).as(LogicalTypeAnnotation.dateType()).named("max")
					 .named("element")
					 .named("list")
					 .named("LIST[DATE_RANGE]")
					 .optionalGroup().as(LogicalTypeAnnotation.listType())
					 .repeatedGroup()
					 .optional(BINARY).as(stringType()).named("element")
					 .named("list")
					 .named("LIST[STRING]")
					 .named("root")
		);

	}

	@Test
	void writeAndRead() throws IOException {

		// Initialize internationalization
		I18n.init();

		// Prepare every input data
		PrintSettings printSettings = new PrintSettings(
				false,
				Locale.ROOT,
				null,
				CONFIG,
				(cer) -> EntityPrintId.from(Integer.toString(cer.getEntityId()), Integer.toString(cer.getEntityId())),
				(selectInfo) -> selectInfo.getSelect().getLabel()
		);
		// The Shard nodes send Object[] but since Jackson is used for deserialization, nested collections are always a list because they are not further specialized
		List<EntityResult> results = getTestEntityResults();

		ManagedQuery managedQuery = getTestQuery();

		// First we write to the buffer, than we read from it and parse it as TSV
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ParquetRenderer.writeToStream(output, ResultTestUtil.ID_FIELDS, managedQuery.getResultInfos(), printSettings, managedQuery.streamResults());

		final byte[] buf = output.toByteArray();

		final ConqueryParquetReaderBuilder builder = new ConqueryParquetReaderBuilder(new ByteArrayInputFile(buf));
		StringJoiner stringJoiner = new StringJoiner("\n");
		try (final ParquetReader<Group> reader1 = builder.build()) {
			Group record;
			while ((record = reader1.read()) != null) {
				if (reader1.getCurrentRowIndex() == 0) {
					// Add Header
					stringJoiner.add(extractHeader(record.getType()));

				}
				stringJoiner.add(appendToString(new StringBuilder(), (SimpleGroup) record, false));
			}
		}

		final String actual = stringJoiner.toString();

		log.info("\n{}", actual);

		assertThat(actual).isEqualTo(ArrowResultGenerationTest.generateExpectedTSV(results, managedQuery.getResultInfos(), printSettings));

	}

	private CharSequence extractHeader(GroupType type) {
		StringJoiner stringJoiner = new StringJoiner("\t");
		type.getFields().stream().map(Type::getName).forEach(stringJoiner::add);
		return stringJoiner.toString();
	}

	private StringBuilder appendToString(StringBuilder builder, SimpleGroup group, boolean inSubGroup) {
		int i = 0;
		for (Type field : group.getType().getFields()) {
			final String name = field.getName();
			final int fieldIndex = group.getType().getFieldIndex(name);
			final int fieldRepetitionCount = group.getFieldRepetitionCount(fieldIndex);
			final int fieldCount = group.getType().getFields().size();
			try {
				if (field.getRepetition() == Type.Repetition.REPEATED) {
					builder.append("[");
					for (int listIndex = 0; listIndex < fieldRepetitionCount; listIndex++) {
						final Group listGroup = group.getGroup(fieldIndex, listIndex);
						if (listIndex > 0) {
							builder.append(",");
						}
						appendToString(builder, (SimpleGroup) listGroup, false);
					}
					builder.append("]");

				}
				else if (field.isPrimitive()) {
					final String stringValue = group.getValueToString(fieldIndex, i);
					if (inSubGroup) {
						builder.append("\"").append(name).append("\":");
					}
					builder.append(stringValue);
				}

				else {
					final Group subGroup = group.getGroup(fieldIndex, i);
					if (field.getLogicalTypeAnnotation() == null) {
						builder.append("{");

					}
					appendToString(builder, (SimpleGroup) subGroup, true);
					if (field.getLogicalTypeAnnotation() == null) {
						builder.append("}");

					}
				}
			}
			catch (RuntimeException e) {
				// Trapped into an unset field
				if (!inSubGroup) {
					// Only print null if not in a struct (actual subgroup)
					builder.append("null");
				}
				else if (fieldIndex > 0 && fieldIndex < fieldCount) {
					// Remove the last `,`
					builder.deleteCharAt(builder.length() - 1);
				}
			}
			finally {
				if (fieldIndex + 1 < fieldCount) {
					if (inSubGroup) {
						builder.append(",");
					}
					else {
						builder.append("\t");
					}
				}
			}
		}
		++i;
		return builder;
	}

	@RequiredArgsConstructor
	private static class ByteArrayInputFile implements InputFile {

		final byte[] buf;

		@Override
		public long getLength() {
			return buf.length;
		}

		@Override
		public SeekableInputStream newStream() {
			final ByteArrayInputStream stream = new ByteArrayInputStream(buf);
			return new DelegatingSeekableInputStream(stream) {
				@Override
				public long getPos() {
					return stream.available() - buf.length;
				}

				@Override
				public void seek(long newPos) {
					stream.reset();
					final long skip = stream.skip(newPos);
					if (newPos != skip) {
						throw new IllegalStateException("This is not intended to happen: Seek beyond the stream");
					}
				}
			};
		}
	}
}
