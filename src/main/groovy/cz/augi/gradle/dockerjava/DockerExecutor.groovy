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

    String executeToString(String... args) {
        new ByteArrayOutputStream().withStream { os ->
            executeWithCustomOutput(os, args)
            os.toString().trim()
        }
    }

    void execute(String... args) {
        executeWithCustomOutput(null, args)
    }

    private void executeWithCustomOutput(OutputStream os, String... args) {
        project.exec { ExecSpec e ->
            def finalArgs = ['docker']
            finalArgs.addAll(args)
            e.commandLine finalArgs
            if (os != null) {
                e.standardOutput os
            }
        }
    }

    List<String> getDockerInfo() {
        def asString = executeToString('info')
        logger.debug("Docker info: $asString")
        asString.readLines()
    }

    String getDockerPlatform() {
        String osType = executeToString('info', '--format', '{{.OSType}}')
        Platform
        osType.empty ? System.getProperty("os.name") : osType
    }

    VersionNumber getVersion() {
        String v = executeToString('version', '--format', '{{.Server.Version}}')
        if (v.indexOf('+') > 0) v = v.substring(0, v.indexOf('+'))
        VersionNumber.parse(v)
    }
}