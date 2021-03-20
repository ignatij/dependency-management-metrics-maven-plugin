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

class ProjectUtil {

    private ProjectUtil() {
    }

    private static final String POM_FILE_NAME = "pom.xml";

    static Map<MavenProject, List<String>> createProjectGraph(ProjectBuildingRequest buildingRequest,
                                                                     ProjectBuilder projectBuilder,
                                                                     MavenProject project) throws ProjectBuildingException {
        Map<MavenProject, List<String>> projectGraph = new LinkedHashMap<>();
        for (String module : project.getModules()) {
            createProjectGraph(projectGraph, buildingRequest, projectBuilder, module, project.getBasedir());
        }
        return projectGraph;
    }

    private static Map<MavenProject, List<String>> createProjectGraph(Map<MavenProject, List<String>> projectGraph,
                                                                      ProjectBuildingRequest buildingRequest,
                                                                      ProjectBuilder projectBuilder,
                                                                      String moduleName,
                                                                      File baseDir
    ) throws ProjectBuildingException {
        buildingRequest.setProject(null);
        File directory = getFile(new File(baseDir.getAbsolutePath()), moduleName);
        File pomFile = getFile(directory, POM_FILE_NAME);
        MavenProject mavenProject = projectBuilder.build(pomFile, buildingRequest)
                .getProject();

        if (projectGraph.containsKey(mavenProject)) {
            return projectGraph;
        }

        List<String> projectDependencies = getProjectDependencies(mavenProject);
        projectGraph.put(mavenProject, projectDependencies);
        for (String dependency : projectDependencies) {
            return createProjectGraph(projectGraph, buildingRequest, projectBuilder, dependency, baseDir);
        }
        return projectGraph;
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
