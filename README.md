[![GitHub Actions Build Status](https://github.com/jupnp/jupnp/actions/workflows/ci-build.yml/badge.svg?branch=main)](https://github.com/jupnp/jupnp/actions/workflows/ci-build.yml)
[![CDDL-1.0](https://img.shields.io/badge/license-CDDL%201.0-green.svg)](https://opensource.org/license/cddl-1-0/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jupnp/org.jupnp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jupnp/org.jupnp) 
[![Javadocs](https://www.javadoc.io/badge/org.jupnp/org.jupnp.svg)](https://www.javadoc.io/doc/org.jupnp/org.jupnp)

# Introduction

jUPnP is a Java UPnP library and has been forked from the no-longer maintained [Cling project](https://github.com/4thline/cling).

# Build Instructions

Building and running the project is fairly easy if you follow the steps detailed below.

## Prerequisites

The build infrastructure is based on Maven in order to make it
as easy as possible to get up to speed. If you know Maven already then
there won't be any surprises for you. If you have not worked with Maven
yet, just follow the instructions and everything will miraculously work ;-)

What you need before you start:
- Java SDK 11 (please note that the build does NOT work with any higher version!)
- Maven 3 from https://maven.apache.org/download.html

Make sure that the "mvn" command is available on your path

## Checkout

Checkout the source code from GitHub, e.g. by running:

```shell
git clone https://github.com/jupnp/jupnp.git
```

## Building with Maven

To build jUPnP from the sources, Maven takes care of everything:
- change into the jupnp directory (`cd jupnp`)
- run `mvn clean install` to compile and package all sources

The build result will be available in the folder `target`.

## Code style

The code style used by jUPnP is available as an Eclipse XML configuration in [codestyle.xml](tools/spotless/codestyle.xml).
To use this configuration while coding, import the code style configuration into an IDE such as Eclipse or IntelliJ.

To check if your code is following the code style run:

```shell
mvn spotless:check
```

To reformat your code so it conforms to the code style you can run:

```shell
mvn spotless:apply
```

## Integration tests

The OSGi integration tests in the "itests" directory use specific versions of bundles in the runbundles of `itest.bndrun`.
You may need to update these runbundles after creating a new jupnp release or when changing dependencies.
Maven can resolve the runbundles automatically by executing:

```shell
mvn clean install -DwithResolver
```

## Working with Eclipse

When using Eclipse ensure that the JDK is set via the `-vm` option in `eclipse.ini`.
Otherwise m2e might fail to resolve the system scoped dependency to `tools.jar`.
