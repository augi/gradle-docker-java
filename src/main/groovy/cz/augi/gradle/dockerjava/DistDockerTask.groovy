package cz.augi.gradle.dockerjava

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.application.CreateStartScripts

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DistDockerTask extends DefaultTask {
    DistDockerTask() {
        this.group = 'distribution'
    }

    @Internal
    DockerExecutor dockerExecutor
    @Nested
    DistDockerSettings settings

    def createDockerfile(File workDir, String tarFileName, String tarRootDirectory, CreateStartScripts startScripts) {
        def dockerFile = new File(workDir, 'Dockerfile')
        if (dockerExecutor.getDockerPlatform().toLowerCase().contains('win')) {
            dockerFile << "FROM " + (settings.baseImage ?: 'openjdk:8u151-nanoserver-sac2016') + '\n'
            dockerFile << 'SHELL ["cmd", "/S", "/C"]\n'
            if (settings.ports.any()) {
                dockerFile << 'EXPOSE' + settings.ports.join(' ') + '\n'
            }
            settings.volumes.each { dockerFile << "VOLUME $it\n" }
            settings.dockerfileLines.each { dockerFile << it + '\n' }
            dockerFile << "ADD $tarFileName C:\n"
            dockerFile << "WORKDIR C:\\\\$tarRootDirectory\\\\bin\n"
            dockerFile << "ENTRYPOINT ${startScripts.windowsScript.name}"
        } else {
            dockerFile << "FROM " + (settings.baseImage ?: 'openjdk:8u151-jre-alpine') + '\n'
            if (settings.ports.any()) {
                dockerFile << 'EXPOSE' + settings.ports.join(' ') + '\n'
            }
            settings.volumes.each { dockerFile << "VOLUME $it\n" }
            settings.dockerfileLines.each { dockerFile << it + '\n' }
            dockerFile << "ADD $tarFileName /var\n"
            dockerFile << "WORKDIR /var/$tarRootDirectory/bin\n"
            dockerFile << "ENTRYPOINT [\"./${startScripts.unixScript.name}\"]"
        }
    }

    @TaskAction
    def create() {
        assert settings.image : 'Image must be specified'
        def workDir = new File(project.buildDir, 'dockerJava')
        Files.createDirectories(workDir.toPath())
        def tarFile = new File(workDir, 'dist.tar')

        File sourceTar = settings.project.tasks.distTar.archivePath
        Files.copy(sourceTar.toPath(), tarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        String tarRootDirectory = sourceTar.name.substring(0, sourceTar.name.lastIndexOf('.'))
        CreateStartScripts startScripts = settings.project.tasks.startScripts
        createDockerfile(workDir, tarFile.name, tarRootDirectory, startScripts)

        dockerExecutor.execute('build', '-t', settings.image, workDir.absolutePath)
    }
}

interface DistDockerSettings {
    @Input
    String getImage()
    @Input @Optional
    String getBaseImage()
    @Input @Optional
    Integer[] getPorts()
    @Input @Optional
    String[] getVolumes()
    @Input @Optional
    String[] getDockerfileLines()
}
