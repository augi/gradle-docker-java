package cz.augi.gradle.dockerjava

import org.gradle.api.Plugin
import org.gradle.api.Project

class DockerJavaPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('dockerJava', DockerJavaExtension, project)
        def distDocker = project.tasks.create('distDocker', DistDockerTask)
        def dockerPush = project.tasks.create('dockerPush', DockerPushTask)
        distDocker.extension = extension
        dockerPush.extension = extension
    }
}
