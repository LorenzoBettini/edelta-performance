package org.example.edeltabench;

import java.time.Instant;
import java.util.Locale;

/**
 * Post-GC used-heap delta for one workload and mode in one JVM.
 * Compiled resources stay strongly reachable through liveResult.
 */
public final class RetainedHeapMain {
	private static volatile Object liveResult;

	private RetainedHeapMain() { }

	public static void main(String[] args) throws InterruptedException {
		BenchmarkMode mode = BenchmarkMode.parse(args.length > 0 ? args[0] : "normal");
		int classes = args.length > 1 ? Integer.parseInt(args[1]) : 250;
		int references = args.length > 2 ? Integer.parseInt(args[2]) : 20;
		int additions = args.length > 3 ? Integer.parseInt(args[3]) : 20;
		ReferenceKind referenceKind = ReferenceKind.parse(args.length > 4 ? args[4] : "write");
		StatementOrder order = StatementOrder.parse(
				args.length > 5 ? args[5] : "references-first");
		var workload = new BenchmarkWorkload(classes, references, additions, 2, referenceKind, order);
		var compiler = new EdeltaCompilerHarness(mode);
		var data = BenchmarkFixtures.generate(workload);
		for (int i = 0; i < 3; i++) {
			BenchmarkSanity.verify(compiler.compileAndValidate(data), workload, mode);
		}
		long baseline = occupiedAfterGc();
		liveResult = compiler.compileAndValidate(data);
		var result = (EdeltaCompilerHarness.CompilationResult) liveResult;
		BenchmarkSanity.verify(result, workload, mode);
		long after = occupiedAfterGc();
		System.out.println("timestamp,mode,classes,references,additions,reference_kind,"
				+ "baseline_bytes,after_bytes,delta_bytes,statement_order");
		System.out.printf(Locale.ROOT, "%s,%s,%d,%d,%d,%s,%d,%d,%d,%s%n", Instant.now(),
				mode.name().toLowerCase(Locale.ROOT), classes, references, additions,
				referenceKind.parameter(), baseline, after, after - baseline, order.parameter());
		java.lang.ref.Reference.reachabilityFence(liveResult);
	}

	private static long occupiedAfterGc() throws InterruptedException {
		for (int i = 0; i < 3; i++) {
			System.gc();
			Thread.sleep(150);
		}
		Runtime rt = Runtime.getRuntime();
		return rt.totalMemory() - rt.freeMemory();
	}
}
