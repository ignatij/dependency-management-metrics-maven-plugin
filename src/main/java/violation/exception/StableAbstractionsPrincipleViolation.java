package violation.exception;

import org.apache.maven.plugin.MojoExecutionException;

public class StableAbstractionsPrincipleViolation extends MojoExecutionException {

    public StableAbstractionsPrincipleViolation(String componentName) {
        super(String.format("Component %s is violating the stable abstraction principle", componentName));
    }

}
