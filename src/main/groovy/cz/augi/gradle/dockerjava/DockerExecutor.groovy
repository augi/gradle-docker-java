package cz.augi.gradle.dockerjava

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.platform.base.Platform
import org.gradle.process.ExecSpec

class DockerExecutor {
    private final Project project
    private final Logger logger

    DockerExecutor(Project project) {
        this.project = project
        this.logger = project.logger
    }

    String execute(String... args) {
        new ByteArrayOutputStream().withStream { os ->
            project.exec { ExecSpec e ->
                def finalArgs = ['docker']
                finalArgs.addAll(args)
                e.commandLine finalArgs
                e.standardOutput os
            }
            os.toString().trim()
        }
    }

    List<String> getDockerInfo() {
        def asString = execute('info')
        logger.debug("Docker info: $asString")
        asString.readLines()
    }

    String getDockerPlatform() {
        String osType = getDockerInfo().find { it.startsWith('OSType:') }
        Platform
        osType.empty ? System.getProperty("os.name") : osType.substring('OSType:'.length()).trim()
    }
}
