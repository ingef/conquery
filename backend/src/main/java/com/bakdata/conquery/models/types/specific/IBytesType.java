package com.bakdata.conquery.models.types.specific;


public interface IBytesType extends Iterable<byte[]> {

	byte[] getElement(Number value);
	
	byte[] getElement(int value);
	
	int size();
	
	int getId(byte[] value);
}
