package com.bakdata.conquery.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class EndCheckableInputStream extends PushbackInputStream {

	public EndCheckableInputStream(InputStream in) {
		super(in, 1);
	}

	public boolean isAtEnd() throws IOException {
		int b = this.read();
		if(b == -1) {
			return true;
		}
		this.unread(b);
		return false;
	}
}
