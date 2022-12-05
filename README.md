# Deploy to webMethods Integration Server Plugin

## Introduction

This plugin allows deployment of webMethods Integration Server composites to the target webMethods Integration Server.

## Getting started

As a pre-requisite, make sure you have WmDeployer package installed and Jenkins has access to the project automator and deployer executables in the file system. These executables exist in the WmDeployer package's bin directory.
 
Follow the steps below to configure the plugin:
1. Login to Jenkins server UI. 
2. Install the Deploy to webMethods Integration Server Plugin on the Jenkins server.
3. Create a new Jenkins job and go to "Configure" page.
4. Scroll down and choose "Deploy to webMethods Integration Server" option from the "Add post-build action" drop-down.
5. Provide the Repository details and assets that need to be deployed.
6. Provide the Deployer details.
7. Provide the Target details.
8. Click "Apply" and "Save" to save the job and then click "Build" to execute the job.

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

