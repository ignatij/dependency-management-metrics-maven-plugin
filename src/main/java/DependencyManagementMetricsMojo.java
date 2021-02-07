import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.*;
import stable_abstractions.StableAbstractionsChecker;
import stable_dependencies.StableDependenciesChecker;
import statistic.Point;
import violation.ViolationChecker;
import violation.exception.StableAbstractionsPrincipleViolation;
import violation.exception.StableDependenciesPrincipleViolation;
import writer.MetricsFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static stable_abstractions.StableAbstractionsChecker.STABLE_ABSTRACTIONS_VIOLATION;
import static stable_dependencies.StableDependenciesChecker.STABLE_DEPENDENCIES_VIOLATION;

@Mojo(name = "check", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class DependencyManagementMetricsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${failOnViolation}", readonly = true)
    private Boolean failOnViolation = false;

    @Parameter(property = "output.file", defaultValue = "${project.build.directory}/dependency-metrics-result.txt", readonly = true)
    private File outputFile;

    @Component
    private ProjectBuilder projectBuilder;

    public void execute() throws MojoExecutionException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        try {
            if (!project.getModules().isEmpty()) {
                final Map<MavenProject, List<String>> projectGraph = ProjectUtil.createProjectGraph(buildingRequest, projectBuilder, project);
                final Map<MavenProject, Double> instabilityPerComponent = new StableDependenciesChecker(projectGraph).checkDependencies();
                final Map<MavenProject, Double> abstractionPerComponent = new StableAbstractionsChecker(projectGraph).calculateAbstractionLevel();

                List<Point> points = new ArrayList<>();
                for (MavenProject project : projectGraph.keySet()) {
                    points.add(new Point(project.getName(), instabilityPerComponent.get(project), abstractionPerComponent.get(project)));
                }
                new MetricsFileWriter(getLog()).write(points, outputFile);
                if (failOnViolation) {
                    new ViolationChecker(StableDependenciesPrincipleViolation.class).check(projectGraph, instabilityPerComponent, STABLE_DEPENDENCIES_VIOLATION);
                    new ViolationChecker(StableAbstractionsPrincipleViolation.class).check(projectGraph,
                            abstractionPerComponent,
                            STABLE_ABSTRACTIONS_VIOLATION
                    );
                }
            }
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Error while building project", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while reading the source files", e);
        }
    }
}
