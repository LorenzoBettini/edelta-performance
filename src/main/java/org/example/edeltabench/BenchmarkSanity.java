package org.example.edeltabench;

import java.util.Locale;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.EcoreUtil;

import edelta.resource.derivedstate.EdeltaDerivedState;

/**
 * Verify that timing/heap experiments actually exercised the intended pipeline.
 * This is deliberately called only outside JMH measured invocations.
 */
public final class BenchmarkSanity {
	private BenchmarkSanity() { }

	public static void verify(EdeltaCompilerHarness.CompilationResult result,
							  BenchmarkWorkload workload, BenchmarkMode mode) {
		if (result.errors() != 0) {
			throw new IllegalStateException("Invalid Edelta program: " + result.issues());
		}
		if (workload.additions() > 0) {
			String lastAdded = String.format(Locale.ROOT, "Added%05d", workload.additions() - 1);
			boolean added = result.interpretedPackage().getEClassifier(lastAdded) != null;
			if (added != (mode == BenchmarkMode.NORMAL)) {
				throw new IllegalStateException("Incorrect interpreter behavior: final class "
						+ lastAdded + ", mode=" + mode + ", found=" + added);
			}
		}
		if (workload.references() > 0) {
			var first = result.interpretedPackage().getEClassifier("C00000");
			if (!(first instanceof EClass cls)) {
				throw new IllegalStateException("Missing first original EClass");
			}
			boolean expectedAbstract = mode == BenchmarkMode.NORMAL
					&& workload.referenceKind() == ReferenceKind.WRITE;
			if (cls.isAbstract() != expectedAbstract) {
				throw new IllegalStateException("Incorrect read/write semantics: "
						+ workload.referenceKind() + ", mode=" + mode);
			}
		}
		// A silent interpreter failure/timeout could still leave an added class.
		// Checking *all* per-AST-occurrence snapshots guards against this.
		var state = (EdeltaDerivedState) EcoreUtil.getAdapter(
				result.program().eAdapters(), EdeltaDerivedState.class);
		if (state == null) {
			throw new IllegalStateException("Missing Edelta derived state adapter");
		}
		long completedSnapshots = state.getEcoreReferenceExpressionStateMap().values()
				.stream().filter(s -> !s.getAccessibleElements().isEmpty()).count();
		long expectedSnapshots = mode == BenchmarkMode.NORMAL ? workload.references() : 0;
		if (completedSnapshots != expectedSnapshots) {
			throw new IllegalStateException("Unexpected nonempty snapshot count: "
					+ completedSnapshots + ", expected " + expectedSnapshots
					+ " for " + mode + ", workload " + workload);
		}
	}
}
