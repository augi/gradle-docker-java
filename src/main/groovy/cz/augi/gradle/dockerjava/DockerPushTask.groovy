package cz.augi.gradle.dockerjava

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Nested

class DockerPushTask extends DefaultTask {
    DockerPushTask() {
        this.group = 'distribution'
    }

    DockerExecutor dockerExecutor
    @Nested
    DockerPushSettings settings
}

interface DockerPushSettings {
    Project getProject()
    String getUsername()
    String getPassword()
}
