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

/**
 * Original-source case studies, NORMAL only. The NOP configuration is not
 * semantically equivalent for programs using the evolving metamodel state.
 * Use -p example=case1,case2 for the cases that pass the manual smoke check.
 */
@State(Scope.Thread)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 8, time = 2)
@Measurement(iterations = 5, time = 2)
public class RealExampleCompilationBenchmark {
	@Param({"catalog-walkthrough"})
	public String example;

	private EdeltaCompilerHarness compiler;
	private RealExampleFixtures.Example fixture;

	@Setup(Level.Trial)
	public void prepare() {
		compiler = new EdeltaCompilerHarness(BenchmarkMode.NORMAL);
		fixture = RealExampleFixtures.get(example);
		RealExampleSmokeMain.verify(fixture, compiler.compileAndValidate(fixture));
	}

	@Benchmark
	public void compileAndValidate(Blackhole blackhole) {
		blackhole.consume(compiler.compileAndValidate(fixture));
	}
}
