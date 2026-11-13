package org.example.edeltabench;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.xtext.ecore.EcoreSupportStandaloneSetup;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;

import edelta.EdeltaRuntimeModule;
import edelta.EdeltaStandaloneSetup;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * One injectable headless compiler configuration, reused across benchmark iterations.
 * Parsing/loading and validation happen within compileAndValidate(); initialization,
 * test data generation, and injector construction are deliberately outside it.
 */
public final class EdeltaCompilerHarness {
	private final Injector injector;
	private final Provider<XtextResourceSet> resourceSets;
	private final IResourceValidator validator;
	private final EdeltaDerivedStateHelper stateHelper;
	private final IResourceFactory edeltaResourceFactory;

	public EdeltaCompilerHarness(BenchmarkMode mode) {
		// Avoid Edelta's editor-safety timeout truncating heavy experiments.
		// This is only a timeout setting, not part of timed work.
		if (System.getProperty("edelta.interpreter.timeout") == null) {
			System.setProperty("edelta.interpreter.timeout", "120000");
		}
		// Explicit classloader binding matches Edelta's standalone testing pattern.
		EdeltaRuntimeModule runtimeModule = mode == BenchmarkMode.NORMAL
				? new EdeltaRuntimeModule() {
					@Override public ClassLoader bindClassLoaderToInstance() {
						return EdeltaCompilerHarness.class.getClassLoader();
					}
				  }
				: new NoOpEdeltaRuntimeModule() {
					@Override public ClassLoader bindClassLoaderToInstance() {
						return EdeltaCompilerHarness.class.getClassLoader();
					}
				  };
		this.injector = new EdeltaStandaloneSetup() {
			@Override
			public Injector createInjector() {
				return Guice.createInjector(runtimeModule);
			}

			@Override
			public void register(Injector injector) {
				EcoreSupportStandaloneSetup.setup();
				super.register(injector);
			}
		}.createInjectorAndDoEMFRegistration();
		this.resourceSets = injector.getProvider(XtextResourceSet.class);
		this.validator = injector.getInstance(IResourceValidator.class);
		this.stateHelper = injector.getInstance(EdeltaDerivedStateHelper.class);
		this.edeltaResourceFactory = injector.getInstance(IResourceFactory.class);
	}

	public CompilationResult compileAndValidate(BenchmarkFixtures.Inputs inputs) {
		return compileAndValidate(inputs, "bench.ecore", "synthetic.edelta", "bench");
	}

