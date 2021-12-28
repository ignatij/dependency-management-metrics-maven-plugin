# Dependency Management Metrics Maven Plugin

---

Maven plugin that computes the Dependency Management Metrics for your multi-module Java projects.

![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)
![Maven Central](https://img.shields.io/maven-central/v/com.github.ignatij/dependency-management-metrics-maven-plugin)
----

### Dependency Management Metrics

For the theoretical part behind the metrics, please refer to the [book](https://www.amazon.com/Clean-Architecture-Craftsmans-Software-Structure/dp/0134494164) itself, or read this Medium [post](https://medium.com/javarevisited/using-metrics-for-crafting-maintainable-solutions-on-the-long-run-the-maven-way-1a2d84508bf0).

## Getting Started

This plugin calculates and outputs the metrics stated and explained above for each component within a design:

* Stability metric
* Abstraction metric
* Distance from <i>Main Sequence</i>

The plugin also outputs the components present in the <i>Zone of Pain</i> and the <i>Zone of Uselessness</i> (if there
are any) in the <i>Zones of Exclusions</i> section.

And finally the plugin can potentially break the build if any of the following principles are broken:

* Stable Dependencies Principle
* Stable Abstractions Principle

Add the following in your parent pom.xml file:

```xml
<build>
  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>com.github.ignatij</groupId>
        <artifactId>dependency-management-metrics-maven-plugin</artifactId>
        <version>1.0.17</version>
      </plugin>
    </plugins>
  </pluginManagement>
  <plugins>
    <plugin>
      <groupId>com.github.ignatij</groupId>
      <artifactId>dependency-management-metrics-maven-plugin</artifactId>
      <inherited>false</inherited>
      <executions>
        <execution>
          <phase>verify</phase>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
    </plugin></plugins>
</build>
```
If you want to potentially fail the build if any of the principles are violated, add the following <b>configuration</b> tag:
```xml
<plugin>
    <groupId>com.github.ignatij</groupId>
    <artifactId>dependency-management-metrics-maven-plugin</artifactId>
    <version>1.0.17</version>
    <configuration>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <inherited>false</inherited>
</plugin>
```
By default, the output file is generated in the root **target** folder.
But you can  customize that if needed:
``` xml
    ...
    <configuration>
        <output.file>...</output.file>
    </configuration>
    ...
```

Feel free to report any issues or open a Pull Request for further improvements.
