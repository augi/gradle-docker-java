package cz.augi.gradle.dockerjava

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class DockerJavaPluginTest extends Specification {
    def "prepares the Docker image"() {
        def project = ProjectBuilder.builder().build()
        when:
        project.plugins.apply 'docker-java'
        project.repositories {
            jcenter()
        }
        project.dockerJava {
        }
        project.evaluate()
        then:
        project.tasks.distDocker as DistDockerTask
    }
}
