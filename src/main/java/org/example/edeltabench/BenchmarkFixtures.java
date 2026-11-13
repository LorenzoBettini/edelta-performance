package org.example.edeltabench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

/** Synthetic, deterministic, valid benchmarks; construction is not timed. */
public final class BenchmarkFixtures {
	private BenchmarkFixtures() { }

	public record Inputs(byte[] ecore, byte[] edelta) { }

	public static Inputs generate(BenchmarkWorkload size) {
		return new Inputs(serializeModel(size), source(size).getBytes(StandardCharsets.UTF_8));
	}

	static byte[] serializeModel(BenchmarkWorkload size) {
		var f = EcoreFactory.eINSTANCE;
		EPackage pkg = f.createEPackage();
		pkg.setName("bench");
		pkg.setNsPrefix("bench");
		pkg.setNsURI("http://example.org/edelta-benchmark/bench");

		EClass[] classes = new EClass[size.classes()];
		for (int i = 0; i < classes.length; i++) {
			EClass cls = f.createEClass();
			cls.setName(className(i));
			for (int j = 0; j < size.attributesPerClass(); j++) {
				var attr = f.createEAttribute();
				attr.setName("attribute" + j);
				attr.setEType(EcorePackage.Literals.ESTRING);
				cls.getEStructuralFeatures().add(attr);
			}
			classes[i] = cls;
			pkg.getEClassifiers().add(cls);
		}
		// Include a containment-independent cross-reference for each class.
		for (int i = 0; i < classes.length; i++) {
			var ref = f.createEReference();
			ref.setName("next");
			ref.setEType(classes[(i + 1) % classes.length]);
			classes[i].getEStructuralFeatures().add(ref);
		}
		Resource resource = new EcoreResourceFactoryImpl().createResource(URI.createURI("bench.ecore"));
		resource.getContents().add(pkg);
		try (var out = new ByteArrayOutputStream()) {
			resource.save(out, Collections.emptyMap());
			return out.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("Could not serialize synthetic Ecore", e);
		}
	}

	static String className(int n) {
		return "C" + String.format(java.util.Locale.ROOT, "%05d", n);
	}

	static String source(BenchmarkWorkload size) {
		var b = new StringBuilder();
		b.append("package benchprogram\n\nmetamodel \"bench\"\n\n")
			.append("modifyEcore evolve epackage bench {\n");
		switch (size.order()) {
			case REFERENCES_FIRST -> {
				for (int i = 0; i < size.references(); i++) {
					appendReference(b, size, i);
				}
				for (int i = 0; i < size.additions(); i++) {
					appendAddition(b, i);
				}
			}
			case ADDITIONS_FIRST -> {
				for (int i = 0; i < size.additions(); i++) {
					appendAddition(b, i);
				}
				for (int i = 0; i < size.references(); i++) {
					appendReference(b, size, i);
				}
			}
			case INTERLEAVED -> {
				// Distribute additions among the gaps between references, so
				// every addition precedes another ecoreref if there are >=2.
				// This deliberately tests cache invalidation between snapshots.
				if (size.references() >= 2) {
					int gaps = size.references() - 1;
					for (int i = 0; i < size.references(); i++) {
						appendReference(b, size, i);
						if (i < gaps) {
							int from = (int) ((long) i * size.additions() / gaps);
							int until = (int) ((long) (i + 1) * size.additions() / gaps);
							for (int a = from; a < until; a++) {
								appendAddition(b, a);
							}
						}
					}
				} else {
					// With <=1 reference there can be no internal gap.
					for (int i = 0; i < size.references(); i++) {
						appendReference(b, size, i);
					}
					for (int i = 0; i < size.additions(); i++) {
						appendAddition(b, i);
					}
				}
			}
		}
		b.append("}\n");
		return b.toString();
	}

	private static void appendReference(StringBuilder b, BenchmarkWorkload size, int i) {
		// All references resolve to original EClasses in NORMAL and NOP modes.
		b.append("  ecoreref(").append(className(i % size.classes()));
		if (size.referenceKind() == ReferenceKind.READ) {
			b.append(").name\n");
		} else {
			b.append(").abstract = true\n");
		}
	}

	private static void appendAddition(StringBuilder b, int i) {
		b.append("  addNewEClass(\"Added")
			.append(String.format(java.util.Locale.ROOT, "%05d", i))
			.append("\")\n");
	}
}
