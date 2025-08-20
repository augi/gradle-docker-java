package cz.augi.gradle.dockerjava

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.platform.base.Platform
import org.gradle.process.ExecSpec
import org.gradle.util.internal.VersionNumber

class DockerExecutor {
    private final Project project
    private final Logger logger

    DockerExecutor(Project project) {
        this.project = project
        this.logger = project.logger
    }

    String execute(String... args) {
        def er = project.providers.exec { ExecSpec e ->
            def finalArgs = ['docker']
            finalArgs.addAll(args)
            e.commandLine finalArgs
        }
        er.standardOutput.asText.get().trim()
    }

    List<String> getDockerInfo() {
        def asString = execute('info')
        logger.debug("Docker info: $asString")
        asString.readLines()
    }

    String getDockerPlatform() {
        String osType = execute('info', '--format', '{{.OSType}}')
        Platform
        osType.empty ? System.getProperty("os.name") : osType
    }

    VersionNumber getVersion() {
        String v = execute('version', '--format', '{{.Server.Version}}')
        if (v.indexOf('+') > 0) v = v.substring(0, v.indexOf('+'))
        VersionNumber.parse(v)
    }
}