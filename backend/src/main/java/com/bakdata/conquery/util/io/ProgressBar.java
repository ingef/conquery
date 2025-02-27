package com.bakdata.conquery.util.io;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

//TODO replace with https://github.com/vdurmont/etaprinter
@Slf4j
public class ProgressBar {
	
	private static final int CHARACTERS = 50;
	private static final char[] BAR_CHARACTERS = {
		' ',
		'▌',
		'█'
	};
	private static final char RIGHT = '▌';
	
	private final AtomicLong currentValue = new AtomicLong(0);
	@Getter
	private final AtomicLong maxValue;
	private final AtomicLong lastPercentage = new AtomicLong(0);
	private final long startTime;

	public ProgressBar(long maxValue) {
		this.maxValue = new AtomicLong(Math.max(1, maxValue));
		this.startTime = System.nanoTime();
	}
	
	public void addCurrentValue(long add) {
		long current = currentValue.addAndGet(add);
		long newPercentage = current*100L/maxValue.get();
		long last = lastPercentage.getAndSet(newPercentage);
		if(newPercentage!=last) {
			print();
		}
	}
	
	public void addMaxValue(long add) {
		maxValue.addAndGet(add);
	}
	
	public void done() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.valueOf(BAR_CHARACTERS[2]).repeat(CHARACTERS));
		sb.append(" 100% DONE");
		log.info(sb.toString());
	}
	
	private void print() {
		StringBuilder sb = new StringBuilder();
		long last = lastPercentage.get();
		long remaining = last;
		
		for(int i=0;i<CHARACTERS;i++) {
			int v = (int)Math.min(remaining, 2);
			remaining -= v;
			sb.append(BAR_CHARACTERS[v]);
		}
		sb.append(RIGHT);
		sb.append(' ');
		if(last<100L) {
			sb.append(' ');
		}
		if(last<10L) {
			sb.append(' ');
		}
		sb.append(last);
		sb.append("%\test. time remaining: ");
		sb.append(Duration
			.ofNanos((System.nanoTime()-startTime)*(101L-last)/(last+1L))
			.toString()
            .substring(2)
            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
            .toLowerCase()
		);
		sb.append('\r');
		log.info(sb.toString());
	}
}
