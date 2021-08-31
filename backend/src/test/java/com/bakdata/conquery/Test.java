package com.bakdata.conquery;

import lombok.RequiredArgsConstructor;
import java.util.List;

public class Test {

	@org.junit.jupiter.api.Test
	public void test() {
		E1 e = E1.A;
	}

	@RequiredArgsConstructor
	public static enum E1{
		A(E2.a);

		private final E2 e;
	}

	@RequiredArgsConstructor
	public static enum E2 {
		a(E1.A);

		private final E1 e;
	}
}
