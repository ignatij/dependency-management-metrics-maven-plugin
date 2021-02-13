package com.github.ignatij.stable_abstractions;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;

public class StableAbstractionsChecker {

    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String ABSTRACT_JAVA_KEYWORD = "abstract";
    private static final String INTERFACE_JAVA_KEYWORD = "interface";


    public static final BiFunction<Double, Double, Boolean> STABLE_ABSTRACTIONS_VIOLATION =
            (outerComponentMetric, innerComponentMetric) -> Double.compare(outerComponentMetric, innerComponentMetric) > 0;

    private final Map<MavenProject, List<String>> projectGraph;

    private Long abstractFiles = 0L;
    private Long regularFiles = 0L;

    public StableAbstractionsChecker(Map<MavenProject, List<String>> projectGraph) {
        this.projectGraph = projectGraph;
    }

    public Map<MavenProject, Double> calculateAbstractionLevel() throws IOException {
        Map<MavenProject, Double> abstractionLevel = new LinkedHashMap<>();
        for (MavenProject mavenProject : projectGraph.keySet()) {
            double abstraction = calculateAbstractionLevel(mavenProject);
            abstractionLevel.put(mavenProject, abstraction);
        }
        return abstractionLevel;
    }

    private Double calculateAbstractionLevel(MavenProject mavenProject) throws IOException {
        List<String> sourceDirectories = mavenProject.getCompileSourceRoots();
        abstractFiles = 0L;
        regularFiles = 0L;
        for (String sourceDirectory : sourceDirectories) {
            calculate(new File(sourceDirectory));
        }
        if (abstractFiles.equals(regularFiles) && abstractFiles.equals(0L)) {
            // not a Java project
            return (double) 0;
        }
        return (double) abstractFiles / (regularFiles + abstractFiles);
    }

    private void calculate(File root) throws IOException {
        Queue<File> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            File curr = queue.poll();
            File[] files = curr.listFiles();
            if (files != null && files.length != 0) {
                queue.addAll(Arrays.asList(files));
            } else {
                if (curr.getName().endsWith(JAVA_FILE_EXTENSION)) {
                    List<String> lines = Files.readAllLines(curr.toPath());
                    if (lines.stream().anyMatch(line -> line.contains(ABSTRACT_JAVA_KEYWORD) || line.contains(INTERFACE_JAVA_KEYWORD))) {
                        abstractFiles++;
                    } else {
                        regularFiles++;
                    }
                }
            }
        }
    }

}
