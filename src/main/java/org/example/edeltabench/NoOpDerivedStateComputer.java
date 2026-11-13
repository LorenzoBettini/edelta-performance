package org.example.edeltabench;

import com.google.inject.Singleton;
import edelta.edelta.EdeltaProgram;
import edelta.resource.EdeltaDerivedStateComputer;

/**
 * Ablation used only for measurement: normal Xbase/JVM-model inference,
 * metamodel copying, and reference recording remain in place.
 * Only the interpretation step is skipped.
 */
@Singleton
public class NoOpDerivedStateComputer extends EdeltaDerivedStateComputer {
	@Override
	protected void runInterpreter(EdeltaProgram program) {
		// Deliberate no-op.
	}
}
