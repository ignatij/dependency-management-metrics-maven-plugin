package com.github.ignatij;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class ProjectGraphCreator {

    private final ProjectBuildingRequest buildingRequest;
    private final ProjectBuilder projectBuilder;

    ProjectGraphCreator(final ProjectBuildingRequest buildingRequest,
                        final ProjectBuilder projectBuilder) {
        this.buildingRequest = buildingRequest;
        this.projectBuilder = projectBuilder;
    }

    private static final String POM_FILE_NAME = "pom.xml";

    Map<MavenProject, List<String>> createProjectGraph(MavenProject project) throws ProjectBuildingException {
        return createProjectGraph(new LinkedHashMap<>(), project);
    }

    private Map<MavenProject, List<String>> createProjectGraph(Map<MavenProject, List<String>> projectGraph,
                                                               MavenProject project) throws ProjectBuildingException {
        for (String module : project.getModules()) {
            buildingRequest.setProject(null);
            File directory = getFile(new File(project.getBasedir().getAbsolutePath()), module);
            File pomFile = getFile(directory, POM_FILE_NAME);
            MavenProject mavenProject = projectBuilder.build(pomFile, buildingRequest)
                    .getProject();
            if (!mavenProject.getModules().isEmpty()) {
                createProjectGraph(projectGraph, mavenProject);
            } else {
                createProjectGraphDependencies(projectGraph, mavenProject);
            }
        }
        return projectGraph;
    }

    private void createProjectGraphDependencies(Map<MavenProject, List<String>> projectGraph,
                                                MavenProject mavenProject) {
        if (projectGraph.containsKey(mavenProject)) {
            return;
        }

        List<String> projectDependencies = getProjectDependencies(mavenProject);
        projectGraph.put(mavenProject, projectDependencies);
    }

    private static List<String> getProjectDependencies(MavenProject mavenProject) {
        List<Dependency> dependencies = mavenProject.getDependencies();
        return dependencies
                .stream()
                .filter(d -> d.getGroupId().equals(mavenProject.getGroupId()))
                .map(Dependency::getArtifactId)
                .collect(Collectors.toList());
    }

    private static File getFile(File dir, String fileName) {
        File[] files = dir.listFiles();
        assert files != null;
        return Arrays.stream(files)
                .filter(file -> file.getName().equals(fileName))
                .findFirst()
                .orElse(null);
    }
}
