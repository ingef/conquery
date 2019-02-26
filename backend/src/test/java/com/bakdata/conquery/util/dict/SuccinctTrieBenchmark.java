
package com.bakdata.conquery.util.dict;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.github.powerlibraries.io.In;
import com.google.common.base.Stopwatch;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j @UtilityClass
public class SuccinctTrieBenchmark {
	public static Stream<String> data() throws IOException {
		return In.resource(SuccinctTrieBenchmark.class, "SuccinctTrieTest.data").streamLines();
	}

	public static void main(String[] args) throws IOException{
		
		//better way to do this?
		Duration putUncrompressedTime = Duration.ofMillis(0L);
		Duration compressTime = Duration.ofMillis(0L);
		Duration getIdTime = Duration.ofMillis(0L);
		Duration getElementTime = Duration.ofMillis(0L);

		
		for (int i = 0; i< 100; i++) {
			Dictionary dict = new Dictionary();
			Stopwatch stopwatch = Stopwatch.createStarted();
	
			data().forEach(dict::add);
			stopwatch.reset().start();
			
			dict.compress();
	
			log.info("time taken for compress: " + stopwatch.elapsed());
	
			stopwatch.reset().start();
	
			data().forEach(dict::getId);
	
			log.info("time taken for getId: " + stopwatch.elapsed());
			stopwatch.reset().start();
	
			IntStream.range(0, dict.size()).forEach(dict::getElement);
	
			log.info("time taken for getElement: " + stopwatch.elapsed());
			stopwatch.stop();
		}
	}
}