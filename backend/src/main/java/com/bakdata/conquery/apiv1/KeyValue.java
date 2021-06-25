package com.bakdata.conquery.apiv1;

import java.io.Serializable;

import lombok.Data;

/**
 * This class represents a simple Key-Value pair as it is used in the additionInfos field.
 */
@Data
public class KeyValue implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String key;
	private String value;
	
}
