package com.bakdata.conquery.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import com.bakdata.conquery.models.auth.basic.PasswordHelper;
import com.password4j.Argon2Function;
import com.password4j.BcryptFunction;
import com.password4j.CompressedPBKDF2Function;
import com.password4j.HashingFunction;
import com.password4j.ScryptFunction;
import com.password4j.types.Argon2;
import com.password4j.types.Bcrypt;
import com.password4j.types.Hmac;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PasswordHelperTest {


	/**
	 * Arguments where generated with:
	 *
	 * <pre>
	 * import com.password4j.Password;
	 *
	 * class Scratch {
	 * 	public static void main(String[] args) {
	 * 		System.out.println(Password.hash("test").withArgon2().getResult());
	 * 		System.out.println(Password.hash("test").withScrypt().getResult());
	 * 		System.out.println(Password.hash("test").withBcrypt().getResult());
	 * 		System.out.println(Password.hash("test").withCompressedPBKDF2().getResult());
	 *        }
	 * }
	 * </pre>
	 */
	static Stream<Arguments> arguments() {
		return Stream.of(
				Arguments.arguments("$argon2id$v=19$m=15360,t=2,p=1$r35m/UGz8lq4ICjcNkb2GUcfYub07450QRTTapYwiJCQDOI9Maa0dlym/iL0AceTNNXgaxLUyGB5EfJoqr+Wng$WVnHZU8uwvufgWPlVh5T+MnTtX5Ry0hhCD0ej90L0Kk", Argon2Function.getInstance(15360, 2, 1, 32, Argon2.ID)),
				Arguments.arguments("$100801$SBpPHCtLT+2FbJ2BS49J4sgRXfvduVm17U9yd0Ygky/3MgUgK1r4LMixKSQX4LQjSEuE6tV8ibABXXAr9tCZKA==$aPTssj2maVw34QgrhRIsUHu6irB1NrjiFpdpUXFHHA+XhjPG03PKrbj5CBXJx3cCUosU/IARQliSW2LWRLFtiw==", ScryptFunction.getInstance(65536, 8, 1, 64)),
				Arguments.arguments("$2b$10$YMPj.MoAs81tO8HzrCYxnOujaPwbu5SGsSrdNyxdIJ9BlBIv9i0t.", BcryptFunction.getInstance(Bcrypt.B, 10)),
				Arguments.arguments("$3$1331439861760256$+Rqke26gKhtP60UkVR2a3SfszrOkVrMiJ6LZUWvl2vI5OpW815zKiod8Sdz3aOcuajo6c1iKEXcWjk61emmgTw==$CiH0mwqibUZD5R5HqFNpaYCkWjiYcTQe0sjG+4ZYw/A=", CompressedPBKDF2Function.getInstance(Hmac.SHA256, 310000, 256))
		);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void test(String hash, HashingFunction hashProvider) {
		assertThat(PasswordHelper.getHashingFunction(hash)).isEqualTo(hashProvider);

	}
}
