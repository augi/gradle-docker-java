package cz.augi.gradle.dockerjava

import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
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
        this.description = 'Build a new Docker image that contains JVM application built from current project.'
    }

    @Internal
    DockerExecutor dockerExecutor
    @Nested
    DistDockerSettings settings

    def createDockerfile(File workDir, String tarFileName, String tarRootDirectory, CreateStartScripts startScripts) {
        def dockerFile = new File(workDir, 'Dockerfile')
        dockerFile.delete()
        if (dockerExecutor.getDockerPlatform().toLowerCase().contains('win')) {
            dockerFile << "FROM " + (settings.baseImage ?: getWindowsBaseImage()) + '\n'
            dockerFile << 'SHELL ["cmd", "/S", "/C"]\n'
            if (settings.ports.any()) {
                dockerFile << 'EXPOSE ' + settings.ports.join(' ') + '\n'
            }
            settings.volumes.each { dockerFile << "VOLUME $it\n" }
            settings.dockerfileLines.each { dockerFile << it + '\n' }
            dockerFile << "ADD $tarFileName C:\n"
            dockerFile << "WORKDIR C:\\\\$tarRootDirectory\\\\bin\n"
            dockerFile << "ENTRYPOINT ${startScripts.windowsScript.name}"
        } else {
            dockerFile << "FROM " + (settings.baseImage ?: getLinuxBaseImage()) + '\n'
            if (settings.ports.any()) {
                dockerFile << 'EXPOSE ' + settings.ports.join(' ') + '\n'
            }
            settings.volumes.each { dockerFile << "VOLUME $it\n" }
            settings.dockerfileLines.each { dockerFile << it + '\n' }
            dockerFile << "ADD $tarFileName /var\n"
            dockerFile << "WORKDIR /var/$tarRootDirectory/bin\n"
            if (settings.javaVersion == JavaVersion.VERSION_1_8) {
                dockerFile << 'ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"\n'
            }
            dockerFile << "ENTRYPOINT [\"./${startScripts.unixScript.name}\"]"
        }
    }

    private String getWindowsBaseImage() {
        switch (settings.javaVersion) {
            case JavaVersion.VERSION_1_8:
                'openjdk:8u161-nanoserver-sac2016'
                break
            case JavaVersion.VERSION_1_9:
                'openjdk:9.0.4-jdk-nanoserver-sac2016'
                break
            default:
                throw new RuntimeException("Java version ${settings.javaVersion} is not supported")
        }
    }

    private String getLinuxBaseImage() {
        switch (settings.javaVersion) {
            case JavaVersion.VERSION_1_8:
                'openjdk:8u151-jre-alpine'
                break
            case JavaVersion.VERSION_1_9:
                'openjdk:9.0.1-11-jre-slim'
                break
            default:
                throw new RuntimeException("Java version ${settings.javaVersion} is not supported")
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
    JavaVersion getJavaVersion()
    @Input @Optional
    String getBaseImage()
    @Input @Optional
    Integer[] getPorts()
    @Input @Optional
    String[] getVolumes()
    @Input @Optional
    String[] getDockerfileLines()
}
