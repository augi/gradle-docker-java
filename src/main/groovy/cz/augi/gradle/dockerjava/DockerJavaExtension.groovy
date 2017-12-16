package cz.augi.gradle.dockerjava

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.Input

class DockerJavaExtension implements DistDockerSettings, DockerPushSettings {
    final Project project
    final DockerExecutor dockerExecutor

    DockerJavaExtension(Project project, DockerExecutor dockerExecutor) {
        this.dockerExecutor = dockerExecutor
        this.project = project
    }

    @Input
    String image
    @Input
    JavaVersion javaVersion
    @Input
    String baseImage
    @Input
    Integer[] ports = []
    @Input
    String[] volumes = []
    @Input
    String[] dockerfileLines = []

    @Input
    String username
    @Input
    String password
}
