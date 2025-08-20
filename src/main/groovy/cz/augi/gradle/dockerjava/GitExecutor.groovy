package cz.augi.gradle.dockerjava

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.process.ExecSpec

class GitExecutor {
    private final Project project
    private final Logger logger

    GitExecutor(Project project) {
        this.project = project
        this.logger = project.logger
    }

    String execute(String... args) {
        def execResult = project.providers.exec { ExecSpec e ->
            def finalArgs = ['git']
            finalArgs.addAll(args)
            e.commandLine finalArgs
            e.ignoreExitValue true
        }
        def so = execResult.standardOutput.asText.get().trim()
        def eo = execResult.standardError.asText.get().trim()
        if (so || eo) logger.debug("Error output from 'git ${args.join(' ')}' command: $so\n$eo")
        if (execResult.result.get().exitValue != 0) {
            throw new RuntimeException("Cannot execute 'git ${args.join(' ')}' command: $so\n$eo")
        }
        so
    }

    String getUrl() {
        try {
            def remoteName = execute('remote').readLines().find()
            remoteName ? execute('remote', 'get-url', remoteName) : null
        } catch (RuntimeException e) {
            logger.debug("Cannot get Git remote url: ${e.message}", e)
            null
        }
    }

    String getRef() {
        try {
            execute('rev-parse', 'HEAD')
        } catch (RuntimeException e) {
            logger.debug("Cannot get Git revision: ${e.message}", e)
            null
        }
    }

    String getBranch() {
        try {
            execute('rev-parse', '--abbrev-ref', 'HEAD')
        } catch (RuntimeException e) {
            logger.debug("Cannot get Git revision: ${e.message}", e)
            null
        }
    }
}
