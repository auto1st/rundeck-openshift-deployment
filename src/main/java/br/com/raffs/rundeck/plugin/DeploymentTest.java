package br.com.raffs.rundeck.plugin;

import br.com.raffs.rundeck.plugin.br.com.raffs.rundeck.plugin.core.HTTPClient;
import br.com.raffs.rundeck.plugin.br.com.raffs.rundeck.plugin.core.OpenshiftClient;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.apache.tools.ant.taskdefs.optional.ssh.Directory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONObject;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.Map;

public class DeploymentTest {

    public static void main(String [] args) {

//        // read all variables from rundeck eco-system.
//        JSONObject rundeckOptions = new JSONObject() {{
//            put("jobName", "testing");
//            put("environment", "homolog");
//        }};
//
//        JtwigTemplate template = JtwigTemplate.fileTemplate(new File("/tmp/Deployment.yml"));
//        JtwigModel model = JtwigModel.newModel()
//                .with("rundeck_param_project", "my-app")
//                .with("rundeck_param_service", "jenkins")
//                .with("rundeck", rundeckOptions);
//
//        template.render(model, System.out);

        try {

            String git_repo = "https://brasil-gitlab.alm.gsnetcloud.corp/openshift/applications.git";
            String git_dir = "/tmp/deployment";
            String branches = "master";
            String username = "xb193212";
            String password = "Rsilva1702";
            String filePath = String.format("%s/example/frontend/Deployment.yml", git_dir);
            String varFiles = String.format("%s/example/frontend/vars/development.yml", git_dir);
//
//            FileUtils.deleteDir(new File(git_dir));
//            System.out.println(String.format("Cloning repository %s ...", git_repo));
//            Git git = Git.cloneRepository()
//                    .setURI(git_repo)
//                    .setDirectory(new File(git_dir))
//                    .setBranch(branches)
//                    .setCredentialsProvider(
//                         new UsernamePasswordCredentialsProvider(username, password)
//                    ).call();
//
//            // read vars file.
//            YamlReader reader = new YamlReader(new FileReader(varFiles));
//            Object vars = reader.read();
//            Map map = (Map)vars;
//
//            JtwigTemplate template = JtwigTemplate.fileTemplate(new File(filePath));
//            JtwigModel model = JtwigModel.newModel()
//                    .with("envs", vars);
//
//            String templ = template.render(model);
//
//            // translate the YAML to json
//            YamlReader _reader = new YamlReader(templ);
//            JSONObject json = new JSONObject((Map) _reader.read());
//            System.out.println(json);
        }
        catch (Exception ex) {
            ex.printStackTrace();

        }

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

            if (! oc.checkService("jenkins")) {
                System.out.println("Unable to found the service: jenkins");

                System.out.println("Trying to create the Deployment Configuration");

                // TODO: Process the create a service.
            }
            else {

                JSONObject deployConfig = oc.getDeploymentConfig();

                System.out.println(deployConfig.getJSONObject("status").toString(2));

                int version = deployConfig.getJSONObject("status")
                        .getInt("latestVersion");

                deployConfig.getJSONObject("status")
                        .put("latestVersion", ++version);

                oc.setDeploymentConfig(deployConfig);

                // oc.watchDeployment();
            }
        }
        catch (Exception ex) {

            ex.printStackTrace();
            System.out.println("Something terrible wrong happened");
        }

    }
}
