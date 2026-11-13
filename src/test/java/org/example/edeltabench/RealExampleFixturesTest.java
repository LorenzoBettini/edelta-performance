package org.example.edeltabench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

/** Checks that the original projects, not generated stand-ins, are bundled. */
class RealExampleFixturesTest {
	@Test
	void allSixteenOriginalProgramsAreBundledWithTheirSourceEcoreModels() {
		var cases = RealExampleFixtures.all();
		assertEquals(16, cases.size());
		var ids = new HashSet<String>();
		for (var example : cases) {
			assertTrue(ids.add(example.id()));
			assertTrue(example.programPath().endsWith(".edelta"));
			assertFalse(example.program().length == 0);
			assertFalse(example.models().isEmpty());
			assertFalse(example.expectedPackage().isBlank());
			for (var model : example.models()) {
				assertTrue(model.path().endsWith(".ecore"));
				assertTrue(model.contents().length > 0);
			}
		}
		assertTrue(ids.contains("catalog-walkthrough"));
		assertTrue(ids.contains("library-v1-v2"));
		assertTrue(ids.contains("refactorings-personlist"));
		assertTrue(RealExampleFixtures.get("catalog-personlist").importsEcore());
		assertTrue(RealExampleFixtures.get("library-v3-v4").importsEcore());
		assertFalse(RealExampleFixtures.get("library-v1-v2").importsEcore());
	}
}
