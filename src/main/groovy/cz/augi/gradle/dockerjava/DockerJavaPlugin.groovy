package cz.augi.gradle.dockerjava

import org.gradle.api.Plugin
import org.gradle.api.Project

class DockerJavaPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply 'application'

        def dockerExecutor = new DockerExecutor(project)
        def extension = project.extensions.create('dockerJava', DockerJavaExtension, project, dockerExecutor)
        def distDocker = project.tasks.create('distDocker', DistDockerTask)
        def dockerPush = project.tasks.create('dockerPush', DockerPushTask)
        distDocker.settings = extension
        distDocker.dockerExecutor = dockerExecutor
        dockerPush.settings = extension
        dockerPush.dockerExecutor = dockerExecutor

        distDocker.dependsOn project.tasks.distTar
        dockerPush.dependsOn distDocker
        project.tasks.assembleDist.dependsOn distDocker
    }
}
