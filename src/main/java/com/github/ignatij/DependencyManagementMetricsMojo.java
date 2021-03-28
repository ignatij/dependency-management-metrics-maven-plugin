package com.github.ignatij;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.github.ignatij.stable_abstractions.StableAbstractionsChecker;
import com.github.ignatij.stable_dependencies.StableDependenciesChecker;
import com.github.ignatij.statistic.Point;
import com.github.ignatij.violation.ViolationChecker;
import com.github.ignatij.violation.exception.StableAbstractionsPrincipleViolation;
import com.github.ignatij.violation.exception.StableDependenciesPrincipleViolation;
import com.github.ignatij.writer.MetricsFileWriter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.ignatij.stable_abstractions.StableAbstractionsChecker.STABLE_ABSTRACTIONS_VIOLATION;
import static com.github.ignatij.stable_dependencies.StableDependenciesChecker.STABLE_DEPENDENCIES_VIOLATION;

/**
 * Performs analysis, computes and outputs the <a href="https://en.wikipedia.org/wiki/Robert_C._Martin">Robert C.Martin</a>'s dependency management metrics,
 * potentially failing the build if violations are found.
 *
 * @author <a href="mailto:ignatij.gichevski@gmail.com">Ignatij Gichevski</a>
 */
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

    private Map<MavenProject, List<String>> projectGraph;
    private Map<MavenProject, Double> instabilityPerComponent;
    private Map<MavenProject, Double> abstractionPerComponent;


    public void execute() throws MojoExecutionException {
        try {
            if (!project.getModules().isEmpty()) {
                initProjectGraphAndCalculateMetrics();
                writeMetricsToFile();
                if (failOnViolation) {
                    checkViolations();
                }
            }
        } catch (ProjectBuildingException e) {
            throw new MojoExecutionException("Error while building project", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error while reading the source files", e);
        }
    }

    private void initProjectGraphAndCalculateMetrics() throws ProjectBuildingException, IOException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        projectGraph = new ProjectGraphCreator(buildingRequest, projectBuilder).createProjectGraph(project);
        instabilityPerComponent = new StableDependenciesChecker(projectGraph).checkDependencies();
        abstractionPerComponent = new StableAbstractionsChecker(projectGraph).calculateAbstractionLevel();
    }

    private void writeMetricsToFile() throws MojoExecutionException, ProjectBuildingException, IOException {
        new MetricsFileWriter(getLog()).write(getPoints(), outputFile);
    }

    private List<Point> getPoints() {
        return projectGraph
                .keySet()
                .stream()
                .map(project -> new Point(project.getName(), instabilityPerComponent.get(project), abstractionPerComponent.get(project)))
                .collect(Collectors.toList());
    }

    private void checkViolations() throws MojoExecutionException {
        getLog().info("Checking for violation in Stable Dependencies Principle");
        new ViolationChecker(StableDependenciesPrincipleViolation.class).check(projectGraph, instabilityPerComponent, STABLE_DEPENDENCIES_VIOLATION);

        getLog().info("Checking for violation in Stable Abstractions Principle");
        new ViolationChecker(StableAbstractionsPrincipleViolation.class).check(projectGraph,
                abstractionPerComponent,
                STABLE_ABSTRACTIONS_VIOLATION
        );
    }
}
