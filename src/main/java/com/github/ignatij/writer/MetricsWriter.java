package com.github.ignatij.writer;

import org.apache.maven.plugin.MojoExecutionException;
import com.github.ignatij.statistic.Point;

import java.io.File;
import java.util.List;

public interface MetricsWriter {
    void write(List<Point> points, File outputFile) throws MojoExecutionException;
}
