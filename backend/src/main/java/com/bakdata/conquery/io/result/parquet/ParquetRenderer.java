package com.bakdata.conquery.io.result.parquet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.google.common.io.CountingOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;

/**
 * Result renderer for the parquet file format.
 */
@UtilityClass
public class ParquetRenderer {

	@RequiredArgsConstructor
	public static class StreamOutputFile implements OutputFile {

		private final PositionTrackingOutputStream outputStream;

		@Override
		public PositionOutputStream create(long blockSizeHint) throws IOException {
			return outputStream;
		}

		@Override
		public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
			return outputStream;
		}

		@Override
		public boolean supportsBlockSize() {
			return false;
		}

		@Override
		public long defaultBlockSize() {
			return 0;
		}
	}

	@RequiredArgsConstructor
	public static class PositionTrackingOutputStream extends PositionOutputStream {

		final private CountingOutputStream stream;

		@Override
		public long getPos() throws IOException {
			return stream.getCount();
		}

		@Override
		public void write(int b) throws IOException {
			stream.write(b);
		}
	}

	public static void writeToStream(
			OutputStream outputStream,

			List<ResultInfo> idHeaders,
			List<ResultInfo> resultInfo,
			PrintSettings printSettings,
			Stream<EntityResult> results) throws IOException {

		// Wrap the request output stream in an output file, so the parquet writer can consume it
		final OutputFile outputFile = new StreamOutputFile(
				new PositionTrackingOutputStream(
						new CountingOutputStream(outputStream)));

		final ConqueryParquetWriterBuilder conqueryParquetWriterBuilder = new ConqueryParquetWriterBuilder(outputFile)
				.setIdHeaders(idHeaders)
				.setResultInfo(resultInfo)
				.setPrintSettings(printSettings);

		try (final ParquetWriter<EntityResult> parquetWriter = conqueryParquetWriterBuilder.build()) {

			/*
			 WORKAROUND: We need the conversion to SinglelineEntityResult here because a RecordConsumer only produces one line/record
			 even if multiple messages are started.
			 */
			Iterator<SinglelineEntityResult> resultIterator = results.flatMap(ParquetRenderer::convertToSingleLine).iterator();
			while (resultIterator.hasNext()) {
				final EntityResult entityResult = resultIterator.next();

				parquetWriter.write(entityResult);
			}
		}
	}

	/**
	 * Converts a possible {@link MultilineEntityResult} to a stream of {@link SinglelineEntityResult}s.
	 *
	 * @param entityResult the result to convert.
	 * @return the stream of {@link SinglelineEntityResult}s
	 */
	private static Stream<SinglelineEntityResult> convertToSingleLine(EntityResult entityResult) {
		if (entityResult instanceof SinglelineEntityResult) {
			return Stream.of((SinglelineEntityResult) entityResult);
		}
		return entityResult.streamValues().map(line -> new SinglelineEntityResult(entityResult.getEntityId(), line));
	}


}
