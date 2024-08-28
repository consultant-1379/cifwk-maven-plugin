package com.ericsson.cifwk.maven.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public class DownloadArtifact {

    private final static String artifactId = "ArtifactId";
    private final static String version = "Version";
    private static String errorMsg;

    public static void downloadArtifactLocally(Map<String, String> artifactDownloadGAV, String cifwkGetArtifactUrl, Log log) throws MojoExecutionException, MojoFailureException {
        log.info("Download Artifact Locally Rest Call run on: " + cifwkGetArtifactUrl);
        List<NameValuePair> artifactInfo = new ArrayList<NameValuePair>();

        log.info("*** Adding The following artifact data to Rest Call ***");
        log.info("Version :" + artifactDownloadGAV.get(version));
        log.info("Artifact Name :" + artifactDownloadGAV.get(artifactId));

        artifactInfo.add(new BasicNameValuePair("artifactID", artifactDownloadGAV.get(artifactId)));
        artifactInfo.add(new BasicNameValuePair("version", artifactDownloadGAV.get(version)));

        try {
            new GenericRestCalls().setUpGETRestCall(artifactInfo, cifwkGetArtifactUrl, log);
        } catch (Exception error) {
            errorMsg =  "Error: Setting up Rest Call for Download Artifact Error: " + error;
            log.error(errorMsg);
            throw new MojoExecutionException(errorMsg);
        }
    }

}
