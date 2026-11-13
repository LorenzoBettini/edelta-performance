package org.example.edeltabench;

import java.util.Locale;

/** Whether the generated ecoreref expressions mutate their target. */
public enum ReferenceKind {
	READ,
	WRITE;

	public static ReferenceKind parse(String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "read", "readonly" -> READ;
			case "write", "mutating" -> WRITE;
			default -> throw new IllegalArgumentException(
					"Expected read or write, got: " + value);
		};
	}

	public String parameter() {
		return name().toLowerCase(Locale.ROOT);
	}
}
