import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import violation.exception.StableAbstractionsPrincipleViolation;
import violation.exception.StableDependenciesPrincipleViolation;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DependencyManagementMetricsMojoTest extends AbstractMojoTestCase {

    /**
     * {@inheritDoc}
     */
    protected void setUp()
            throws Exception {
        // required
        super.setUp();

    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown()
            throws Exception {
        // required
        super.tearDown();
    }

    /**
     * Simple two module project where one module is dependent of the another.
     *
     * @throws Exception if any
     */
    public void testSimpleTwoModuleProjectScenario()
            throws Exception {
        String twoModuleProjectDir = "src/test/resources/two-module-project/";
        DependencyManagementMetricsMojo myMojo = getMojo(twoModuleProjectDir);
        assertNotNull(myMojo);
        assertDoesNotThrow(myMojo::execute);
    }

    /**
     * A rather complex scenario of the following:
     * Module1, Module2, Module3 are depending on Module4.
     * Module4 is depending on Module5.
     * Module5 depends on Module6, Module7, Module8.
     * This is a clear violation of the stable dependencies principle, where a rather stable Module4 is dependent of
     * much more unstable Module5.
     *
     * @throws Exception if any
     */
    public void testWeakDependenciesWithException()
            throws Exception {
        // given
        String stableDependenciesPrincipleViolationProjectDir = "src/test/resources/weak-dependencies-weak-abstractions-with-exception/";
        DependencyManagementMetricsMojo myMojo = getMojo(stableDependenciesPrincipleViolationProjectDir);
        assertNotNull(myMojo);

        // when
        Throwable t = assertThrows(StableDependenciesPrincipleViolation.class, myMojo::execute);

        // then
        assertEquals("Component module4 is violating the stable dependencies principle", t.getMessage());
    }

    /**
     * A rather complex scenario of the following:
     * Module1, Module2, Module3 are depending on Module4.
     * Module4 is depending on Module5.
     * Module5 depends on Module6, Module7, Module8.
     * <p>
     * This is a clear violation of the stable dependencies principle, where a rather stable Module4 is dependent of
     * much more unstable Module5.
     * <p>
     * The configuration is set that no exception is thrown here.
     *
     * @throws Exception if any
     */
    public void testWeakDependenciesWithoutException()
            throws Exception {
        String stableDependenciesPrincipleViolationProjectDir = "src/test/resources/weak-dependencies-weak-abstractions/";
        DependencyManagementMetricsMojo myMojo = getMojo(stableDependenciesPrincipleViolationProjectDir);
        assertNotNull(myMojo);
        assertDoesNotThrow(myMojo::execute);
    }

    /**
     * A rather complex scenario of the following:
     * Module1, Module2, Module3 are depending on Module4.
     * Module4 is depending on Module5.
     * Module5 is not dependent of any component.
     * Module6 is depending on Module5, Module7, Module8.
     * <p>
     * Module5 abstraction level is lower than the abstraction level of Module4,
     * so here Module4 is clearly violating the Stable Abstraction Principle.
     * <p>
     * The configuration is set that no exception is thrown here.
     *
     * @throws Exception if any
     */
    public void testWeakAbstractionsWithoutException()
            throws Exception {
        String multiModuleStableDependenciesProject = "src/test/resources/stable-dependencies-weak-abstractions";
        DependencyManagementMetricsMojo mojo = getMojo(multiModuleStableDependenciesProject);
        assertNotNull(mojo);
        assertDoesNotThrow(mojo::execute);
    }

    /**
     * A rather complex scenario of the following:
     * Module1, Module2, Module3 are depending on Module4.
     * Module4 is depending on Module5.
     * Module5 is not dependent of any component.
     * Module6 is depending on Module5, Module7, Module8.
     * <p>
     * Module5 abstraction level is lower than the abstraction level of Module4,
     * so here Module4 is clearly violating the Stable Abstraction Principle.
     * <p>
     *
     * @throws Exception if any
     */
    public void testWeakAbstractionsWithException()
            throws Exception {
        // given
        String multiModuleStableDependenciesProject = "src/test/resources/stable-dependencies-weak-abstractions-with-exception";
        DependencyManagementMetricsMojo mojo = getMojo(multiModuleStableDependenciesProject);
        assertNotNull(mojo);

        // when
        Throwable exception = assertThrows(StableAbstractionsPrincipleViolation.class, mojo::execute);

        // then
        assertEquals("Component module4 is violating the stable abstraction principle", exception.getMessage());
    }

    /**
     * A rather complex scenario of the following:
     * Module1, Module2, Module3 are depending on Module4.
     * Module4 is depending on Module5.
     * Module5 is not dependent of any component.
     * Module6 is depending on Module5, Module7, Module8.
     * <p>
     * Module5 abstraction level is higher than the abstraction level of Module4,
     * which makes the Module5 the most stable and most abstract component.
     * <p>
     *
     * @throws Exception if any
     */
    public void testStableDependenciesAndStableAbstractions() throws Exception {
        String project = "src/test/resources/stable-dependencies-stable-abstractions";
        DependencyManagementMetricsMojo mojo = getMojo(project);
        assertNotNull(mojo);
        assertDoesNotThrow(mojo::execute);
    }

    private DependencyManagementMetricsMojo getMojo(String projectDir) throws Exception {
        File pom = getTestFile(projectDir);
        MavenProject mavenProject = readMavenProject(pom);
        MavenSession mavenSession = newMavenSession(mavenProject);
        MojoExecution mojoExecution = newMojoExecution("check");
        return (DependencyManagementMetricsMojo) lookupConfiguredMojo(mavenSession, mojoExecution);
    }

    protected MavenProject readMavenProject(File basedir)
            throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());
        MavenProject project = lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        assertNotNull(project);
        return project;
    }

}
