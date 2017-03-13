/**
 * Copyleft 2017 - RafaOS (rafaeloliveira.cs@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package br.com.raffs.rundeck.plugin;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONObject;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

@Plugin(name = "openshift-deploy", service = ServiceNameConstants.WorkflowStep)
@PluginDescription(
        title = "Openshift Deployment Orchestration",
        description = "Responsible to provision and deployment configuration on Openshift"
)
public class Deployment implements StepPlugin {

    // class constacts
    public final static String SERVICE_PROVIDER = "openshift-deployment";

    // define the gitlab variable for the Plugin configuration.
    @PluginProperty(
            name = "Gitlab Repository",
            description = "Define the Gitlab Repository where the Configuration will be store",
            required = true,
            scope = PropertyScope.Framework
    )
    private String gitlab_repo;             // GLOBAL

    // define the gitlab branch for the plugin configuration.
    @PluginProperty(
            name = "Gitlab Branch",
            description = "Define the branch that will be clone when Update Deployment Configuration file",
            required = true,
            defaultValue = "master",
            scope = PropertyScope.Framework
    )
    private String gitlab_branch;           // GLOBAL

    // define the gitlab username for authentication
    @PluginProperty(
            name = "Gitlab Username",
            description = "Define the username that can clone the Repository on <b>Gitlab Repo</b>",
            scope = PropertyScope.Framework,
            required = true
    )
    private String gitlab_username;         // GLOBAL

    // define the gitlab password for authentication
    @PluginProperty(
            name = "Gitlab Password",
            description = "Define the password for the user to authenticated on Gitlab Repository",
            required = true,
            scope = PropertyScope.Framework
    )
    private String gitlab_password;         // GLOBAL

    // define the directory where the repository will be clone
    @PluginProperty(
            name = "Gitlab Stage Repository",
            description = "Define where the file will be clone, when reaching the configuration file",
            required = true,
            defaultValue = "/tmp/rd-oc-deployment-dir",
            scope = PropertyScope.Framework
    )
    private String gitlab_directory;        // GLOBAL

    // define the deployment configuration file
    @RenderingOption(key = "groupName", value = "Gitlab")
    @PluginProperty(
            name = "Deployment Configuration Path",
            description = "Define the path off the Deployment Configuration file inside the Git Repository",
            required = true
    )
    private String gitlab_deployment_file;  // LOCAL

    // define the Gitlab Deployment file path
    @RenderingOption(key = "groupName", value = "Gitlab")
    @PluginProperty(
            name = "Deployment Variables Path",
            description = "Define the path off the variables file inside the Git Repository",
            required = true
    )
    private String gitlab_variable_file;    // LOCAL

    // define the openshift server url
    @PluginProperty(
            name = "Openshift Server URL",
            description = "Define the Openshift Server Url (ex: https://console.openshift.com:8443",
            required = true,
            scope = PropertyScope.Framework
    )
    private String openshift_server;        // GLOBAL

    // define the openshfit api version
    @PluginProperty(
            name = "Openshift API Version",
            description = "Define the Openshift URL version (ex: v1, v2 -> oc.example.com:8443/oapi/v1)",
            required = true,
            scope = PropertyScope.Framework
    )
    private String openshift_apiversion;    // GLOBAL

    // define the Openshfit Username
    @PluginProperty(
            name = "Openshift Username",
            description = "Openshift username to log into the server api, and call the procedures",
            scope = PropertyScope.Framework
    )
    private String openshift_username;      // GLOBAL

    // define the Openshift User password
    @PluginProperty(
            name = "Openshift Password",
            description = "Openshift user's password to log into the server api, and call the procedures",
            scope = PropertyScope.Framework
    )
    private String openshift_password;      // GLOBAL

    // define the Openshift token access
    @PluginProperty(
            name = "Openshift Access Token",
            description = "When defined, will override the username/password and use the token to access the API resources",
            scope = PropertyScope.Framework
    )
    private String openshift_token;         // GLOBAL

    // define the Openshift project name.
    @PluginProperty(
            name = "Openshift Project Name",
            description = "Define the project name where the service is located on Openshift System",
            required = true
    )
    private String openshift_project;       // LOCAL

    // define the Openshift service name
    @PluginProperty(
            name = "Openshift Service Name",
            description = "Define the Openshift service name that will be provisioned or updated",
            required = true
    )
    private String openshift_service;       // LOCAL

    // define the network timeout paramters
    @PluginProperty(
            name = "Network Timeout",
            description = "Define the Network timeout configuration (in seconds)",
            required = true,
            defaultValue = "30",
            scope = PropertyScope.Framework
    )
    private String network_timeout;             // GLOBAL

    // define the network max count attempts on watching the deployment
    @PluginProperty(
            name = "Network Max Count Attempts",
            description = "Define the network max attempts to validate the Deployment (in seconds)",
            required = true,
            defaultValue = "120",
            scope = PropertyScope.Framework
    )
    private String network_max_count_attemps;   // GLOBAL

    // define the netwowrk attempts time inverval.
    @PluginProperty(
            name = "Network Attempts Interval",
            description = "Define the time interval between the status attempts (in seconds)",
            required = true,
            defaultValue = "5",
            scope = PropertyScope.Framework
    )
    private String network_attempts_time_interval;  // GLOBAL

    /**
     * Execute a step on Node
     */
    public void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        // gitlab_repo is a required parameters.
        if (gitlab_repo == null)
            throw new StepException(
                    "Configuration failed, missing the gitlab_repo variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // gitlab_repo is a required parameters.
        if (gitlab_branch == null)
            throw new StepException(
                    "Configuration failed, missing the gitlab_branch variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // gitlab_repo is a required parameters.
        if (gitlab_username == null)
            throw new StepException(
                    "Configuration failed, missing the gitlab_username variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // gitlab_repo is a required parameters.
        if (gitlab_password == null)
            throw new StepException(
                    "Configuration failed, missing the gitlab_password variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // gitlab_repo is a required parameters.
        if (gitlab_directory == null)
            throw new StepException(
                    "Configuration failed, missing the gitlab_directory variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // openshift_Server is a required parameters.
        if (openshift_server == null)
            throw new StepException(
                    "Configuration failed, missing the openshift_server variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // openshift_apiversion is a required parameters.
        if (openshift_apiversion == null)
            throw new StepException(
                    "Configuration failed, missing the openshift_apiversion variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );


        // One of the two authentication is need
        if (openshift_username == null && openshift_password == null && openshift_token == null)
            throw new StepException(
                    "Configuration failed, missing username/password and token variable which drive the "+
                            "Openshift Authentication on Rundeck framework properties configuration file. ",
                    StepFailureReason.ConfigurationFailure
            );

        // network_timeout is a required parameters.
        if (network_timeout == null)
            throw new StepException(
                    "Configuration failed, missing the network_timeout variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // network max count attempts is a required parameters.
        if (network_max_count_attemps == null)
            throw new StepException(
                    "Configuration failed, missing the network_max_count_attempts variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );

        // network attempts time interval is a required parameters.
        if (network_attempts_time_interval == null)
            throw new StepException(
                    "Configuration failed, missing the network_attempts_time_interval variables on Rundeck Framework properties",
                    StepFailureReason.ConfigurationFailure
            );


        // Trying to Download the Deployment Defition
        try {

            // Clean-up first, remove existing cloning directory when exists.
            File tempFileDir = new File(gitlab_directory);
            if (tempFileDir.exists()) {
                FileUtils.deleteDir(tempFileDir);
            }

            // Download the files from the Gitlab, when it's the Service Definition.
            System.out.println("Sync projects definition on: " + gitlab_repo + " ...");
            Git git = Git.cloneRepository()
                    .setURI(gitlab_repo)
                    .setDirectory(tempFileDir)
                    .setBranch(gitlab_branch)
                    .setCredentialsProvider(
                         new UsernamePasswordCredentialsProvider(gitlab_username, gitlab_password)
                    ).call();

            // Define the file gitlab based location
            String varsPath = String.format("%s/%s", gitlab_deployment_file, gitlab_deployment_file);
            String deploymentPath = String.format("%s/%s", gitlab_deployment_file, gitlab_deployment_file);

            // validate the environment files.
            if (! (new File(varsPath).exists())) {
                throw new StepException(
                        String.format(
                            "Could not find the file: %s inside the repository: %s",
                                gitlab_variable_file, gitlab_repo
                        ),
                        StepFailureReason.ConfigurationFailure
                );
            }
            else if (! (new File(deploymentPath).exists())) {
                throw new StepException(
                        String.format(
                                "Could not find the file: %s inside the repository: %s",
                                gitlab_deployment_file, gitlab_repo
                        ),
                        StepFailureReason.ConfigurationFailure
                );
            }

            // TODO: Read all the rundeck paramters to passed on the template
            JSONObject rundeckVars = new JSONObject();

            // Read the environment variables into map of variables.
            YamlReader varsReader = new YamlReader(new FileReader(varsPath));
            Object vars = varsReader.read();

            //  Read the Deployment file using template-engine to validate the blocks and
            // dynamic content
            JtwigTemplate template = JtwigTemplate.fileTemplate(new File(deploymentPath));
            JtwigModel model = JtwigModel.newModel()
                    .with("vars", vars)
                    .with("rundeck", rundeckVars);

            String deploymentFile = template.render(model);

            // Return the Deployment Configuration from the YAML file.
            JSONObject deployConfig = new JSONObject(new YamlReader(deploymentFile).read());

            System.out.println("This is the Deployment Configuration");
        }
        catch (Exception ex) {
            ex.printStackTrace();

            throw new StepException(
                    "Error on trying automate Openshift Deployment & Provision",
                    StepFailureReason.PluginFailed
            );
        }
    }
}
