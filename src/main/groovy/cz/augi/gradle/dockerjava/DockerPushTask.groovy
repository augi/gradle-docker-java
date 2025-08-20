package cz.augi.gradle.dockerjava

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
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
        def dockerConfigDir = new File(project.layout.buildDirectory.get().getAsFile(), 'localDockerConfig')
        try {
            if (settings.username) {
                if (dockerExecutor.version >= VersionNumber.parse('17.07.0')) {
                    project.providers.exec {
                        it.commandLine 'docker', '--config', dockerConfigDir.absolutePath, 'login', '-u', settings.username, '--password-stdin', settings.registry
                        it.standardInput = new ByteArrayInputStream((settings.password ?: '').getBytes(StandardCharsets.UTF_8))
                    }.result.get()
                } else {
                    project.providers.exec {
                        it.commandLine 'docker', '--config', dockerConfigDir.absolutePath, 'login', '-u', settings.username, '-p', settings.password, settings.registry
                    }.result.get()
                }
            }
            project.providers.exec {
                def args = ['docker']
                if (settings.username) {
                    args.addAll(['--config', dockerConfigDir.absolutePath])
                }
                args.addAll(['push', settings.image])
                it.commandLine(*args)
            }.result.get()
            settings.alternativeImages.each { alternativeImage ->
                def args = ['docker']
                if (settings.username) {
                    args.addAll(['--config', dockerConfigDir.absolutePath])
                }
                args.addAll(['push', alternativeImage])
                project.providers.exec {
                    it.commandLine(*args)
                }.result.get()
            }
        } finally {
            if (dockerConfigDir.exists()) {
                if (dockerConfigDir.directory) {
                    dockerConfigDir.listFiles().each { it.delete() }
                }
                dockerConfigDir.delete()
            }
        }
        if (settings.removeImage) {
            def args = ['docker', 'rmi', '--force', settings.image]
            args.addAll(settings.alternativeImages)
            project.providers.exec {
                it.commandLine(*args)
            }.result.get()
        }
    }
}

interface DockerPushSettings {
    @Input
    String getImage()
    @Input @Optional
    String[] getAlternativeImages()
    @Input @Optional
    String getUsername()
    @Input @Optional
    String getPassword()
    @Input
    String getRegistry()
    @Input
    Boolean getRemoveImage()
}
