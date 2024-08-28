package com.ericsson.cifwk.maven.plugin;

import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public class DatabaseUpdate {
    private final String groupId = "GroupId";
    private final String artifactId = "ArtifactId";
    private final String version = "Version";
    private final String description = "Description";
    private final String type = "Type";
    private final String intendedDrop = "IntendedDrop";
    private final String repository = "Repository";
    private final String product = "Product";
    private final String projectMediaCategory = "mediaCategory";
    private final String localProjectMediaPath = "mediaPath";
    private final String autoDeliver = "autoDeliver";
    private final String isoExclude = "isoExclude";
    private final String infra = "infra";
    private final String imageContentJSON = "imageContentJSON";
    private final String type3pp = "type3pp";
    private String errorMsg;

    Map<String, String> testPomGAV;
    Map<String, String> pkgGAV;
    List<Map<String, String>> testwareProjects;
    Log log;
    Boolean processDependencies;
    String restUrl;
    String cifwkTestwareImportRestUrl;
    String processDependenciesRestUrl;

    public DatabaseUpdate(Map<String, String> testPomGAV, List<Map<String, String>> testwareProjects, String cifwkTestwareImportRestUrl, Log log) {
        this.testPomGAV = testPomGAV;
        this.testwareProjects = testwareProjects;
        this.log = log;
        this.restUrl = cifwkTestwareImportRestUrl;
    }

    public DatabaseUpdate(Map<String, String> projectDescription, String cifwkRestURL, Log log) {
        this.pkgGAV = projectDescription;
        this.log = log;
        this.restUrl = cifwkRestURL;
    }

    public DatabaseUpdate(Map<String, String> pkgGAV, Boolean processDependencies, String processDependenciesRestUrl, Log log) throws MojoExecutionException, MojoFailureException{
        this.pkgGAV = pkgGAV;
        this.processDependencies = processDependencies;
        this.restUrl = processDependenciesRestUrl;
        this.log = log;

    }

    public void updateTestwareArtifact() throws MojoExecutionException, MojoFailureException {
        log.info("Update Testware Rest Call run on: " + restUrl);
        List<BasicNameValuePair> dbInfo = new ArrayList<BasicNameValuePair>(8);

        log.info("*** Adding The following testware Info to the database ***");
        log.info("Testware Execution ArtifactId: " + testPomGAV.get(artifactId));
        log.info("Testware Execution GroupId: " + testPomGAV.get(groupId));
        log.info("Testware Execution Version: " + testPomGAV.get(version));
        log.info("Testware Artifact: " + testwareProjects.get(0).get(artifactId));
        log.info("Testware Version: " + testwareProjects.get(0).get(version));
        log.info("Testware GroupId: " + testwareProjects.get(0).get(groupId));
        log.info("Testware Execution Description: " + testPomGAV.get(description));

        dbInfo.add(new BasicNameValuePair("execVer", testPomGAV.get(version)));
        dbInfo.add(new BasicNameValuePair("execGroupId", testPomGAV.get(groupId)));
        dbInfo.add(new BasicNameValuePair("execArtifactId", testPomGAV.get(artifactId)));
        dbInfo.add(new BasicNameValuePair("description", testPomGAV.get(description)));
        dbInfo.add(new BasicNameValuePair("testwareArtifact", testwareProjects.get(0).get(artifactId)));
        dbInfo.add(new BasicNameValuePair("version", testwareProjects.get(0).get(version)));
        dbInfo.add(new BasicNameValuePair("groupId", testwareProjects.get(0).get(groupId)));
        dbInfo.add(new BasicNameValuePair("signum", "Auto"));

        try {
            new GenericRestCalls().setUpPOSTRestCall(dbInfo, restUrl, log);
        } catch (Exception error) {
            errorMsg = "Error: Setting up Rest Call for Update Testware Error: " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
        }

    }

    public void updatePackageArtifact() throws MojoExecutionException, MojoFailureException{
        log.info("UpdatePkg Rest Call run on: " + restUrl);
        List<BasicNameValuePair> dbInfo = new ArrayList<BasicNameValuePair>(12);

        log.info("*** Adding The following package Info to the database ***");
        log.info("Version :" + pkgGAV.get(version));
        log.info("GroupId :" + pkgGAV.get(groupId));
        log.info("PackageName :" + pkgGAV.get(artifactId));
        log.info("M2Type :" + pkgGAV.get(type));
        log.info("Description :" + pkgGAV.get(description));
        log.info("Signum :mplugin");
        log.info("IntendedDrop :" + pkgGAV.get(intendedDrop));
        log.info("Product :" + pkgGAV.get(product));
        log.info("Repository :" + pkgGAV.get(repository));
        log.info("Media Category :" + pkgGAV.get(projectMediaCategory));
        log.info("Media Path :" + pkgGAV.get(localProjectMediaPath));
        log.info("Auto Deliver after KGB :" + pkgGAV.get(autoDeliver));
        log.info("Exclude from ISO :" + pkgGAV.get(isoExclude));
        log.info("Infrastructure Artifact :" + pkgGAV.get(infra));
        log.info("3pp Type :" + pkgGAV.get(type3pp));
        if (pkgGAV.get(imageContentJSON) != "") {
            log.info("Image Content:" + pkgGAV.get(imageContentJSON));
        }

        dbInfo.add(new BasicNameValuePair("version", pkgGAV.get(version)));
        dbInfo.add(new BasicNameValuePair("groupId", pkgGAV.get(groupId)));
        dbInfo.add(new BasicNameValuePair("packageName", pkgGAV.get(artifactId)));
        dbInfo.add(new BasicNameValuePair("m2Type", pkgGAV.get(type)));
        dbInfo.add(new BasicNameValuePair("description", pkgGAV.get(description)));
        dbInfo.add(new BasicNameValuePair("signum", "mplugin"));
        dbInfo.add(new BasicNameValuePair("intendedDrop", pkgGAV.get(intendedDrop)));
        dbInfo.add(new BasicNameValuePair("product", pkgGAV.get(product)));
        dbInfo.add(new BasicNameValuePair("repository", pkgGAV.get(repository)));
        dbInfo.add(new BasicNameValuePair("mediaCategory", pkgGAV.get(projectMediaCategory)));
        dbInfo.add(new BasicNameValuePair("mediaPath", pkgGAV.get(localProjectMediaPath)));
        dbInfo.add(new BasicNameValuePair("autoDeliver", pkgGAV.get(autoDeliver)));
        dbInfo.add(new BasicNameValuePair("isoExclude", pkgGAV.get(isoExclude)));
        dbInfo.add(new BasicNameValuePair("infra", pkgGAV.get(infra)));
        dbInfo.add(new BasicNameValuePair("type3pp", pkgGAV.get(type3pp)));
        if (pkgGAV.get(imageContentJSON) != "") {
            dbInfo.add(new BasicNameValuePair(imageContentJSON, pkgGAV.get(imageContentJSON)));
        }
        try {
            new GenericRestCalls().setUpPOSTRestCall(dbInfo, restUrl, log);
        } catch (Exception error) {
            errorMsg = "Error: Setting up Rest Call for Update Testware Error: " + error;
            log.error(errorMsg);
            throw new MojoFailureException(errorMsg);
 
        }

    }

    public void processDependencies() throws MojoExecutionException, MojoFailureException{
        log.info("ProcessDependencies Rest Call run on: " + restUrl);
        List<BasicNameValuePair> dbInfo = new ArrayList<BasicNameValuePair>(2);

        log.info("*** Adding The following package Info to the database ***");
        log.info("Version :" + pkgGAV.get(version));
        log.info("PackageName :" + pkgGAV.get(artifactId));

        dbInfo.add(new BasicNameValuePair("version", pkgGAV.get(version)));
        dbInfo.add(new BasicNameValuePair("packageName", pkgGAV.get(artifactId)));

        try {
            new GenericRestCalls().setUpPOSTRestCall(dbInfo, restUrl, log);
        } catch (Exception error) {
            errorMsg = "Error: Setting up Rest Call for Process Dependencies Error: " + error;
            log.error(errorMsg);
            throw new MojoExecutionException(errorMsg);
        }

    }
}
