package br.com.raffs.rundeck.plugin.br.com.raffs.rundeck.plugin.core;

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

        String authorization = "Bearer " + this.token;
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
                    this.joinPath("/oapi", ""));

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
            int status = client.get(path).getInt("statusCode");
            if (status != 200) {

                if (status == 403 || status == 401)
                    throw new Exception(
                            String.format(
                                    "[%d] Unauthorized to validate the project: on %s on %s",
                                    status, project, serverUrl
                            )
                    );
                else if (status == 404){
                    throw new Exception(
                            String.format(
                                    "[%d] Unauthorized to validate the project: on %s on %s",
                                    status, project, serverUrl
                            )
                    );
                }
                else
                    throw new Exception(
                            "Could not find the project instances" +
                                    " server connection returned error code: " + status
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
        int status = response.getInt("statusCode");
        if (status != 200) {
            if (status == 401)
                throw new Exception(
                        String.format(
                                "Receive error [%d] on try to return the Deployment Configuration: %s/%s",
                                status, project, service
                        )
                );

            else if (status == 403)
                throw new Exception(
                        String.format(
                                "Receive error [%d] on try to return the Deployment Configuration: %s/%s",
                                status, project, service
                        )
                );

            else if (status == 404)
                return false;

            else throw new Exception(
                        String.format(
                                "Receive error [%d] on try to return the Deployment Configuration: %s/%s",
                                status, project, service
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
            if (status == 401)
                throw new Exception(
                        String.format(
                                "Unauthorized to request the service: %s from the project %s ",
                                service, project
                        )
                );

            else if (status == 403)
                throw new Exception(
                        String.format(
                                "The service %s is forbidden to access on the project: %s",
                                service, project
                        )
                );

            else if (status == 404) {
                throw new Exception(
                        String.format(
                                "Unable to found the service: %s on project %s",
                                service, project
                        )
                );
            }

            else throw new Exception(
                        String.format(
                                "Receive error [%d] on try to return the Deployment Configuration: %s/%s",
                                status, project, service
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
            if (status == 401)
                throw new Exception(
                        String.format(
                                "Unauthorized to update the Deployment Configuration from: %s on project %s",
                                service, project
                        )
                );

            else if (status == 403)
                throw new Exception(
                        String.format(
                                "Access to update the service: %s on Project: %s",
                                service, project
                        )
                );

            else if (status == 404)
                throw new Exception(
                        String.format(
                                "The resource %s/%s could not be found on the server",
                                project, service
                        )
                );

            else throw new Exception(
                        String.format(
                                "Receive error [%d] on try to return the Deployment Configuration: %s/%s",
                                status, project, service
                        )
                );

        }

        return response;
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
     * @param servce
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
}