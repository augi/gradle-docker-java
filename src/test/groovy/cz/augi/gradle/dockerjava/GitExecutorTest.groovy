package cz.augi.gradle.dockerjava

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GitExecutorTest extends Specification {
    def "reads url, ref and branch"() {
        def project = ProjectBuilder.builder().build()
        def target = new GitExecutor(project)
        target.execute('init')
        project.file('file.txt') << 'content'
        target.execute('checkout', '-b', 'master')
        target.execute('add', '.')
        target.execute('commit', '-m', 'first commit')
        target.execute('remote', 'add', 'origin', 'https://github.com/test/test')
        when:
        def url = target.url
        def ref = target.ref
        def branch = target.branch
        then:
        url == 'https://github.com/test/test'
        ref.size() == 40
        branch == 'master'
        cleanup:
        project.projectDir.deleteDir()
    }
}
