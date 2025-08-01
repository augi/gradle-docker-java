package cz.augi.gradle.dockerjava

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.application.CreateStartScripts

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Clock

class DistDockerTask extends DefaultTask {
    DistDockerTask() {
        this.group = 'distribution'
        this.description = 'Build a new Docker image that contains JVM application built from current project.'
    }

    @Internal
    DockerExecutor dockerExecutor
    @Internal
    GitExecutor gitExecutor
    @Nested
    DistDockerSettings settings

    def createDockerfile(File workDir, String unpackedDistributionDir, String applicationJarFilename, CreateStartScripts startScripts) {
        def dockerFile = new File(workDir, 'Dockerfile')
        dockerFile.delete()
        if (dockerExecutor.getDockerPlatform().toLowerCase().contains('win')) {
            dockerFile << 'FROM ' + settings.baseImage + '\n'
            dockerFile << 'SHELL ["cmd", "/S", "/C"]\n'
            if (settings.ports.any()) {
                dockerFile << 'EXPOSE ' + settings.ports.join(' ') + '\n'
            }
            settings.volumes.each { dockerFile << "VOLUME $it\n" }
            dockerFile << 'LABEL ' + getLabels().collect { "\"${it.key}\"=\"${it.value}\"" }.join(' ') + '\n'
            settings.dockerfileLines.each { dockerFile << it + '\n' }
            dockerFile << "COPY $unpackedDistributionDir C:\n"
            dockerFile << "COPY $applicationJarFilename C:\\\\lib\n"
            dockerFile << "WORKDIR C:\\\\bin\n"
            dockerFile << "ENTRYPOINT ${startScripts.windowsScript.name} ${settings.arguments.join(' ')}"
        } else {
            dockerFile << 'FROM ' + settings.baseImage + '\n'
            if (settings.ports.any()) {
                dockerFile << 'EXPOSE ' + settings.ports.join(' ') + '\n'
            }
            settings.volumes.each { dockerFile << "VOLUME $it\n" }
            dockerFile << 'LABEL ' + getLabels().collect { "\"${it.key}\"=\"${it.value}\"" }.join(' ') + '\n'
            settings.dockerfileLines.each { dockerFile << it + '\n' }
            dockerFile << "COPY $unpackedDistributionDir /var/app\n"
            dockerFile << "COPY $applicationJarFilename /var/app/lib\n"
            dockerFile << "WORKDIR /var/app/bin\n"
            dockerFile << "ENTRYPOINT [\"./${startScripts.unixScript.name}\"${settings.arguments.collect { ",\"$it\"" }.join('')}]"
        }
    }

    private Map<String, String> getLabels() {
        def url = getUrl()
        def vcsUrl = getVcsUrl()
        def vcsRef = getVcsRef()

        // https://label-schema.org/
        def labels = ['org.label-schema.schema-version':'1.0']
        labels.put('org.label-schema.build-date', Clock.systemUTC().instant().toString())
        labels.put('org.label-schema.version', project.version.toString())
        labels.put('org.label-schema.name', project.name)
        if (project.description) labels.put('org.label-schema.description', project.description)
        if (url) labels.put('org.label-schema.url', url)
        if (vcsUrl) labels.put('org.label-schema.vcs-url', vcsUrl)
        if (vcsRef) labels.put('org.label-schema.vcs-ref', vcsRef)
        labels.put('org.label-schema.docker.cmd', "docker run -d ${settings.ports.collect { "-p $it:$it" }.join(' ')} ${settings.volumes.collect { "-v $it:$it" }.join(' ')} ${settings.image}")

        // https://github.com/opencontainers/image-spec/blob/main/annotations.md
        labels.put('org.opencontainers.image.created', Clock.systemUTC().instant().toString())
        labels.put('org.opencontainers.image.version', project.version.toString())
        labels.put('org.opencontainers.image.title', project.name)
        if (project.description) labels.put('org.opencontainers.image.description', project.description)
        if (url) labels.put('org.opencontainers.image.url', url)
        if (vcsUrl) labels.put('org.opencontainers.image.source', vcsUrl)
        if (vcsRef) labels.put('org.opencontainers.image.revision', vcsRef)

        labels.putAll(settings.labels)
        labels
    }

    // following environment variables that can be present in various environments
    //  * https://confluence.jetbrains.com/display/TCD10/Predefined+Build+Parameters
    //  * https://wiki.jenkins.io/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-belowJenkinsSetEnvironmentVariables
    //  * https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
    //  * https://circleci.com/docs/1.0/environment-variables/
    //  * http://circleci.com/docs/2.0/env-vars/#build-details

    private String getVcsUrl() {
        def r = System.getenv('GIT_URL') ?: System.getenv('CIRCLE_REPOSITORY_URL')
        if (r) return r
        def slug = System.getenv('TRAVIS_PULL_REQUEST_SLUG') ?: System.getenv('TRAVIS_REPO_SLUG')
        if (slug) return "https://github.com/$slug"
        gitExecutor.getUrl()
    }

    private String getVcsRef() {
        def r = System.getenv('GIT_COMMIT') ?: System.getenv('TRAVIS_COMMIT') ?: System.getenv('CIRCLE_SHA1')
        if (r) return r
        def fromTC = System.getenv().findAll { it.key.startsWith('BUILD_VCS_NUMBER') }.collect { it.value }.find()
        if (fromTC) return fromTC
        gitExecutor.getRef()
    }

    private String getVcsBranch() {
        def r = System.getenv('GIT_BRANCH') ?: System.getenv('TRAVIS_PULL_REQUEST_BRANCH') ?: System.getenv('TRAVIS_BRANCH') ?: System.getenv('CIRCLE_BRANCH')
        if (r) return r
        gitExecutor.getBranch()
    }

    private String getUrl() {
        def url = getVcsUrl()
        if (url && url.startsWith('http')) url else null
    }

    @TaskAction
    def create() {
        assert settings.image : 'Image must be specified'
        Path workDir = settings.customDockerfile ? settings.customDockerfile.parentFile.toPath() : settings.dockerBuildDirectory.toPath()
        Files.createDirectories(workDir)
        if (settings.filesToCopy) {
            settings.filesToCopy.each { Files.copy(it.toPath(), workDir.resolve(it.name), StandardCopyOption.REPLACE_EXISTING) }
        }
        if (settings.customDockerfile) {
            def args = ['build', '-t', settings.image]
            settings.alternativeImages.each { args.addAll(['-t', it]) }
            args.addAll(['--file', settings.customDockerfile.name])
            settings.buildArgs.each { args.addAll(['--build-arg', it]) }
            args.addAll(settings.dockerBuildArgs)
            args.add(workDir.toFile().absolutePath)
            dockerExecutor.execute(*args)
        } else {
            File sourceTar = project.tasks.distTar.archiveFile.get().getAsFile()
            String tarRootDirectory = sourceTar.name.substring(0, sourceTar.name.lastIndexOf('.'))
            project.copy {
                it.from(project.tarTree(sourceTar))
                it.into workDir
            }
            String applicationJarFilename = project.tasks.jar.archiveFileName.get()
            Path applicationJarSourcePath = Paths.get(workDir.toAbsolutePath().toString(), tarRootDirectory, 'lib', applicationJarFilename)
            Path applicationJarTargetPath = Paths.get(workDir.toAbsolutePath().toString(), applicationJarFilename)
            Files.move(applicationJarSourcePath, applicationJarTargetPath, StandardCopyOption.REPLACE_EXISTING)

            CreateStartScripts startScripts = project.tasks.startScripts
            createDockerfile(workDir.toFile(), tarRootDirectory, applicationJarFilename, startScripts)
            def args = ['build', '-t', settings.image]
            settings.alternativeImages.each { args.addAll(['-t', it]) }
            args.addAll(settings.dockerBuildArgs)
            args.add(workDir.toFile().absolutePath)
            dockerExecutor.execute(*args)
        }
    }
}

interface DistDockerSettings {
    @Input
    String getImage()
    @Input @Optional
    String[] getAlternativeImages()
    @Input @Optional
    String getBaseImage()
    @Input @Optional
    Integer[] getPorts()
    @Input @Optional
    String[] getVolumes()
    @Input @Optional
    Map<String, String> getLabels()
    @Input @Optional
    String[] getDockerfileLines()
    @Input @Optional
    String[] getArguments()
    @Input @Optional
    String[] getDockerBuildArgs()
    @InputDirectory @Optional
    File getDockerBuildDirectory()
    @Input @Optional
    File[] getFilesToCopy()
    @InputFile @Optional
    File getCustomDockerfile()
    @Input @Optional
    String[] getBuildArgs()
}
