package cz.augi.gradle.dockerjava

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

import java.nio.charset.StandardCharsets

class DockerPushTask extends DefaultTask {
    DockerPushTask() {
        this.group = 'distribution'
        this.description = 'Pushes the existing image to Docker Registry.'
    }

    @Internal
    DockerExecutor dockerExecutor
    @Nested
    DockerPushSettings settings

    @TaskAction
    def push() {
        assert settings.image : 'Image must be specified'
        def configFile = new File(project.buildDir, 'localDockerConfig')
        try {
            if (settings.username) {
                if (dockerExecutor.version >= VersionNumber.parse('17.07.0')) {
                    project.exec {
                        it.commandLine 'docker', '--config', configFile.absolutePath, 'login', '-u', settings.username, '--password-stdin', settings.registry
                        it.standardInput = new ByteArrayInputStream((settings.password ?: '').getBytes(StandardCharsets.UTF_8))
                    }
                } else {
                    project.exec {
                        it.commandLine 'docker', '--config', configFile.absolutePath, 'login', '-u', settings.username, '-p', settings.password, settings.registry
                    }
                }
            }
            project.exec {
                it.commandLine 'docker', '--config', configFile.absolutePath, 'push', settings.image
            }
        } finally {
            configFile.delete()
        }
        if (settings.removeImage) {
            project.exec {
                it.commandLine 'docker', 'rmi', '--force', settings.image
            }
        }
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
    @Input
    Boolean getRemoveImage()
}
