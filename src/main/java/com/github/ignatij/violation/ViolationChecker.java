package com.github.ignatij.violation;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import com.github.ignatij.violation.exception.StableAbstractionsPrincipleViolation;
import com.github.ignatij.violation.exception.StableDependenciesPrincipleViolation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ViolationChecker {

    private final Map<Class<? extends MojoExecutionException>, Function<String, MojoExecutionException>> exceptionMap = Map.of(
            StableAbstractionsPrincipleViolation.class, StableAbstractionsPrincipleViolation::new,
            StableDependenciesPrincipleViolation.class, StableDependenciesPrincipleViolation::new
    );

    private final Class<? extends MojoExecutionException> exceptionType;

    public ViolationChecker(Class<? extends MojoExecutionException> exceptionType) {
        this.exceptionType = exceptionType;
    }

    public void check(Map<MavenProject, List<String>> projectGraph,
                      Map<MavenProject, Double> metricPerComponent,
                      BiFunction<Double, Double, Boolean> calculateViolation) throws MojoExecutionException {
        Set<MavenProject> visited = new LinkedHashSet<>();
        Queue<MavenProject> queue = new ArrayDeque<>();
        List<MavenProject> modules = new ArrayList<>(projectGraph.keySet());

        while (visited.size() != modules.size()) {
            MavenProject root = modules.stream().filter(module -> !visited.contains(module)).findFirst().orElse(null);
            visited.add(root);
            queue.add(root);
            while (!queue.isEmpty()) {
                MavenProject curr = queue.poll();
                List<MavenProject> dependencies = projectGraph.get(curr)
                        .stream()
                        .map(dependency -> getDependencyModule(modules, dependency))
                        .collect(Collectors.toList());

                dependencies.stream().filter(next -> !visited.contains(next)).forEach(next -> {
                    visited.add(next);
                    queue.add(next);
                });

                if (isPrincipleViolated(curr, dependencies, metricPerComponent, calculateViolation)) {
                    throw exceptionMap.get(exceptionType).apply(curr.getName());
                }
            }
        }
    }

    private MavenProject getDependencyModule(List<MavenProject> modules, String dependency) {
        return modules.stream().filter(module -> module.getArtifactId().equals(dependency)).findFirst().orElse(null);
    }

    private boolean isPrincipleViolated(MavenProject outerComponent,
                                        List<MavenProject> innerComponents,
                                        Map<MavenProject, Double> metricsPerComponent,
                                        BiFunction<Double, Double, Boolean> calculateViolation) {
        return innerComponents.stream().anyMatch(innerComponent -> principleViolated(outerComponent, innerComponent, metricsPerComponent, calculateViolation));
    }

    private boolean principleViolated(MavenProject outerComponent,
                                      MavenProject innerComponent,
                                      Map<MavenProject, Double> metricsPerComponent,
                                      BiFunction<Double, Double, Boolean> calculateViolation) {
        return calculateViolation.apply(metricsPerComponent.get(outerComponent), metricsPerComponent.get(innerComponent));
    }

}
