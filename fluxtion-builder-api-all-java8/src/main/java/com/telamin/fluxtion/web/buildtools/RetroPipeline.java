/*
 * Maven post-shade orchestrator for the browser-bundle bytecode pipeline.
 *
 *   shaded.jar
 *     -> unpack into temp dir
 *     -> fork JDK 8 + Retrolambda (rewrites invokedynamic+LambdaMetafactory
 *        sites into real anonymous-class .class files; CheerpJ Java 8 mode
 *        cannot re-resolve invokedynamic in JAR-shipped classes on the
 *        second hit, so this transform is mandatory)
 *     -> RetroPatch (in-process; injects writeReplace() into every
 *        Lambda$N class implementing a LambdaReflection$Serializable*
 *        interface, since Retrolambda does not — issue #195)
 *     -> repack to final.jar
 *
 * Invoked from pom.xml via exec-maven-plugin. Required system properties:
 *   jdk8.home          path to a JDK 8 installation (Retrolambda 2.5.7
 *                      uses LambdaMetafactory internals that broke on JDK 9+)
 *   retrolambda.jar    path to retrolambda-2.5.7.jar in the local repo
 *
 * Args (in order):
 *   inputJar    path to the shaded JAR produced by maven-shade-plugin
 *   outputJar   path where the post-processed JAR should be written
 *   overlays... optional JARs whose original class files replace the
 *               Retrolambda output (used for javac-facing runtime APIs)
 *
 * Retrolambda's resolution classpath is reused from this process's own
 * java.class.path — exec-maven-plugin populates that with all
 * compile-scope deps, which is exactly what Retrolambda needs to
 * resolve method handles for invokedynamic call sites referencing
 * classes that were excluded from the shaded JAR (Spring, Kryo, etc.).
 */

package com.telamin.fluxtion.web.buildtools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class RetroPipeline {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: RetroPipeline <inputJar> <outputJar>");
            System.exit(2);
        }
        Path inputJar = Paths.get(args[0]);
        Path outputJar = Paths.get(args[1]);
        // exec-maven-plugin populates java.class.path with all compile-scope deps
        String resolutionClasspath = System.getProperty("java.class.path", "");

        String jdk8Home = required("jdk8.home");
        Path retrolambdaJar = Paths.get(required("retrolambda.jar"));

        if (!Files.isDirectory(Paths.get(jdk8Home))) {
            throw new IllegalStateException("jdk8.home does not exist: " + jdk8Home);
        }
        if (!Files.isRegularFile(retrolambdaJar)) {
            throw new IllegalStateException("retrolambda.jar does not exist: " + retrolambdaJar);
        }
        if (!Files.isRegularFile(inputJar)) {
            throw new IllegalStateException("input jar does not exist: " + inputJar);
        }

        Path workDir = Files.createTempDirectory("fluxtion-retro-");
        Path inputDir = workDir.resolve("in");
        Path outputDir = workDir.resolve("out");
        Files.createDirectories(inputDir);
        Files.createDirectories(outputDir);
        try {
            log("step 1/5 — unpacking " + inputJar.getFileName() + " into " + inputDir);
            unzip(inputJar, inputDir);

            log("step 2/5 — running Retrolambda under JDK 8");
            // Run via -jar only — Retrolambda's Main self-attaches the agent.
            // Adding an explicit -javaagent: flag double-registers the agent
            // callback, so every JDK lambda save fires twice and pushes to
            // LambdaReifier's bounded(1) deque from the second hit, surfacing
            // as `Deque full` on classes whose <clinit> creates lambdas (e.g.
            // DataFlow.NULL_EVENTHANDLER).
            String fullClasspath = inputDir + File.pathSeparator + resolutionClasspath;
            ProcessBuilder pb = new ProcessBuilder(
                    jdk8Home + "/bin/java",
                    "-Dretrolambda.bytecodeVersion=52",
                    "-Dretrolambda.defaultMethods=true",
                    "-Dretrolambda.inputDir=" + inputDir,
                    "-Dretrolambda.outputDir=" + outputDir,
                    "-Dretrolambda.classpath=" + fullClasspath,
                    "-jar", retrolambdaJar.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.io.InputStream in = p.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    System.out.write(buf, 0, n);
                }
            }
            int rc = p.waitFor();
            if (rc != 0) {
                throw new RuntimeException("Retrolambda failed with exit code " + rc);
            }

            // Retrolambda only processes .class files; non-class resources
            // (META-INF, .properties, Velocity .template files, etc.) stay
            // behind in inputDir and must be carried into outputDir before
            // we repack — otherwise the final JAR is missing all resources
            // and the manifest.
            copyResources(inputDir, outputDir);

            log("step 3/5 — injecting writeReplace via RetroPatch");
            int[] counts = RetroPatch.transformDir(outputDir);
            log("  scanned=" + counts[0] + " patched=" + counts[1] + " skipped=" + counts[2]);

            log("step 4/5 — restoring clean javac-facing class files");
            for (int i = 2; i < args.length; i++) {
                Path overlayJar = Paths.get(args[i]);
                if (!Files.isRegularFile(overlayJar)) {
                    throw new IllegalStateException("overlay jar does not exist: " + overlayJar);
                }
                overlayClasses(overlayJar, outputDir);
                log("  overlaid classes from " + overlayJar.getFileName());
            }

            log("step 5/5 — repacking to " + outputJar.getFileName());
            zipTo(outputDir, outputJar);
        } finally {
            deleteTree(workDir);
        }
        log("done — final jar: " + outputJar);
    }

    static String required(String key) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("required system property -D" + key + " not set");
        }
        return v;
    }

    static void copyResources(Path src, Path dst) throws IOException {
        Files.walkFileTree(src, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) return java.nio.file.FileVisitResult.CONTINUE;
                Path rel = src.relativize(file);
                Path target = dst.resolve(rel);
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    static void unzip(Path zip, Path dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                Path out = dest.resolve(e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream o = Files.newOutputStream(out)) {
                        zis.transferTo(o);
                    }
                }
            }
        }
    }

    static void overlayClasses(Path zip, Path dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (e.isDirectory()
                        || !name.endsWith(".class")
                        || name.equals("module-info.class")
                        || name.startsWith("META-INF/versions/")) {
                    continue;
                }
                Path out = dest.resolve(name);
                Files.createDirectories(out.getParent());
                try (OutputStream o = Files.newOutputStream(out)) {
                    zis.transferTo(o);
                }
            }
        }
    }

    static void zipTo(Path dir, Path zip) throws IOException {
        Files.createDirectories(zip.getParent());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            Files.walkFileTree(dir, new java.nio.file.SimpleFileVisitor<Path>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String name = dir.relativize(file).toString().replace(File.separatorChar, '/');
                    zos.putNextEntry(new ZipEntry(name));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        }
    }

    static void deleteTree(Path dir) {
        if (!Files.exists(dir)) return;
        try {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    static void log(String msg) {
        System.out.println("[retro-pipeline] " + msg);
    }

}
