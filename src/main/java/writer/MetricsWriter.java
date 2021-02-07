package writer;

import org.apache.maven.plugin.MojoExecutionException;
import statistic.Point;

import java.io.File;
import java.util.List;

public interface MetricsWriter {
    void write(List<Point> points, File outputFile) throws MojoExecutionException;
}
