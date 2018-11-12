# mlflow-gocd
[![Build Status](https://travis-ci.org/indix/mlflow-gocd.svg?branch=master)](https://travis-ci.org/indix/mlflow-gocd)

GoCD plugins to work with MLFlow as model repository

The plugin works with a process where runs within an experiment are "promoted" for production use. A new build is triggered for each promoted run in an experiment and exposes the `artifact_uri` as an environment variable to the build.

Tested on `GoCD 17.2.0+` and `MLFlow 0.7.0`

## Configuring the repository

<p align="center">
<img src="docs/configure-repository.png" width="801px" height="400px"/>
</p>

## Configuring experiments as packages

<p align="center">
<img src="docs/configure-package.png" width="600px" height="300px"/>
</p>

## Build comment with trackback url

<p align="center">
<img src="docs/trackback.png" width="600px" height="270px"/>
</p>

## Fetching models/artifacts from mlflow

Optionally the fetch plugin can also be used in conjunction with the package plugin to fetch artifacts stored in mlflow (backed by S3.)

<p align="center">
<img src="docs/fetch.png" width="400px" height="420px"/>
</p>


