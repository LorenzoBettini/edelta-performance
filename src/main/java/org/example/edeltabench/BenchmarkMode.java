package org.example.edeltabench;

public enum BenchmarkMode {
	NORMAL,
	NOP;

	public static BenchmarkMode parse(String value) {
		return switch (value.toLowerCase(java.util.Locale.ROOT)) {
			case "normal" -> NORMAL;
			case "nop" -> NOP;
			default -> throw new IllegalArgumentException("Expected normal or nop, got: " + value);
		};
	}
}
