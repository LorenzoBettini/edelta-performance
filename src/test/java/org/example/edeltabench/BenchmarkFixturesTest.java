package org.example.edeltabench;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BenchmarkFixturesTest {
	@Test
	void reproducibleInputs() {
		var spec = new BenchmarkWorkload(10, 3, 4, 2);
		var a = BenchmarkFixtures.generate(spec);
		var b = BenchmarkFixtures.generate(spec);
		assertArrayEquals(a.ecore(), b.ecore());
		assertArrayEquals(a.edelta(), b.edelta());
		assertTrue(new String(a.edelta(), StandardCharsets.UTF_8)
				.contains("ecoreref(C00000).abstract = true"));
	}

	@Test
	void readOnlyDoesNotEmitMutation() {
		var spec = new BenchmarkWorkload(10, 3, 4, 2, ReferenceKind.READ);
		var text = BenchmarkFixtures.source(spec);
		assertTrue(text.contains("ecoreref(C00000).name"));
		assertFalse(text.contains(".abstract = true"));
		assertTrue(text.contains("addNewEClass(\"Added00003\")"));
	}

	@Test
	void zeroAdditionsAllowedWhenReferencesExist() {
		var workload = new BenchmarkWorkload(10, 2, 0, 2, ReferenceKind.READ);
		assertFalse(BenchmarkFixtures.source(workload).contains("addNewEClass"));
	}

	@Test
	void orderPreservesExactlyTheSameStatements() {
		var refFirst = new BenchmarkWorkload(12, 10, 5, 2, ReferenceKind.READ,
				StatementOrder.REFERENCES_FIRST);
		var additionsFirst = new BenchmarkWorkload(12, 10, 5, 2, ReferenceKind.READ,
				StatementOrder.ADDITIONS_FIRST);
		var interleaved = new BenchmarkWorkload(12, 10, 5, 2, ReferenceKind.READ,
				StatementOrder.INTERLEAVED);
		var before = BenchmarkFixtures.source(refFirst);
		var after = BenchmarkFixtures.source(additionsFirst);
		var mixed = BenchmarkFixtures.source(interleaved);
		assertTrue(before.indexOf("ecoreref(") < before.indexOf("addNewEClass"));
		assertTrue(after.indexOf("addNewEClass") < after.indexOf("ecoreref("));
		assertTrue(mixed.indexOf("ecoreref(") < mixed.indexOf("addNewEClass"));
		assertTrue(mixed.indexOf("addNewEClass") < mixed.lastIndexOf("ecoreref("));
		for (var text : new String[]{before, after, mixed}) {
			assertEquals(10, text.split("ecoreref\\(", -1).length - 1);
			assertEquals(5, text.split("addNewEClass\\(", -1).length - 1);
			for (int i = 0; i < 10; i++) {
				assertTrue(text.contains("ecoreref(C" + String.format("%05d", i) + ").name"));
			}
			for (int i = 0; i < 5; i++) {
				assertTrue(text.contains("addNewEClass(\"Added" + String.format("%05d", i) + "\")"));
			}
		}
	}

	@Test
	void degenerateOrderingHasNoGapsButIsValid() {
		var oneRef = new BenchmarkWorkload(10, 1, 4, 2,
				ReferenceKind.READ, StatementOrder.INTERLEAVED);
		var noRefs = new BenchmarkWorkload(10, 0, 4, 2,
				ReferenceKind.READ, StatementOrder.INTERLEAVED);
		var noAdd = new BenchmarkWorkload(10, 4, 0, 2,
				ReferenceKind.READ, StatementOrder.INTERLEAVED);
		assertEquals(1, BenchmarkFixtures.source(oneRef).split("ecoreref\\(", -1).length - 1);
		assertEquals(4, BenchmarkFixtures.source(noRefs).split("addNewEClass\\(", -1).length - 1);
		assertEquals(4, BenchmarkFixtures.source(noAdd).split("ecoreref\\(", -1).length - 1);
	}

	@Test
	void rejectsInvalidSizes() {
		assertThrows(IllegalArgumentException.class, () -> new BenchmarkWorkload(0, 1, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> new BenchmarkWorkload(10, 0, 0, 1));
		assertThrows(IllegalArgumentException.class, () ->
				new BenchmarkWorkload(10, 1, 0, 1, null));
	}
}
