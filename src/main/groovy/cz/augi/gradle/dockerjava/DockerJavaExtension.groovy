package cz.augi.gradle.dockerjava

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class DockerJavaExtension implements DistDockerSettings, DockerPushSettings {
    final Project project
    final DockerExecutor dockerExecutor

    DockerJavaExtension(Project project, DockerExecutor dockerExecutor) {
        this.dockerExecutor = dockerExecutor
        this.project = project
    }

    String image
    JavaVersion getJavaVersion() { customJavaVersion ?: project.targetCompatibility }
    void setJavaVersion(JavaVersion version) { customJavaVersion = version }
    private JavaVersion customJavaVersion
    String baseImage
    Integer[] ports = []
    String[] volumes = []
    String[] dockerfileLines = []

    String username
    String password
    String getRegistry() { image.substring(0, image.indexOf('/')) }
}
