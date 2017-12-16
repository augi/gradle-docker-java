package cz.augi.gradle.dockerjava

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

class DockerPushTask extends DefaultTask {
    DockerPushTask() {
        this.group = 'distribution'
    }

    @Internal
    DockerExecutor dockerExecutor
    @Nested
    DockerPushSettings settings

    @TaskAction
    def push() {
        def configFile = new File(project.buildDir, 'localDockerConfig')
        project.exec {
            it.commandLine 'docker', '--config', configFile.absolutePath, 'login', '-u', settings.username, '-p', settings.password, settings.registry
        }
        project.exec {
            it.commandLine 'docker', '--config', configFile.absolutePath, 'push', settings.image
        }
        configFile.delete()
    }
}

interface DockerPushSettings {
    @Input
    String getImage()
    @Input
    String getUsername()
    @Input
    String getPassword()
    @Input
    String getRegistry()
}
