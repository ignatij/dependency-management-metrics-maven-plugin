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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public void execute() throws MojoExecutionException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        try {
            if (!project.getModules().isEmpty()) {
                final Map<MavenProject, List<String>> projectGraph = ProjectUtil.createProjectGraph(buildingRequest, projectBuilder, project);
                getLog().info("Project graph defined: " + projectGraph);
                final Map<MavenProject, Double> instabilityPerComponent = new StableDependenciesChecker(projectGraph).checkDependencies();
                getLog().info("Instability per component calculated: " + instabilityPerComponent);
                final Map<MavenProject, Double> abstractionPerComponent = new StableAbstractionsChecker(projectGraph).calculateAbstractionLevel();
                getLog().info("Abstraction per component calculated: " + abstractionPerComponent);

                List<Point> points = new ArrayList<>();
                for (MavenProject project : projectGraph.keySet()) {
                    points.add(new Point(project.getName(), instabilityPerComponent.get(project), abstractionPerComponent.get(project)));
                }
                getLog().info("Writing to file: " + outputFile);
                new MetricsFileWriter(getLog()).write(points, outputFile);
                if (failOnViolation) {
                    getLog().info("failOnViolation is ON");
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
