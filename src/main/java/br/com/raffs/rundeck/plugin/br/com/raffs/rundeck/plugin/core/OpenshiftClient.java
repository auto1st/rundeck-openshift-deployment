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

package br.com.raffs.rundeck.plugin.br.com.raffs.rundeck.plugin.core;

import org.json.JSONArray;
import org.json.JSONObject;

public class OpenshiftClient {

    // Class attributes
    private String serverUrl;
    private String apiVersion;
    private String project;
    private String service;
    private String token;
    private String username;
    private String password;
    private int timeout;

    private HTTPClient client;

    /**
     * Class constructor.
     */
    public OpenshiftClient() {
        this.timeout = 30;
    }

    /**
     * Responsible to instance the object inside the class.
     *
     * @return
     */
    public OpenshiftClient build() throws Exception {

        if (project == null || service == null)
            throw new Exception(
                    String.format(
                            "Either could not find the project/service %s/%s",
                            project, service
                    )
            );

        // define the authorization.
        String authorization = null;
        if (this.username != null && this.password != null)
            authorization = Utils.getToken(
                this.serverUrl, this.username, this.password, this.timeout
            );
        else authorization = "Bearer " + this.token;

        this.client = new HTTPClient()
                .withTimeout(this.timeout)
                .withBaseUrl(this.serverUrl)
                .withAuthorization(authorization)
                .build();

        return this;
    }

    /**
     * Send a GET Request to validate the servers connections
     *
     * @return
     */
    public int getServerStatus() throws Exception {
        int statusCode;

        if (client != null) {

            JSONObject response = client.get(
                    this.joinPath("/oapi", "/"));

            if (response != null) {
                statusCode = response.getInt("statusCode");
            }
            else
                throw new Exception(
                        "Error on trying to connect with: " +
                                this.joinPath("/oapi", "")
                );
        }
        else
            throw new Exception(
                    "Could not find valid open connection on " +
                            "to httpclient inside the Openshift Client class"
            );

        return statusCode;
    }

    /**
     * Responsible to get the Deployment Configuration from the resources.
     *
     * @return
     */
    public boolean checkProject(String project) throws Exception {
        if (client != null) {
            String path = this.joinPath("/oapi", "/projects/" + project);
            JSONObject response = client.get(path);

            int status;
            if ((status = response.getInt("statusCode")) != 200) {
                throw new Exception(
                     String.format(
                            "Receive error '%d' on try to validate whether the project '%s' exists: "
                                    + "openshift message => %s",
                            status, project, response.getString("message")
                     )
                );
            }
            else return true;
        }
        else
            throw new Exception(
                "Could not find any instance of http client please usage build syntax"
            );
    }

    /**
     * Return true/false whethe the service already exists or not
     * on the system.
     *
     * @param service
     * @return
     */
    public boolean checkService(String service) throws Exception {
        String path = this.joinPath("/oapi",
                String.format(
                        "/namespaces/%s/deploymentconfigs/%s",
                        project, service
                )
        );

        JSONObject response = client.get(path);
        int status;
        if ((status = response.getInt("statusCode")) != 200) {
            throw new Exception(
                    String.format(
                            "Receive error '%d' on try to validate whether the service %s/%s exists: "
                                    + "openshift message => %s",
                            status, project, service, response.getString("message")
                    )
            );
        }
        else return true;
    }

    /**
     * Return the Deployment Configuration from the instanciated object.
     *
     * @return
     * @throws Exception
     */
    public JSONObject getDeploymentConfig() throws Exception {
        JSONObject response = null;
        String path = this.joinPath("/oapi",
                String.format(
                        "/namespaces/%s/deploymentconfigs/%s",
                        project, service
                )
        );

        response = client.get(path);

        int status;
        if ((status = response.getInt("statusCode")) != 200) {
            throw new Exception(
                    String.format(
                            "Receive error '%d' on try get a Deployment Configuration on %s/%s: "
                                    + "openshift message => %s",
                            status, project, service, response.getString("message")
                    )
            );
        }

        return response;
    }

    /**
     * Given a Deployment Configuration, responsible to update the
     * Deployment Configuration from the existing file.
     *
     * @param deployConfig
     * @return
     */
    public JSONObject setDeploymentConfig(JSONObject deployConfig) throws Exception {
        JSONObject response;
        String path = this.joinPath("/oapi",
                String.format(
                        "/namespaces/%s/deploymentconfigs/%s",
                        project, service
                )
        );
        response = client.put(path, deployConfig);

        int status;
        if ((status = response.getInt("statusCode")) != 200) {
            throw new Exception(
                    String.format(
                            "Receive error '%d' on try to update a Deployment Configuration on %s/%s: "
                                    + "openshift message => %s",
                            status, project, service, response.getString("message")
                    )
            );
        }

        return response;
    }

    /**
     * Given a Deployment Configuration, responsible to update the
     * Deployment Configuration from the existing file.
     *
     * @param deployConfig
     * @return
     */
    public JSONObject createDeploymentConfig(JSONObject deployConfig) throws Exception {
        JSONObject response;
        String path = this.joinPath("/oapi",
            String.format("/namespaces/%s/deploymentconfigs", project)
        );
        response = client.post(path, deployConfig);

        int status;
        if ((status = response.getInt("statusCode")) != 200) {
            throw new Exception(
                  String.format(
                     "Receive error '%d' on try to create a new Deployment Configuration on %s/%s: "
                             + "openshift message => %s",
                         status, project, service, response.getString("message")
                     )
                );
        }

        return response;
    }

