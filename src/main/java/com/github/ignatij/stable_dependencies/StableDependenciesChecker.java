package com.github.ignatij.stable_dependencies;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StableDependenciesChecker {


    public static final BiFunction<Double, Double, Boolean> STABLE_DEPENDENCIES_VIOLATION =
            (outerMetric, innerMetric) -> Double.compare(outerMetric, innerMetric) < 0;

    private final Map<MavenProject, List<String>> projectGraph;

    public StableDependenciesChecker(Map<MavenProject, List<String>> projectGraph) {
        this.projectGraph = projectGraph;
    }

    public Map<MavenProject, Double> checkDependencies() throws MojoExecutionException {
        return calculateInstability();
    }

    private Map<MavenProject, Double> calculateInstability() {
        return projectGraph.keySet()
                .stream()
                .collect(Collectors.toMap(Function.identity(), this::calculateInstability));
    }

    private Double calculateInstability(MavenProject mavenProject) {
        long numberOfComponentsThatDependOnModule = projectGraph
                .values()
                .stream()
                .filter(dependencies -> dependencies.contains(mavenProject.getName()))
                .count();
        long numberOfComponentsThatModuleDependsOn = projectGraph.get(mavenProject).size();

        return (double) numberOfComponentsThatModuleDependsOn / (numberOfComponentsThatDependOnModule + numberOfComponentsThatModuleDependsOn);
    }

}
