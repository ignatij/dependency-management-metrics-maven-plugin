package violation.exception;

import org.apache.maven.plugin.MojoExecutionException;

public class StableDependenciesPrincipleViolation extends MojoExecutionException {

    public StableDependenciesPrincipleViolation(String componentName) {
        super(String.format("Component %s is violating the stable dependencies principle", componentName));
    }

}
