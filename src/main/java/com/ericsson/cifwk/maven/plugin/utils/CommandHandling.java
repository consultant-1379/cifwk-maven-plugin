package com.ericsson.cifwk.maven.plugin.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class CommandHandling {

    public static void getStream(Process process, Log log) {
        StringBuffer output = new StringBuffer();
        String installData = null;
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String input = "";
        try {
            while ((input = stdInput.readLine()) != null) {
                installData += input + "\n";
            }
        } catch (IOException e) {
        }
        try {
            stdInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadArtifactToRepository(String nexusHUBURL, String parentPOMGAVandFileName, Log log){
        String [] projectGavArray = parentPOMGAVandFileName.split(":");
        String deployArtifactCommand =
                 "mvn deploy:deploy-file -DgroupId="
                + projectGavArray[0] + " -DartifactId="
                + projectGavArray[1] + " -Dversion="
                + projectGavArray[2] + " -Dpackaging=bomSnipplet -DgeneratePom=false" + " -Dfile="
                + projectGavArray[3] + " -DrepositoryId=releases"
                + " -Durl=" + nexusHUBURL + "\n";
        log.info("Command that will be run to upload BOM Snipplet: " + deployArtifactCommand);
        executeCommand(deployArtifactCommand, log);
    }

    public static String executeCommand(String executeCommand, Log log) {
        Process process = null;
        String installData = "";
        try {
            process = Runtime.getRuntime().exec(executeCommand);
            ReadStream stream1 = new ReadStream("stdin",
                    process.getInputStream(), log);
            ReadStream stream2 = new ReadStream("stderr",
                    process.getErrorStream(), log);
            stream1.start();
            stream2.start();

            getStream(process, log);
            installData = stream2.getInstallData();

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                throw new MojoFailureException("Problem reading stream:");
            } catch (MojoFailureException error1) {
                log.error(error1);
                error1.printStackTrace();
            }
        } finally {
            if (process != null)
                process.destroy();
        }
        return installData;
    }
}