    /**
     * Responsible to watch the Deployment, basically execute
     * the call to validate whether the numbers of updated containers
     * is updated and ready to production.
     *
     * @return
     */
    public boolean notReady() throws Exception {
        // Define the replicas updates.
        int readyReplicas = 0;
        int pendingReplicas = 0;
        int failedReplicas = 0;

        // Validate the deployment configs updates pods.
        JSONObject response = getDeploymentConfig();
        JSONObject podList = getPodStatus(response);

        // go each pods and validate the status.
        if (podList.has("items")) {
            for (Object pods : podList.getJSONArray("items")) {
                JSONObject pod = (JSONObject) pods;

                if (pod.has("status")) {
                    String podStatus = pod.getJSONObject("status").getString("phase");

                    if (podStatus.equals("Pending"))
                        pendingReplicas += 1;

                    else if (podStatus.equals("Running")) {
                        if (pod.getJSONObject("status").has("conditions")) {
                            JSONArray objects = pod.getJSONObject("status").getJSONArray("conditions");

                            // Go through each conditional to find the ready status.
                            for (Object object : objects) {
                                JSONObject condition = (JSONObject) object;

                                if (condition.getString("type").equals("Ready")
                                        && condition.getString("status").equals("True")) {

                                    readyReplicas += 1;
                                }
                            }
                        }
                    }

                    else if (podStatus.equals("Failed"))
                        failedReplicas += 1;
                }
            }
        }
        else throw new Exception("Could not find any running/pending pods to validate !!!");

        // throwing error on failed replicas.
        if (failedReplicas > 0)
             throw new Exception(
                 String.format("[%d] replicas has failed deployment", failedReplicas)
             );

        // Change the deploy cycle when observed when it's finished.
        int updatesReplicas = -1;
        if (response.getJSONObject("status").has("updatedReplicas")) {
            updatesReplicas = response.getJSONObject("status").getInt("updatedReplicas");
        }

        // define whether there's the same numbers of running and updated replicas.
        return ! (pendingReplicas == 0 && readyReplicas == updatesReplicas);
    }

    /**
     * Get Stauts from the running pods.
     *
     * @param deployConfig
     * @return
     * @throws Exception
     */
    public JSONObject getPodStatus(JSONObject deployConfig) throws Exception {
        JSONObject returnResponse = null;

        // Get parameters.
        if (deployConfig.has("metadata") && deployConfig.has("status")) {
            String project = deployConfig.getJSONObject("metadata").getString("namespace");
            String resource = deployConfig.getJSONObject("metadata").getString("name");
            int version = deployConfig.getJSONObject("status").getInt("latestVersion");

            // Define the string format
            String path = String.format("/api/v1/namespaces/%s/pods?labelSelector=deployment=%s-%d",
                    project, resource, version);
            returnResponse = client.get(path);

            // Validate the request
            int statusCode = (Integer) returnResponse.get("statusCode");
            if (statusCode != 200) {

                if (statusCode == 401 || statusCode == 403) {
                    System.out.println("Unauthorized to rewrite the Deployment Configuration" +
                            "Normally this happened when there's other Deployment already running");
                }
                else if (statusCode == 404) {
                    System.out.println("Could not found Deployment Configuration");
                }
            }
        }
        else throw new Exception("Could not find metadata/status on deployment config json-schema");

        return returnResponse;
    }


    /**
     * Define the timeout connection on the Openshift API console.
     *
     * @param timeout
     * @return
     */
    public OpenshiftClient withTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Define the Openshift Console Management.
     *
     * @param url
     * @return
     */
    public OpenshiftClient withServerUrl(String url) {
        this.serverUrl = url;
        return this;
    }

    /**
     * Define the Openshift API Version.
     *
     * @param version
     * @return
     */
    public OpenshiftClient withApiVersion(String version) {
        this.apiVersion = version;
        return this;
    }

    /**
     * Define the project name identification.
     *
     * @param project
     * @return
     */
    public OpenshiftClient withProject(String project) {
        this.project = project;
        return this;
    }

    /**
     * Define the service.
     *
     * @param service
     * @return
     */
    public OpenshiftClient withService(String service) {
        this.service = service;
        return this;
    }

    /**
     * Define the username authentication.
     *
     * @param username
     * @return
     */
    public OpenshiftClient withUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Define the password authentication
     *
     * @param password
     * @return
     */
    public OpenshiftClient withPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Define the token string for Openshift Authentication.
     *
     * @param token
     * @return
     */
    public OpenshiftClient withToken(String token) {
        this.token = token;
        return this;
    }

    /**
     * Define the API path based on type and versions.
     *
     * @param api
     * @param path
     * @return
     */
    private String joinPath(String api, String path) {
        if (apiVersion != null) {
            return String.format("%s/%s%s", api, apiVersion, path);
        }
        else return String.format("%s/v1/%s", api, path);
    }

    /**
     * Define the API path based on type and versions.
     *
     * @param api
     * @param path
     * @return
     */
    private String joinPath(String path) {
        return String.format(path);
    }
}