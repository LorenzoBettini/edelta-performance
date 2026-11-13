package org.example.edeltabench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;

/** Regression checks for programs importing the built-in Ecore metamodel. */
class RealExampleEcoreSupportTest {
	@Test
	void ecoreIsLoadedOnlyForProgramsImportingIt() {
		var compiler = new EdeltaCompilerHarness(BenchmarkMode.NORMAL);
		var withEcore = compiler.compileAndValidate(RealExampleFixtures.get("catalog-personlist"));
		var resource = withEcore.resourceSet().getResource(
				URI.createURI("EcoreForTests.ecore"), false);
		assertNotNull(resource);
		assertEquals(1, resource.getContents().size());
		var ecore = (EPackage) resource.getContents().get(0);
		assertEquals("ecore", ecore.getName());
		assertNotNull(ecore.getEClassifier("EString"));
		assertNotNull(ecore.getEClassifier("EInt"));
		assertEquals(0, withEcore.errors(), () -> withEcore.issues().toString());

		var withoutEcore = compiler.compileAndValidate(RealExampleFixtures.get("library-v1-v2"));
		assertNull(withoutEcore.resourceSet().getResource(
				URI.createURI("EcoreForTests.ecore"), false));
		assertEquals(0, withoutEcore.errors(), () -> withoutEcore.issues().toString());
	}

	@Test
	void otherPreviouslyFailingEcoreProgramsValidate() {
		var compiler = new EdeltaCompilerHarness(BenchmarkMode.NORMAL);
		for (String id : new String[] {
				"library-v3-v4", "catalog-walkthrough", "extract-class",
				"extract-superclass", "refactorings-personlist" }) {
			var result = compiler.compileAndValidate(RealExampleFixtures.get(id));
			assertTrue(result.issues().stream().noneMatch(
					issue -> issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR),
					() -> "Validation errors for " + id + ": " + result.issues());
		}
	}
}
