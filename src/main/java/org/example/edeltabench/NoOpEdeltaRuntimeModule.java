package org.example.edeltabench;

import edelta.EdeltaRuntimeModule;
import org.eclipse.xtext.resource.IDerivedStateComputer;

public class NoOpEdeltaRuntimeModule extends EdeltaRuntimeModule {
	@Override
	public Class<? extends IDerivedStateComputer> bindIDerivedStateComputer() {
		return NoOpDerivedStateComputer.class;
	}
}
