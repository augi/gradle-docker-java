package cz.augi.gradle.dockerjava

import org.gradle.api.Project

class DockerJavaExtension {
    private final Project project

    DockerJavaExtension(Project project) {
        this.project = project
    }
}
