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

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

public class HTTPClient {

    // Class attributes.
    private int timeout;
    private String baseUrl;
    private String authorization;
    private OkHttpClient client;

    // Define the default media-type which is JSON
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    // Define enum-type for HTTP Methods.
    private enum HttpMethod {
        POST, PUT, GET
    }

    /**
     * Class constructor, responsible to initialize
     * all the attributes.
     */
    public HTTPClient() {
        this.timeout = 30;
        client = new OkHttpClient();
    }

    /**
     * Define the read,write and connect timeout
     * for the http client class connection.
     *
     * @param timeout time in seconds for timeout connections
     * @return the instance of the http-class.
     */
    public HTTPClient withTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Define the baseUrl of the target connection,
     * so you can simplify the path requests.
     *
     * @param url the base url used when connect within the class
     * @return the instance of HTTPClient
     */
    public HTTPClient withBaseUrl(String url) {
        this.baseUrl = url;
        return this;
    }

    /**
     * Responsible to initialize class that uses
     * parameters defined on the construction.
     *
     * @return HTTPClient instance.
     */
    public HTTPClient build() {

        // Instance the OkHttpClient on the class.
        this.client = new OkHttpClient.Builder()
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .build();

        return this;
    }

    /**
     * Define the Authorization Token String for Openshift.
     *
     * @param authorization
     * @return
     */
    public HTTPClient withAuthorization(String authorization) {
        this.authorization = authorization;
        return this;
    }

    /**
     * Responsible to send GET-METHOD Requests to the server
     * then return the results.
     *
     * @param path path on the http server
     * @return json-object with the content
     */
    public JSONObject get(String path) throws Exception {
        return this.request(path, null, HttpMethod.GET);
    }

    /**
     * Responsible to send GET-METHOD Requests to the server
     * then return the results.
     *
     * @param path path on the http server
     * @return json-object with the content
     */
    public JSONObject post(String path, JSONObject data) throws Exception {
        return this.request(path, data, HttpMethod.POST);
    }

    /**
     * Send a put request to the server.
     *
     * @param path
     * @param data
     * @return
     * @throws Exception
     */
    public JSONObject put(String path, JSONObject data) throws Exception {
        return this.request(path, data, HttpMethod.PUT);
    }

    /**
     * Responsible to send GET-METHOD Requests to the server
     * then return the results.
     *
     * @param path path on the http server
     * @return json-object with the content
     */
    private JSONObject request(String path, JSONObject data, HttpMethod type) throws Exception {
        JSONObject response;

        if (this.client == null)
            throw new Exception(
                    "Could not find valid-value for client" +
                            "Please, make sure to use build() method"
            );

        if (path == null)
            throw new Exception(
                    "Could not find a valid path, please ensure"+
                            "ensure the path is given on arguments"
            );

        RequestBody body;
        Request request;
        switch (type) {
            case POST:
                body = RequestBody.create(JSON, data.toString());
                if (this.authorization != null) {
                    request = new Request.Builder()
                            .addHeader("Authorization", this.authorization)
                            .addHeader("Content-Type", "application/json")
                            .url(this.joinPath(path))
                            .post(body)
                            .build();
                }
                else {
                    request = new Request.Builder()
                            .url(this.joinPath(path))
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();
                }
                break;

            case PUT:

                body = RequestBody.create(JSON, Utils.parserString(data.toString()));
                if (this.authorization != null) {
                    request = new Request.Builder()
                            .addHeader("Authorization", this.authorization)
                            .addHeader("Content-Type", "application/json")
                            .url(this.joinPath(path))
                            .put(body)
                            .build();
                }
                else {
                    request = new Request.Builder()
                            .url(this.joinPath(path))
                            .addHeader("Content-Type", "application/json")
                            .put(body)
                            .build();
                }
                break;

            case GET:
                if (this.authorization != null) {
                    request = new Request.Builder()
                            .header("Authorization", this.authorization)
                            .addHeader("Content-Type", "application/json")
                            .url(this.joinPath(path))
                            .build();
                }
                else {
                    request = new Request.Builder()
                            .url(this.joinPath(path))
                            .addHeader("Content-Type", "application/json")
                            .build();
                }
                break;

            default:
                throw new Exception(
                        "Could identify the TYPE must either POST/PUT/GET "+
                                "for requests."
                );

        }

        if (request != null) {

            Response httpResponse = null;
            try {
                httpResponse = client.newCall(request).execute();

                response = new JSONObject(httpResponse.body().string());
                response.put("statusCode", httpResponse.code());
            }
            catch (Exception ex) {
                ex.printStackTrace();

                response = new JSONObject();
                if (httpResponse != null)  {
                    response.put("statusCode", httpResponse.code());
                }
                else response.put("statusCode", 522);
            }
            return response;
        }
        else throw new Exception("THe request data appear to be null. ");
    }

    /**
     * Join baseUrl with the pass given on arguments
     *
     * @param suffix the path on the server
     * @return concatenated string with baseUrl/suffix
     */
    private String joinPath(String suffix) {
        return String.format(
                "%s%s", this.baseUrl.trim(), suffix.trim()
        );
    }
}