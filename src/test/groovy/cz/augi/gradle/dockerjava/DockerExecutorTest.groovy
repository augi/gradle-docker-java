package cz.augi.gradle.dockerjava

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.internal.VersionNumber
import spock.lang.Specification

class DockerExecutorTest extends Specification {
    def "reads version"() {
        def project = ProjectBuilder.builder().build()
        def target = new DockerExecutor(project)
        when:
        def version = target.version
        then:
        version >= VersionNumber.version(1)
        cleanup:
        project.projectDir.deleteDir()
    }
}
