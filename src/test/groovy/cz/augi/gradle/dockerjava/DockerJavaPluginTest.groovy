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
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments('distDocker', '-Pversion=1.2.3')
                .withPluginClasspath()
                .build()
        def dockerOutput = new DockerExecutor(ProjectBuilder.builder().build()).execute('run', '--rm', 'test/my-app:1.2.3')
        then:
        !result.output.contains('FAILED')
        dockerOutput.contains('Hello from Docker')
        def workingDirectory = Paths.get(projectDir.absolutePath, 'build', 'dockerJava')
        Files.exists(workingDirectory.resolve('Dockerfile'))
        cleanup:
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
