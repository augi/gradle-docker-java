# Gradle Docker Java Plugin

[![Build Status](https://travis-ci.org/augi/gradle-docker-java.svg)](https://travis-ci.org/augi/gradle-docker-java) [ ![Download](https://api.bintray.com/packages/augi/maven/gradle-docker-java/images/download.svg) ](https://bintray.com/augi/maven/gradle-docker-java/_latestVersion)

Gradle plugin that wraps your JVM application to a new Docker image.
 The image has [standard labels](http://label-schema.org/rc1/) derived from the build environment (environment variables, Git).

The plugin takes product of `distTar` task (added by [the application plugin](https://docs.gradle.org/current/userguide/application_plugin.html)) and wraps it to Docker image.


Usage
=====
Only `image` parameter is mandatory - it's name of the resulting image.

	buildscript {
		repositories {
			jcenter()
		}
		dependencies {
			classpath 'cz.augi:gradle-docker-java:putCurrentVersionHere'
		}
	}

	apply plugin: 'docker-java'
	
	dockerJava {
        image = "myorg/my-app:$version" // name of the resulting Docker image; mandatory
        alternativeImages = ["myorg/my-app:latest"] // array of alternative image names; default is empty
        ports = [80] // list of exposed ports; default: empty
        volumes = ['/my-folder'] // list of volumes; default: empty
        baseImage = 'my-org/our-base-image:1.2.3' // default: automatically choosed the best based on current Docker platform and Java version
        javaVersion = JavaVersion.VERSION_1_8 // Java version used to choose appropriate base Docker image; default: project.targetCompatibility
        dockerfileLines = ['RUN apt-get ...'] // additional lines to include to Dockerfile; default: empty
        // username and password are used if the Docker Registry requires credentials for pushing
        username = 'registry-username'
        password = System.getenv('DOCKER_REGISTRY_PASSWORD')
        registry = 'docker.company.com' // Docker registry used to login; default: tries to extract it from 'image'
        removeImage = false // indicates if the image should be removed after publishing, default is true
	}

The plugin can be also applied using [the new Gradle syntax](https://plugins.gradle.org/plugin/cz.augi.gradle.wartremover):

    plugins {
      id 'cz.augi.docker-java' version 'putCurrentVersionHere'
    }

The plugin provides following tasks:
 * `distDocker` - creates temporary Dockerfile and build it to a new Docker image
 * `dockerPush` - pushes the Docker image to Docker Registry

The plugin executes `docker` command under the hood, so it supposes that [Docker Engine](https://www.docker.com/docker-engine) is installed and available in `PATH`.

Motivation
==========
Almost all the JVM-based applications have the same Dockerfile so why to copy or write the same Dockerfile again and again?
