package org.example.edeltabench;

/**
 * Deterministic fixture parameters; generation is outside JMH timing.
 * Older constructors preserve the legacy references-first ordering.
 */
public record BenchmarkWorkload(int classes, int references, int additions,
		int attributesPerClass, ReferenceKind referenceKind, StatementOrder order) {
	public BenchmarkWorkload {
		if (classes < 1 || references < 0 || additions < 0 || attributesPerClass < 0
				|| referenceKind == null || order == null || (references == 0 && additions == 0)) {
			throw new IllegalArgumentException("Invalid workload: " + classes + ", "
					+ references + ", " + additions + ", " + attributesPerClass
					+ ", " + referenceKind + ", " + order);
		}
	}

	public BenchmarkWorkload(int classes, int references, int additions,
			int attributesPerClass, ReferenceKind referenceKind) {
		this(classes, references, additions, attributesPerClass, referenceKind,
				StatementOrder.REFERENCES_FIRST);
	}

	public BenchmarkWorkload(int classes, int references, int additions,
			int attributesPerClass) {
		this(classes, references, additions, attributesPerClass, ReferenceKind.WRITE,
				StatementOrder.REFERENCES_FIRST);
	}
}
