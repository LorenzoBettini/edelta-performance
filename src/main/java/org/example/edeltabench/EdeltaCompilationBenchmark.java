package org.example.edeltabench;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/** Fresh resources + Ecore loading + Edelta parse/derived state + validation. */
@State(Scope.Thread)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 8, time = 2)
@Measurement(iterations = 5, time = 2)
public class EdeltaCompilationBenchmark {
	@Param({"normal", "nop"})
	public String mode;

	@Param({"50", "250", "1000"})
	public int classes;

	@Param({"0", "10", "50"})
	public int references;

	@Param({"5", "20"})
	public int additions;

	@Param({"read", "write"})
	public String referenceKind;

	@Param({"references-first"})
	public String order;

	private EdeltaCompilerHarness compiler;
	private BenchmarkFixtures.Inputs fixture;

	@Setup(Level.Trial)
	public void prepare() {
		var workload = new BenchmarkWorkload(classes, references, additions, 2,
				ReferenceKind.parse(referenceKind), StatementOrder.parse(order));
		fixture = BenchmarkFixtures.generate(workload);
		compiler = new EdeltaCompilerHarness(BenchmarkMode.parse(mode));
		// All correctness checks occur outside the measured invocation.
		BenchmarkSanity.verify(compiler.compileAndValidate(fixture),
				workload, BenchmarkMode.parse(mode));
	}

	@Benchmark
	public void compileAndValidate(Blackhole blackhole) {
		blackhole.consume(compiler.compileAndValidate(fixture));
	}
}
