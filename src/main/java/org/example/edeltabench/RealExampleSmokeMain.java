package org.example.edeltabench;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;

import edelta.resource.derivedstate.EdeltaDerivedState;

/**
 * Manual smoke tests for unchanged programs from the example projects.
 *
 * --list shows every bundled case, --all tests all cases, or pass specific IDs.
 * Not every case is guaranteed to compile in isolation: migration programs and
 * library calls may require project-specific dependencies or registries.
 */
public final class RealExampleSmokeMain {
	private RealExampleSmokeMain() { }

	public static void main(String[] args) {
		if (args.length == 1 && args[0].equals("--list")) {
			for (var example : RealExampleFixtures.all()) {
				System.out.println(example.id() + " [" + example.category() + "] "
						+ example.programPath() + " (" + example.models().size() + " Ecore file(s))");
			}
			return;
		}
		List<RealExampleFixtures.Example> cases = new ArrayList<>();
		if (args.length == 1 && args[0].equals("--all")) {
			cases.addAll(RealExampleFixtures.all());
		} else if (args.length == 0) {
			cases.add(RealExampleFixtures.get("catalog-walkthrough"));
		} else {
			for (String id : args) {
				cases.add(RealExampleFixtures.get(id));
			}
		}
		EdeltaCompilerHarness compiler = new EdeltaCompilerHarness(BenchmarkMode.NORMAL);
		int failures = 0;
		for (var example : cases) {
			try {
				var result = compiler.compileAndValidate(example);
				verify(example, result);
				System.out.println("PASS " + example.id() + " (" + result.issues().size()
						+ " total diagnostics)");
			} catch (Exception | AssertionError e) {
				failures++;
				System.err.println("FAIL " + example.id() + ": " + e);
			}
		}
		if (failures > 0) {
			throw new IllegalStateException(failures + " / " + cases.size()
					+ " original examples did not pass isolated compilation; do not benchmark those IDs");
		}
	}

	static void verify(RealExampleFixtures.Example example,
			EdeltaCompilerHarness.CompilationResult result) {
		if (result.errors() != 0) {
			throw new IllegalStateException("Original example '" + example.id()
					+ "' has validation errors: " + result.issues());
		}
		// Validation alone is insufficient: the safe interpreter can recover from
		// failures. Require observable interpreter activity for every original case.
		var state = (EdeltaDerivedState) EcoreUtil.getAdapter(
				result.program().eAdapters(), EdeltaDerivedState.class);
		if (state == null || state.getModifiedElements().isEmpty()
				|| state.getEcoreReferenceExpressionStateMap().values().stream()
						.noneMatch(s -> !s.getAccessibleElements().isEmpty())) {
			throw new IllegalStateException("Original example '" + example.id()
					+ "' did not produce both modifications and reference snapshots");
		}
		if ("catalog-walkthrough".equals(example.id())
				&& (result.interpretedPackage().getEClassifier("RenamedClass") == null
					|| result.interpretedPackage().getEClassifier("NewClass") == null)) {
			throw new IllegalStateException("Catalog example interpretation did not finish");
		}
	}
}
