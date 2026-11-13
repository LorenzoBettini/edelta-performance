package org.example.edeltabench;

import java.util.Locale;

/** Order in which a fixture evaluates references and creates new classes. */
public enum StatementOrder {
	REFERENCES_FIRST("references-first"),
	ADDITIONS_FIRST("additions-first"),
	INTERLEAVED("interleaved");

	private final String parameter;

	StatementOrder(String parameter) {
		this.parameter = parameter;
	}

	public String parameter() {
		return parameter;
	}

	public static StatementOrder parse(String input) {
		for (var order : values()) {
			if (order.parameter.equals(input.toLowerCase(Locale.ROOT))) {
				return order;
			}
		}
		throw new IllegalArgumentException("Unknown statement order: " + input);
	}
}
