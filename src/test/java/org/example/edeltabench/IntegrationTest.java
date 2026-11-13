package org.example.edeltabench;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** NORMAL creates snapshots and applies mutations; NOP does neither. */
class IntegrationTest {
	@Test
	void bothReferenceKindsWorkInBothModes() {
		for (var kind : ReferenceKind.values()) {
			for (var order : StatementOrder.values()) {
				var workload = new BenchmarkWorkload(8, 4, 3, 2, kind, order);
				var inputs = BenchmarkFixtures.generate(workload);
				for (var mode : BenchmarkMode.values()) {
					var result = new EdeltaCompilerHarness(mode).compileAndValidate(inputs);
					assertDoesNotThrow(() -> BenchmarkSanity.verify(result, workload, mode));
				}
			}
		}
	}

	@Test
	void snapshotsExistWithoutMetamodelMutations() {
		var workload = new BenchmarkWorkload(8, 3, 0, 2, ReferenceKind.READ);
		var inputs = BenchmarkFixtures.generate(workload);
		for (var mode : BenchmarkMode.values()) {
			var result = new EdeltaCompilerHarness(mode).compileAndValidate(inputs);
			assertDoesNotThrow(() -> BenchmarkSanity.verify(result, workload, mode));
		}
	}
}
