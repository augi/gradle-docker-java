# Gradle Docker Java Plugin

[![Build Status](https://travis-ci.org/augi/gradle-docker-java.svg)](https://travis-ci.org/augi/gradle-docker-java) [ ![Download](https://api.bintray.com/packages/augi/maven/gradle-docker-java/images/download.svg) ](https://bintray.com/augi/maven/gradle-docker-java/_latestVersion)

Gradle plugin that wraps your JVM application to a new Docker image.

It takes product of `distTar` task (added by [the application plugin](https://docs.gradle.org/current/userguide/application_plugin.html)) and wraps it to Docker image.

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
        image = "my-org/my-app:$version" // name of the resulting Docker image; mandatory
        ports = [80] // list of exposed ports; default: empty
        volumes = ['/my-folder'] // list of volumes; default: empty
        baseImage = 'my-org/our-base-image:1.2.3' // default: automatically choosed the best based on current Docker platform
        dockerfileLines = ['RUN apt-get ...'] // additional lines to include to Dockerfile; default: empty
        // username and password are used if the Docker Registry requires credentials for pushing
        username = 'registry-username'
        password = System.getenv('DOCKER_REGISTRY_PASSWORD')
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
