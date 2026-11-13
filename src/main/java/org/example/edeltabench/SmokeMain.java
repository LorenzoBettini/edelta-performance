package org.example.edeltabench;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/** Fast correctness check for normal/NOP and read-only/mutating cases. */
public final class SmokeMain {
	private SmokeMain() { }

	public static void main(String[] args) {
		EcorePlugin.INSTANCE.getString("_UI_DiagnosticRoot_diagnostic");
		int classes = args.length > 0 ? Integer.parseInt(args[0]) : 12;
		int references = args.length > 1 ? Integer.parseInt(args[1]) : 3;
		int additions = args.length > 2 ? Integer.parseInt(args[2]) : 4;
		for (var kind : ReferenceKind.values()) {
			if (args.length > 3 && kind != ReferenceKind.parse(args[3])) {
				continue;
			}
			for (var order : StatementOrder.values()) {
				if (args.length > 4 && order != StatementOrder.parse(args[4])) {
					continue;
				}
				var workload = new BenchmarkWorkload(
						classes, references, additions, 2, kind, order);
				var inputs = BenchmarkFixtures.generate(workload);
				for (var mode : BenchmarkMode.values()) {
					var result = new EdeltaCompilerHarness(mode).compileAndValidate(inputs);
					BenchmarkSanity.verify(result, workload, mode);
					System.out.printf("mode=%s, referenceKind=%s, order=%s, classes=%d, refs=%d, "
									+ "additions=%d, errors=%d%n",
							mode, kind, order.parameter(), classes, references, additions,
							result.errors());
				}
			}
		}
		System.out.println("Smoke check OK: modes and snapshot counts verified.");
	}
}
