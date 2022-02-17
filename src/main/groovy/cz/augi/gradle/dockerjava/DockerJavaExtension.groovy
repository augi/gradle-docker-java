package cz.augi.gradle.dockerjava

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.Internal

class DockerJavaExtension implements DistDockerSettings, DockerPushSettings {
    @Internal final Project project

    DockerJavaExtension(Project project) {
        this.project = project
        this.dockerBuildDirectory = new File(project.buildDir, 'dockerJava')
        this.dockerBuildDirectory.mkdirs()
    }

    String image
    String[] alternativeImages = []
    String baseImage
    Integer[] ports = []
    String[] volumes = []
    Map<String, String> labels = [:]
    String[] dockerfileLines = []
    String[] arguments = []
    String[] dockerBuildArgs = []
    File dockerBuildDirectory
    File[] filesToCopy = []
    File customDockerfile
    String[] buildArgs = []

    String username
    String password
    String getRegistry() {
        if (customRegistry) return customRegistry
        if (image.indexOf('/') < 0) return ''
        // if the part before first slash contains dot then it's private Docker Registry
        def potentialRegistry = image.substring(0, image.indexOf('/'))
        if (potentialRegistry.contains('.')) potentialRegistry else ''
    }
    void setRegistry(String registry) { customRegistry = registry }
    private String customRegistry
    Boolean removeImage = true
}
