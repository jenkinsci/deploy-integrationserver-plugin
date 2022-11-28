# Deploy to webMethods Integration Server Plugin

## Introduction

This plugin allows the deployment of package(s) to webMethods Integration Server/ Microservices Runtime. It takes package .zip file(s) and upload them to the remote server's replicate > inbound directory and then install the package(s).

## Getting started

As a pre-requisite, make sure "WxPackages" package is installed and enabled on the webMethods Integration Server/ Microservices Runtime.
 
Follow the steps below to configure the plugin:
1. Login to Jenkins server UI. 
2. Install the Deploy to webMethods Integration Server Plugin on the Jenkins server.
3. Create a new Jenkins job and go to "Configure" page.
4. Scroll down and choose "Deploy to webMethods Integration Server" option from the "Add post-build action" drop-down.
5. Provide Integration Server/ Microservices Runtime server base url in Server URL field. Example: http://localhost:5555
6. Provide package name(s) as a comma-separated list or regular expression that needs to be deployed to the server.
7. Choose credentials to authenticate the API calls to server to deploy package(s). By default, it requires server's Administrator credentials as Username and Password.
8. Click "Apply" and "Save" to save the job and then click "Build" to execute the job.

The package(s) that needs to be deployed must exist in the Jenkins workplace directory as .zip files.

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

