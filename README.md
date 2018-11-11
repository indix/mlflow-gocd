# mlflow-gocd
[![Build Status](https://travis-ci.org/indix/mlflow-gocd.svg?branch=master)](https://travis-ci.org/indix/mlflow-gocd)

GoCD plugins to work with MLFlow as model repository

The plugin works with a process where runs within an experiment are "promoted" for production use. A new build is triggered for each promoted run in an experiment and exposes the `artifact_uri` as an environment variable to the build.

## Configuring the repository

<p align="center">
<img src="docs/configure-repository.png"/>
</p>

## Configuring experiments as packages

<p align="center">
<img src="docs/configure-package.png"/>
</p>

## Build comment with trackback url

<p align="center">
<img src="docs/trackback.png"/>
</p>


