# Rundeck Plugin - Openshift Deployment

Rundeck plugin to trigger and watch openshift deployment. 
This plugin uses the Openshift API to trigger an application deployment, and watches whether the deployment failed or succeeded. 

## Build

To build the plugin you need to have the maven tool installed: 

To build the rundeck plugin JAR file, execute: 

```sh
mvn clean install -B -V 
```
note: You can also use the `build-env` script to build the plugin by running: `./build-env build`. 

### build-env script

The build-env script is shell script to help build and test the Openshift Deployment plugin. The following action are
provided: 

* build            - Generate the JAR file with the Rundeck Plugin format
* create_container - Create a rundeck docker container for testing purposes
* deploy           - Build the rundeck plugin and copy into the Rundeck container instances. 
* clean            - Cleanup the plugin files and the created containers. 
* bash             - Open a bash console into the generated container from *create_container* option. 


# that's all folks. 
