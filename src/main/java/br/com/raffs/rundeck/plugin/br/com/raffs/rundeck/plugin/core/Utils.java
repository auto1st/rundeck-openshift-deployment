package br.com.raffs.rundeck.plugin.br.com.raffs.rundeck.plugin.core;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    /**
     * Responsible to remove unused double-quotes on boolean, integers
     * and other values.
     */
    static String parserString(String str) {
        return str
             .replace("\"privileged\":\"false\"", "\"privileged\":false")
             .replace("\"test\":\"false\"", "\"test\":false")
             .replace("\"automatic\":\"true\"", "\"automatic\":true")
             .replace("\"port\":\"8080\"", "\"port\": 8080");
    }

    /**
     * Helper library to extract the token from the responsose.
     */
    static String extractTokenContent(String tokenContext) {
        String token = null;
        Pattern pattern = Pattern.compile("access_token=(.*?)\\&");
        Matcher matcher = pattern.matcher(tokenContext);
        while (matcher.find()) {
            token = matcher.group(1);
        }

        return token;
    }

    /**
     * Function responsible to return a token from Openshift.
     *
     * @param username
     * @param password
     * @return
     */
    static String getToken(String baseUrl, String username, String password, int timeout) {

        // Handling exception
        try {
            // encode the username and password.
            String enconding = null;
            try {
                enconding = DatatypeConverter.printBase64Binary(
                        String.format("%s:%s", username, password).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();

                throw new Exception("Error on trying to enconding the username and password");
            }

            // Define the path
            String path = String.format(
                    "%s/oauth/authorize?client_id=openshift-challenging-client&response_type=token",
                    baseUrl
            );

            // Instance the OkHttpClient on the class.
            OkHttpClient client = new OkHttpClient.Builder()
                    .writeTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .connectTimeout(timeout, TimeUnit.SECONDS)
                    .build();

            // Whether to Connect Using TOKEN Method or
            // basic Authentication Method.
            Request request;
            request = new Request.Builder()
                    .url(path)
                    .addHeader("X-CSRF-Token", "1")
                    .addHeader("Authorization", "Basic " + enconding)
                    .addHeader("Content-Type", "Application/json")
                    .build();

            // send the request and parser to JSON status
            Response response = client.newCall(request).execute();

            // Return the Authorization string from OAuth token.
            return String.format("Bearer %s", Utils.extractTokenContent(response.toString()));
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // that's it.
        return null;
    }
}
