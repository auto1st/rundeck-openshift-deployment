package br.com.raffs.rundeck.plugin;

import br.com.raffs.rundeck.plugin.br.com.raffs.rundeck.plugin.core.HTTPClient;
import br.com.raffs.rundeck.plugin.br.com.raffs.rundeck.plugin.core.OpenshiftClient;
import org.json.JSONObject;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.File;

public class DeploymentTest {

    public static void main(String [] args) {

//        JtwigTemplate template = JtwigTemplate.fileTemplate(new File("/tmp/Deployment.yml"));
//        JtwigModel model = JtwigModel.newModel()
//                .with("rundeck_param_project", "my-app")
//                .with("rundeck_param_service", "jenkins");
//
//        template.render(model, System.out);


//        HTTPClient hclient = new HTTPClient()
//                .withTimeout(45)
//                .withBaseUrl("https://10.2.2.2:8443")
//                .build();
//
//        try {
//            JSONObject response = hclient.put("/oapi/v1", new JSONObject() {{
//                put("nome", "Rafael");
//            }});
//
//            if (response.getInt("statusCode") == 200) {
//                System.out.println("Post corretly");
//                System.out.println(response);
//            }
//            else {
//                System.out.println("Could not do connection? [" + response.get("statusCode") + "]");
//            }
//        }
//        catch (Exception ex) {
//
//            ex.printStackTrace();
//            System.out.println("Somthing terrible wrong just happend");
//        }

        try {

            OpenshiftClient oc = new OpenshiftClient()
                    .withApiVersion("v1")
                    .withServerUrl("https://10.2.2.2:8443")
                    .withTimeout(30)
                    .withProject("my-ap")
                    .withService("jenkins")
                    .build();

            int status = oc.getServerStatus();
            if (status == 200) {
                System.out.println("Server connection ok");
            }
            else
                System.out.println("Returned + " + status);

            if (oc.checkProject("my-ap")) {
                System.out.println("Project exists");
            }

            JSONObject deployConfig = oc.getDeploymentConfig();

            System.out.println(deployConfig.getJSONObject("status").toString(2));

            int version = deployConfig.getJSONObject("status")
                            .getInt("latestVersion");

            deployConfig.getJSONObject("status")
                        .put("latestVersion", ++version);

            oc.setDeploymentConfig(deployConfig);
        }
        catch (Exception ex) {

            ex.printStackTrace();
            System.out.println("Something terrible wrong happened");
        }
    }
}
