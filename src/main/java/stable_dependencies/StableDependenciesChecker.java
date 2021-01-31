package stable_dependencies;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import violation.ViolationChecker;
import violation.exception.StableDependenciesPrincipleViolation;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StableDependenciesChecker {


    private static final BiFunction<Double, Double, Boolean> STABLE_DEPENDENCIES_VIOLATION =
            (outerMetric, innerMetric) -> Double.compare(outerMetric, innerMetric) < 0;

    private final Map<MavenProject, List<String>> projectGraph;

    private final boolean failOnViolation;

    public StableDependenciesChecker(Map<MavenProject, List<String>> projectGraph, boolean failOnViolation) {
        this.projectGraph = projectGraph;
        this.failOnViolation = failOnViolation;
    }

    public Map<MavenProject, Double> checkDependencies() throws MojoExecutionException {
        Map<MavenProject, Double> instabilityPerComponent = calculateInstability();
        if (failOnViolation) {
            new ViolationChecker(StableDependenciesPrincipleViolation.class).check(projectGraph, instabilityPerComponent, STABLE_DEPENDENCIES_VIOLATION);
        }
        return instabilityPerComponent;
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
