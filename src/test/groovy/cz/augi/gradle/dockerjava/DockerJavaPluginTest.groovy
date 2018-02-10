package cz.augi.gradle.dockerjava

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

class DockerJavaPluginTest extends Specification {
    def "creates the tasks"() {
        def project = ProjectBuilder.builder().build()
        when:
        project.plugins.apply 'docker-java'
        project.dockerJava {
        }
        then:
        project.tasks.distDocker as DistDockerTask
        project.tasks.dockerPush as DockerPushTask
        cleanup:
        project.projectDir.deleteDir()
    }

    def "builds a new image that correctly wraps simple Java application"() {
        def dockerExecutor = new DockerExecutor(ProjectBuilder.builder().build())
        def projectDir = File.createTempDir('test', 'gradleDockerJava')
        new File(projectDir, 'build.gradle') << '''
            plugins {
              id 'docker-java'
            }
            mainClassName = 'cz.augi.gradle.dockerjava.TestApp'
            dockerJava {
                image = "test/my-app:$version"
                ports = [80, 8080]
                volumes = ['/test-volume']
            }
        '''
        def appDirectory = Paths.get(projectDir.absolutePath, 'src', 'main', 'java', 'cz', 'augi', 'gradle', 'dockerjava')
        Files.createDirectories(appDirectory)
        new File(appDirectory.toFile(), 'TestApp.java') << '''
        package cz.augi.gradle.dockerjava;
        public class TestApp {
            public static void main(String[] args) {
                System.out.println("Hello from Docker");
            }
        }
'''
        when:
        def gradleExecutionResult = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments('distDocker', '-Pversion=1.2.3', '-S')
                .withPluginClasspath()
                .build()
        def dockerRunOutput = dockerExecutor.execute('run', '--rm', 'test/my-app:1.2.3')
        def schemaVersionLabel = dockerExecutor.execute('inspect' ,'--format', '"{{ index .Config.Labels \\"org.label-schema.schema-version\\"}}"', 'test/my-app:1.2.3')
        def versionLabel = dockerExecutor.execute('inspect' ,'--format', '"{{ index .Config.Labels \\"org.label-schema.version\\"}}"', 'test/my-app:1.2.3')
        then:
        !gradleExecutionResult.output.contains('FAILED')
        dockerRunOutput.contains('Hello from Docker')
        schemaVersionLabel == '1.0'
        versionLabel == '1.2.3'
        def workingDirectory = Paths.get(projectDir.absolutePath, 'build', 'dockerJava')
        Files.exists(workingDirectory.resolve('Dockerfile'))
        cleanup:
        dockerExecutor.execute('rmi', 'test/my-app:1.2.3')
        dockerExecutor.project.projectDir.deleteDir()
        projectDir.deleteDir()
    }

    /* TODO: Build a test image and push it to Docker registry.
    def "pushes to Docker Registry"() {
        def project = ProjectBuilder.builder().build()
        when:
        project.plugins.apply 'docker-java'
        project.dockerJava {
            image = 'test-image'
            username = 'testuser'
            password = 'password_from_environment'
        }
        then:
        def pushTask = project.tasks.dockerPush as DockerPushTask
        pushTask.push()
        cleanup:
        project.projectDir.deleteDir()
    }*/
}
