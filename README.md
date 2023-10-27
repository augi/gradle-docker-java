# Gradle Docker Java Plugin [![Build](https://github.com/augi/gradle-docker-java/actions/workflows/build.yml/badge.svg)](https://github.com/augi/gradle-docker-java/actions/workflows/build.yml) [![Version](https://badgen.net/maven/v/maven-central/cz.augi/gradle-docker-java)](https://repo1.maven.org/maven2/cz/augi/gradle-docker-java/)

Gradle plugin that wraps your JVM application to a new Docker image.
 The image has [standard labels](http://label-schema.org/rc1/) and [OCI annotations](https://github.com/opencontainers/image-spec/blob/main/annotations.md) derived from the build environment (environment variables, Git).
 Almost all the logic on Dockerfile generation is in [this file](src/main/groovy/cz/augi/gradle/dockerjava/DistDockerTask.groovy).

The plugin takes product of `distTar` task (added by [the application plugin](https://docs.gradle.org/current/userguide/application_plugin.html)) and wraps it to Docker image.
 It copies files in two steps - first, it copies all files except the application JAR. And finally, it copies the application JAR. So if you change just application code, the layer with dependencies remains the same.

The plugin just generates the `Dockerfile` and then executes the `docker` commands, so it works even on Windows with Windows Containers. This can be the reason why to use this plugin instead of https://github.com/GoogleContainerTools/jib (that doesn't support Windows containers). You could also consider https://github.com/bmuschko/gradle-docker-plugin that uses Docker remove API.

Usage
=====
The plugin is published to [Gradle Plugins portal](https://plugins.gradle.org/plugin/cz.augi.docker-java). Only the `image` parameter is mandatory - it's the name of the resulting image.

There are actually two use-cases:
1. You have your own `Dockerfile` - then you can specify path to it using `customDockerfile`, and the plugin actually just executes `docker build` to get the Docker image
2. You don't have your own `Dockerfile`, then you have to specify `baseImage` only, and the `Dockerfile` will be generated for you.
```gradle
    plugins {
        id 'cz.augi.docker-java' version 'putCurrentVersionHere'
    }
	
    dockerJava {
        image = "myorg/my-app:$version" // name of the resulting Docker image; mandatory
        alternativeImages = ["myorg/my-app:latest"] // array of alternative image names; default is empty

        baseImage = 'my-org/our-base-image:1.2.3' // required if customDockerfile is not specified
        ports = [80] // list of exposed ports; default: empty
        labels = ['mylabel':'mylabelvalue'] // additonal labels of Dockerfile; default: empty
        volumes = ['/my-folder'] // list of volumes; default: empty
        dockerfileLines = ['RUN apt-get ...'] // additional lines to include to Dockerfile; default: empty
        arguments = ['--server'] // arguments to be passed to your application; default: empty
        filesToCopy = [project.file('my-file-txt')] // list of files to copy to the Docker working directory (so these file can be copied to the image using COPY or ADD directives)

        customDockerfile = file('Dockerfile') // path to a custom Dockerfile - then all of the previous options (except image and alternativeImages) are ignored; default: null
        buildArgs = ['version=1.2.3'] // build arguments to be send to 'docker build' command when using custom Dockerfile; default: empty

        dockerBuildDirectory = project.file('my-directory') // directory where Dockerfile is created; default: "$buildDir/dockerJava"
        dockerBuildArgs = ['--isolation=hyperv'] // additional arguments to be send to 'docker build' command

        // username and password are used if the Docker Registry requires credentials for pushing
        username = 'registry-username'
        password = System.getenv('DOCKER_REGISTRY_PASSWORD')
        registry = 'docker.company.com' // Docker registry used to login; default: tries to extract it from 'image'
        removeImage = false // indicates if the image should be removed after publishing, default is true        
    }
```

The plugin provides following tasks:
 * `distDocker` - creates temporary Dockerfile and build it to a new Docker image
 * `dockerPush` - pushes the Docker image to Docker Registry

The plugin executes `docker` command under the hood, so it supposes that [Docker Engine](https://www.docker.com/docker-engine) is installed and available in `PATH`.

Motivation
==========
Almost all the JVM-based applications have the same Dockerfile so why to copy or write the same Dockerfile again and again?
