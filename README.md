# Gradle Docker Java Plugin

[![Build Status](https://travis-ci.org/augi/gradle-docker-java.svg)](https://travis-ci.org/augi/gradle-docker-java) [ ![Download](https://api.bintray.com/packages/augi/maven/gradle-docker-java/images/download.svg) ](https://bintray.com/augi/maven/gradle-docker-java/_latestVersion)

Gradle plugin that wraps your JVM application to Docker image.

It takes product of `distTar` task (added by [the application plugin](https://docs.gradle.org/current/userguide/application_plugin.html)) and wraps to Docker image.

Usage
========================
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
        baseImage = 'my-org/our-base-image:1.2.3' // default: automatically choosed the best on based on current Docker platform
        ports = [80] // list of exposed ports; default: empty
        volumes = ['/my-folder'] // list of volumes; default: empty
        // username and password are used if the Docker Registry requires credentials for pushing
        username = 'registry-username'
        password = System.env('DOCKER_REGISTRY_PASSWORD')
	}

The plugin can be also applied using [the new Gradle syntax](https://plugins.gradle.org/plugin/cz.augi.gradle.wartremover):

    plugins {
      id 'cz.augi.gradle.docker-java' version 'putCurrentVersionHere'
    }

The plugin provides following tasks:
 * `distDocker` - creates temporary Dockerfile and build it to a new Docker image
 * `dockerPush` - pushes the Docker image to Docker Registry

The plugin executes `docker` command under the hood, so it supposes that [Docker Engine](https://www.docker.com/docker-engine) is installed and available in `PATH`.
