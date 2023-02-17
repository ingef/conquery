package com.bakdata.conquery.models.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.bakdata.conquery.models.auth.oidc.keycloak.GroupUtil;
import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakGroup;
import org.junit.jupiter.api.Test;

public class GroupUtilTest {

	private static final Set<KeycloakGroup> HIERARCHY = Set.of(
			new KeycloakGroup("a", "A", "a", Map.of(), Set.of(
					new KeycloakGroup("aa", "A", "a/a", Map.of(), Set.of()),
					new KeycloakGroup("ab", "B", "a/b", Map.of(), Set.of()),
					new KeycloakGroup("ac", "C", "a/c", Map.of(), Set.of())
			)),
			new KeycloakGroup("b", "B", "b", Map.of(), Set.of(
					new KeycloakGroup("ba", "A", "b/a", Map.of(), Set.of()),
					new KeycloakGroup("bb", "B", "b/b", Map.of(), Set.of(

							new KeycloakGroup("bba", "A", "b/b/a", Map.of(), Set.of()),
							new KeycloakGroup("bbb", "B", "b/b/b", Map.of(), Set.of()),
							new KeycloakGroup("bbc", "C", "b/b/c", Map.of(), Set.of())
					)),
					new KeycloakGroup("bc", "C", "b/c", Map.of(), Set.of())
			)),
			new KeycloakGroup("c", "C", "c", Map.of(), Set.of())

	);

	@Test
	public void findTopLevel() {
		final Collection<KeycloakGroup> actual = GroupUtil.getParentGroups(new KeycloakGroup("c", "C", "c", Map.of(), Set.of()), HIERARCHY);

		assertThat(actual).containsExactly(new KeycloakGroup("c", "C", "c", Map.of(), Set.of()));
	}

	@Test
	public void findMidLevel() {
		final Collection<KeycloakGroup> actual = GroupUtil.getParentGroups(new KeycloakGroup("ba", "A", "b/a", Map.of(), Set.of()), HIERARCHY);

		assertThat(actual).containsExactly(
				new KeycloakGroup("ba", "A", "b/a", Map.of(), Set.of()),
				new KeycloakGroup("b", "B", "b", Map.of(), Set.of(
						new KeycloakGroup("ba", "A", "b/a", Map.of(), Set.of()),
						new KeycloakGroup("bb", "B", "b/b", Map.of(), Set.of(

								new KeycloakGroup("bba", "A", "b/b/a", Map.of(), Set.of()),
								new KeycloakGroup("bbb", "B", "b/b/b", Map.of(), Set.of()),
								new KeycloakGroup("bbc", "C", "b/b/c", Map.of(), Set.of())
						)),
						new KeycloakGroup("bc", "C", "b/c", Map.of(), Set.of())
				))
		);
	}

	@Test
	public void findLowLevel() {
		final Collection<KeycloakGroup> actual = GroupUtil.getParentGroups(new KeycloakGroup("bba", "A", "b/b/a", Map.of(), Set.of()), HIERARCHY);

		assertThat(actual).containsExactly(
				new KeycloakGroup("bba", "A", "b/b/a", Map.of(), Set.of()),
				new KeycloakGroup("bb", "B", "b/b", Map.of(), Set.of(

						new KeycloakGroup("bba", "A", "b/b/a", Map.of(), Set.of()),
						new KeycloakGroup("bbb", "B", "b/b/b", Map.of(), Set.of()),
						new KeycloakGroup("bbc", "C", "b/b/c", Map.of(), Set.of())
				)),
				new KeycloakGroup("b", "B", "b", Map.of(), Set.of(
						new KeycloakGroup("ba", "A", "b/a", Map.of(), Set.of()),
						new KeycloakGroup("bb", "B", "b/b", Map.of(), Set.of(

								new KeycloakGroup("bba", "A", "b/b/a", Map.of(), Set.of()),
								new KeycloakGroup("bbb", "B", "b/b/b", Map.of(), Set.of()),
								new KeycloakGroup("bbc", "C", "b/b/c", Map.of(), Set.of())
						)),
						new KeycloakGroup("bc", "C", "b/c", Map.of(), Set.of())
				))
		);
	}

	@Test
	public void throwOnUnknownLeafGroup() {
		assertThatCode(
				() -> GroupUtil.getParentGroups(new KeycloakGroup("baa", "A", "b/a/a", Map.of(), Set.of()), HIERARCHY)
		).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	public void throwOnUnknownSiblingGroup() {
		assertThatCode(
				() -> GroupUtil.getParentGroups(new KeycloakGroup("bd", "D", "b/d", Map.of(), Set.of()), HIERARCHY)
		).isInstanceOf(NoSuchElementException.class);
	}

}
