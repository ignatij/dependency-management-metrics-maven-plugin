import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.*;
import stable_abstractions.StableAbstractionsChecker;
import stable_dependencies.StableDependenciesChecker;
import statistic.Point;
import statistic.StatisticUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mojo(name = "check", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class DependencyManagementMetricsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${failOnViolation}", readonly = true)
    private Boolean failOnViolation = false;

    @Component
    private ProjectBuilder projectBuilder;

    public void execute() throws MojoExecutionException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        try {
            if (!project.getModules().isEmpty()) {
                final Map<MavenProject, List<String>> projectGraph = ProjectUtil.createProjectGraph(buildingRequest, projectBuilder, project);
                final Map<MavenProject, Double> instabilityPerComponent = new StableDependenciesChecker(projectGraph, failOnViolation).checkDependencies();
                final Map<MavenProject, Double> abstractionPerComponent = new StableAbstractionsChecker(projectGraph, failOnViolation).calculateAbstractionLevel();

                List<Point> points = new ArrayList<>();
                for (MavenProject project : projectGraph.keySet()) {
                    System.out.println(project.getName() + " " + instabilityPerComponent.get(project) + ", " + abstractionPerComponent.get(project));
                    points.add(new Point(instabilityPerComponent.get(project), abstractionPerComponent.get(project)));
                }
                System.out.println("MEAN: " + StatisticUtil.mean(points));
                System.out.println("VARIANCE: " + StatisticUtil.variance(points));
                System.out.println("STANDARD DEVIATION: " + StatisticUtil.standardDeviation(points));
            }
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Error while building project", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while reading the source files", e);
        }
    }
}
