package com.bakdata.conquery.models.events.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.parser.specific.string.StringParser;
import com.bakdata.conquery.models.types.specific.AStringType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


@Slf4j
public class GenerationTests {
	public static Stream<Arguments> createRandomContent() {
		return IntStream
			.of(100)
			.mapToObj( numberOfValues -> {
				Random r = new Random(7);
				ArrayList<Object[]> arrays = new ArrayList<>();
				for(int i = 0;i<numberOfValues;i++) {
					Object[] event = new Object[18];
					arrays.add(event);

					if(r.nextBoolean()) {
						event[0] = (CDate.ofLocalDate(LocalDate.now()));
					}
					if(r.nextBoolean()) {
						event[1] = Long.toHexString(r.nextLong());
					}
					if(r.nextBoolean()) {
						event[2] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[3] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[4] = Long.valueOf(r.nextInt());
					}
					if(r.nextBoolean()) {
						event[5] = Integer.valueOf((byte)r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[6] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[7] = Long.valueOf((byte)r.nextInt());
					}
					if(r.nextBoolean()) {
						event[8] = Long.valueOf((byte)r.nextInt());
					}
					if(r.nextBoolean()) {
						event[9] = Integer.valueOf(r.nextInt()).toString();
					}
					if(r.nextBoolean()) {
						event[10] = BigDecimal.valueOf(r.nextInt(4), r.nextInt(10)-5);
					}
					if(r.nextBoolean()) {
						event[11] = Long.valueOf((byte)r.nextInt());
					}
					event[12] = Long.valueOf(r.nextInt());
					event[13] = Long.valueOf(r.nextInt());
					//event[14] = null;
					event[15] = Long.valueOf(r.nextInt());
					if(r.nextBoolean()) {
						event[16] = BigDecimal.valueOf(r.nextInt(4), r.nextInt(120)-60);
					}
					if(r.nextBoolean()) {
						event[17] = Integer.toString(r.nextInt(800));
					}
				}
				arrays.trimToSize();

				return Arguments.of(numberOfValues, arrays);
			}
			);
	}

	public Bucket generateBucket(List<Object[]> arrays) throws IOException {
		Parser[] parser = new Parser[] {
			MajorTypeId.DATE.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.STRING.createParser(),
			MajorTypeId.DECIMAL.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.INTEGER.createParser(),
			MajorTypeId.DECIMAL.createParser(),
			MajorTypeId.STRING.createParser()
		};


		for(Object[] event:arrays) {
			for(int i=0;i<parser.length;i++) {
				try {
					//only parse strings, this test otherwise already creates parsed values
					if(parser[i] instanceof StringParser && event[i] != null) {
						event[i] = parser[i].parse((String)event[i]);
					}
					parser[i].addLine(event[i]);
				} catch(Exception e) {
					throw new IllegalArgumentException("Column "+i, e);
				}
			}
		}
		
		Import imp = new Import();
		imp.setTable(new TableId(new DatasetId("test_dataset"), "table"));
		imp.setName("import");
		imp.setColumns(IntStream.range(0, parser.length)
			.mapToObj(i->column(imp,i))
			.toArray(ImportColumn[]::new)
		);

		Decision[] decisions = Arrays.stream(parser).map(Parser::findBestType).toArray(Decision[]::new);
		for(int i=0;i<parser.length;i++) {
			imp.getColumns()[i].setType(decisions[i].getType());
			log.info("{}: {} mapped to {}", imp.getColumns()[i], parser[i], imp.getColumns()[i].getType());
		}
		
		List<Object[]> result = new ArrayList<>(arrays.size());
		for(Object[] event:arrays) {
			Object[] line = Arrays.copyOf(event, event.length);
			for(int i=0;i<imp.getColumns().length;i++) {
				if(event[i] != null) {
					line[i] = decisions[i].getTransformer().transform(event[i]);
				}
			}
			result.add(line);
		}
		for(int i=0;i<imp.getColumns().length;i++) {
			imp.getColumns()[i].getType().writeHeader(new NullOutputStream());
		}

		return null;
	}
	
	@ParameterizedTest(name="{0}")
	@MethodSource("createRandomContent")
	public void testParallelSerialization(int numberOfValues, List<Object[]> arrays) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchMethodException, SecurityException, JSONException, InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(100);
		List<Future<?>> futures = new ArrayList<>();
		for(int i=0;i<30;i++) {
			List<Object[]> l = arrays.stream().map(v->Arrays.copyOf(v, v.length)).collect(Collectors.toList());
			futures.add(pool.submit(()->generateBucket(l)));
		}
		pool.shutdown();
		for(Future<?> f:futures) {
			assertThatCode(()->f.get()).doesNotThrowAnyException();
		}
		pool.awaitTermination(1, TimeUnit.HOURS);
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("createRandomContent")
	public void testSerialization(int numberOfValues, List<Object[]> arrays) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchMethodException, SecurityException, JSONException {
		List<Object[]> originalArrays = new ArrayList<>(arrays);
		originalArrays.replaceAll(v->Arrays.copyOf(v, v.length));
		
		Bucket bucket = generateBucket(arrays);
		for(int i=0;i<arrays.size();i++) {
			for(int c=0;c<arrays.get(i).length;c++) {
				Column fake = new Column();
				fake.setPosition(c);
				
				Object orig = originalArrays.get(i)[c];
				String message = "checking "+c+" "+bucket.getImp().getColumns()[c].getType()+":"+i+" = "+orig;
				
				if(orig == null) {
					assertThat(bucket.has(i, fake))
						.as(message)
						.isFalse();
				}
				else {
					ImportColumn impCol = bucket.getImp().getColumns()[c];
					assertThat(bucket.has(i, fake))
						.as(message+" is not null")
						.isTrue();
					
					if(orig instanceof BigDecimal) {
						assertThat((BigDecimal)bucket.getAsObject(i, fake))
							.as(message)
							.usingComparator(BigDecimal::compareTo)
							.isEqualTo(orig);
					}
					else if(impCol.getType().getTypeId() == MajorTypeId.STRING) {
						assertThat(((AStringType<?>)impCol.getType()).getElement(bucket.getString(i, fake)))
							.as(message)
							.isEqualTo(orig);
					}
					else {
						assertThat(bucket.getAsObject(i, fake))
							.as(message)
							.isEqualTo(orig);
					}
				}
				
			}
			bucket.calculateMap(i, bucket.getImp());
		}
		CentralRegistry registry = new CentralRegistry();
		registry.register(bucket.getImp());

		SerializationTestUtil
			.forType(Bucket.class)
			.registry(registry)
			.test(bucket);
	}

	private ImportColumn column(Import imp, int pos) {
		ImportColumn col = new ImportColumn();
		col.setName(String.format("@column%02d", pos));
		col.setParent(imp);
		col.setPosition(pos);
		return col;
	}
}