	/** Same compilation path for an independently supplied Ecore/Edelta pair. */
	public CompilationResult compileAndValidate(BenchmarkFixtures.Inputs inputs,
			String modelFile, String programFile, String expectedPackageName) {
		// Never reuse a mutable resource set or a previously derived state.
		XtextResourceSet resources = resourceSets.get();
		resources.setClasspathURIContext(EdeltaCompilerHarness.class.getClassLoader());
		resources.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("ecore", new EcoreResourceFactoryImpl());
		// Use the resource factory of this injector, not a global EMF mapping that
		// might have been registered by another NORMAL/NOP benchmark trial.
		resources.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("edelta", edeltaResourceFactory);

		Resource model = resources.createResource(URI.createURI(modelFile));
		Resource program = resources.createResource(URI.createURI(programFile));
		if (model == null || program == null) {
			throw new IllegalStateException("Required Ecore/Edelta resource factory is unavailable");
		}
		try {
			model.load(new ByteArrayInputStream(inputs.ecore()), Collections.emptyMap());
			program.load(new ByteArrayInputStream(inputs.edelta()), Collections.emptyMap());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load benchmark fixture", e);
		}

		// Forces parsing/linking, JVM/derived-state installation and validator checks.
		// IMPORTANT: do not measure only program.load(), since Xtext can be lazy.
		List<Issue> issues = validator.validate(program, CheckMode.ALL, CancelIndicator.NullImpl);
		var copies = stateHelper.getCopiedEPackagesMap(program);
		EPackage copied = copies.values().stream()
				.filter(p -> expectedPackageName.equals(p.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Edelta derived-state computation did not copy " + expectedPackageName));
		// Keep all loaded resources reachable for optional post-GC heap measurement.
		return new CompilationResult(resources, program, copied, issues);
	}


	/** Same headless processing for the unchanged original multi-Ecore examples. */
	public CompilationResult compileAndValidate(RealExampleFixtures.Example example) {
		return compileAndValidate(example.programPath(), example.program(),
				example.models(), example.expectedPackage(), example.importsEcore());
	}

	private CompilationResult compileAndValidate(String programFile, byte[] edelta,
			List<RealExampleFixtures.ModelResource> models, String expectedPackageName,
			boolean importsEcore) {
		// Never reuse a mutable resource set or a previously derived state.
		XtextResourceSet resources = resourceSets.get();
		resources.setClasspathURIContext(EdeltaCompilerHarness.class.getClassLoader());
		resources.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("ecore", new EcoreResourceFactoryImpl());
		resources.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("edelta", edeltaResourceFactory);

		// EcoreSupportStandaloneSetup registers Ecore infrastructure, but it does
		// not expose the EPackage named "ecore" as a loaded, indexable Ecore model
		// in this fresh ResourceSet. Edelta's own tests load EcoreForTests.ecore
		// before parsing programs containing `metamodel "ecore"`.
		// Load it only for those programs so unrelated measurements are unchanged.
		if (importsEcore) {
			loadEcoreMetamodel(resources);
		}

		// Preserve original project-relative paths, including relative Ecore hrefs.
		// Pre-create every Ecore resource before loading any of them, so cross-file
		// Ecore references can resolve within the same fresh ResourceSet.
		List<Resource> modelResources = new ArrayList<>();
		for (var model : models) {
			Resource r = resources.createResource(URI.createURI(model.path()));
			if (r == null) {
				throw new IllegalStateException("No Ecore factory for " + model.path());
			}
			modelResources.add(r);
		}
		try {
			for (int i = 0; i < models.size(); i++) {
				modelResources.get(i).load(
						new ByteArrayInputStream(models.get(i).contents()), Collections.emptyMap());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load original Ecore fixtures", e);
		}
		for (Resource model : modelResources) {
			for (var object : model.getContents()) {
				if (object instanceof EPackage ePackage && ePackage.getNsURI() != null) {
					// Only the per-iteration ResourceSet registry is modified.
					resources.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
				}
			}
		}

		Resource program = resources.createResource(URI.createURI(programFile));
		if (program == null) {
			throw new IllegalStateException("No Edelta resource factory for " + programFile);
		}
		try {
			program.load(new ByteArrayInputStream(edelta), Collections.emptyMap());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load original Edelta program", e);
		}

		// Force parsing/linking, JVM/derived-state installation and validation.
		List<Issue> issues = validator.validate(program, CheckMode.ALL, CancelIndicator.NullImpl);
		var copies = stateHelper.getCopiedEPackagesMap(program);
		EPackage copied = copies.values().stream()
				.filter(p -> expectedPackageName.equals(p.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Edelta derived-state computation did not copy " + expectedPackageName
						+ " (" + programFile + ")"));
		// Keep all loaded resources reachable for optional post-GC heap measurement.
		return new CompilationResult(resources, program, copied, issues);
	}

	/** Expose Edelta's test Ecore metamodel to Xtext linking as a normal resource. */
	private static void loadEcoreMetamodel(XtextResourceSet resources) {
		Resource ecoreResource = resources.createResource(URI.createURI("EcoreForTests.ecore"));
		if (ecoreResource == null) {
			throw new IllegalStateException("No Ecore resource factory for EcoreForTests.ecore");
		}
		try {
			ecoreResource.load(new ByteArrayInputStream(RealExampleFixtures.ecoreSupportModel()),
					Collections.emptyMap());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load EcoreForTests.ecore", e);
		}
		if (ecoreResource.getContents().size() != 1
				|| !(ecoreResource.getContents().get(0) instanceof EPackage ecorePackage)
				|| !"ecore".equals(ecorePackage.getName())) {
			throw new IllegalStateException("Expected the built-in EPackage named ecore");
		}
		// Do NOT register this independent Ecore-file instance under EcorePackage.eNS_URI:
		// EMF's canonical EcorePackage.eINSTANCE must remain the runtime metamodel.
	}

	public record CompilationResult(XtextResourceSet resourceSet, Resource program,
									EPackage interpretedPackage, List<Issue> issues) {
		public long errors() {
			return issues.stream().filter(i -> i.getSeverity()
					== org.eclipse.xtext.diagnostics.Severity.ERROR).count();
		}
	}
}
