package writer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import statistic.Point;
import statistic.StatisticUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class MetricsFileWriter implements MetricsWriter {
    private static final String OUTPUT_FORMAT = "%-30s %-30s %-30s %-30s %n";
    private final Log log;

    public MetricsFileWriter(Log log) {
        this.log = log;
    }

    @Override
    public void write(List<Point> points, File outputFile) throws MojoExecutionException {
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot create file: " + outputFile.getAbsolutePath(), e);
            }
        }
        try (FileWriter writer = new FileWriter(outputFile, false)) {
            log.info("Writing to file: " + outputFile.getAbsolutePath());

            writeComponentInfo(points, writer);
            writeZonesOfExclusion(points, writer);
            writeStatisticalAnalysis(points, writer);

            log.info("Finished writing to file: " + outputFile.getAbsolutePath());
            writer.flush();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write to result file: " + outputFile.getAbsolutePath(), e);
        }
    }

    private void writeComponentInfo(List<Point> points, FileWriter writer) throws IOException {
        writer.write(String.format(OUTPUT_FORMAT, "COMPONENT", "INSTABILITY", "ABSTRACTION", "DISTANCE FROM MAIN SEQUENCE"));
        for(int i = 0; i < 120; i++) {
            writer.write("=");
        }
        writer.write("\n\n");
        for (Point point : points) {
            writer.write(String.format(OUTPUT_FORMAT, point.getComponent(), point.getX(), point.getY(), point.distance()));
        }
        writer.write("\n");
    }

    private void writeZonesOfExclusion(List<Point> points, FileWriter writer) throws IOException {
        if (points.stream().noneMatch(Point::isInZoneOfPain) && points.stream().noneMatch(Point::isInZoneOfUselessness)) {
            return;
        }
        header("ZONES OF EXCLUSION", writer);
        if (points.stream().anyMatch(Point::isInZoneOfPain)) {
            writer.write("IN ZONE OF PAIN: " + points.stream().filter(Point::isInZoneOfPain).map(Point::getComponent).collect(Collectors.toList()));
            writer.write("\n");
        }
        if (points.stream().anyMatch(Point::isInZoneOfUselessness)) {
            writer.write("IN ZONE OF USELESSNESS: " + points.stream().filter(Point::isInZoneOfUselessness).map(Point::getComponent).collect(Collectors.toList()));
            writer.write("\n");
        }
        writer.write("\n");
    }

    private void writeStatisticalAnalysis(List<Point> points, FileWriter writer) throws IOException {
        header("STATISTICAL ANALYSIS OF DISTANCE FROM MAIN SEQUENCE", writer);
        writer.write("MEAN: " + StatisticUtil.mean(points) + "\n");
        writer.write("VARIANCE: " + StatisticUtil.variance(points) + "\n");
        writer.write("STANDARD DEVIATION: " + StatisticUtil.standardDeviation(points));
    }

    private void header(String header, FileWriter writer) throws IOException {
        writer.write("\n");

        for (int i = 0; i < 30; i++) {
            writer.write("=");
        }
        writer.write(header);
        for (int i = 0; i < 30; i++) {
            writer.write("=");
        }
        writer.write("\n\n");
    }
}
