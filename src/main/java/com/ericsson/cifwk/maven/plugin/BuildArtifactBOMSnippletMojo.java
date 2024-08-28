package com.ericsson.cifwk.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;

import com.ericsson.cifwk.maven.plugin.utils.CommandHandling;
import com.ericsson.cifwk.maven.plugin.utils.FileHandling;

/**
 * @goal build-bomsnipplet-file
 * @phase deploy
 */

public class BuildArtifactBOMSnippletMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;

    /**
     * Base directory of the project.
     * @parameter default-value="${basedir}"
     */
    private File basedir;

    /**
     * @parameter default-value="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    /**
     * @parameter property="fileName" default-value="target/buildDependencyList"
    */
    @Parameter
    private String fileName;

    /**
     * @parameter property="cifwkRESTAPIURL" default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/api/"
    */
    @Parameter
    private String cifwkRESTAPIURL;

    private String dependencyFileName = "target/dependencyList";
    private String artifactBomFileLocation = "target/";
    private String parentPOMGAVAndFileName;
    private final String pattern = "ERIC(.*)_CXP(.*)";
    private final String xpattern = "EXTR(.*)_CXP(.*)";
    private String finalArtifactID;
    private String finalGroupId;
    private String finalVersion;
    private String nexusHUBURL;
    private String ericModuleMarker = artifactBomFileLocation + "ericmodule";

    public void execute() throws MojoExecutionException, MojoFailureException {
        try{
            Collection<Artifact> artifacts = new ArrayList<Artifact>();
            artifacts.add(project.getArtifact());
            
            File fileDir = new File(artifactBomFileLocation);
            if(!fileDir.exists()){
                fileDir.mkdir();
            }
       
            if(!project.getDistributionManagement().getRepository().getId().toString().contains("snaphot")){
                nexusHUBURL = project.getDistributionManagement().getRepository().getUrl().toString();
            }
            
            if (isThisTheExecutionRoot()){
                executionRootActions();
            } else{
                for (File file: fileDir.listFiles()){
                    if (file.getName().toLowerCase().endsWith(".pom")){
                        artifactBomFileLocation = file.toString();
                        break;
                    }
                }
            } 
            new FileHandling().buildUpDependencyFiles(project, ericModuleMarker, fileName, dependencyFileName, artifactBomFileLocation, getLog());
            finalModuleExecutions(artifactBomFileLocation);
        } catch (MojoExecutionException error) {
            getLog().error("Error getting media information from cifwk" + error);
            throw new MojoExecutionException(error.getMessage());
        } catch (MojoFailureException error) {
            getLog().error("Error getting media information from cifwk" + error);
            throw new MojoFailureException(error.getMessage());
        }
    }
    
    public boolean isThisTheExecutionRoot() throws MojoExecutionException, MojoFailureException
    {
        getLog().debug("Root Folder:" + mavenSession.getExecutionRootDirectory());
        getLog().debug("Current Folder:"+ basedir );
        boolean result = mavenSession.getExecutionRootDirectory().equalsIgnoreCase(basedir.toString());
        if (result){
            getLog().debug( "This is the execution root." );
        }
        else{
            getLog().debug( "This is NOT the execution root." );
        }
        return result;
    }

    public void executionRootActions() throws MojoExecutionException, MojoFailureException {
        List<String> moduleList = project.getModules();
        if(!new File(dependencyFileName).exists()){
            new FileHandling().createFileInDirectory(dependencyFileName,getLog());
        }
        if(!new File(fileName).exists()){
            new FileHandling().createFileInDirectory(fileName,getLog());
        }
        for(String module : moduleList){
            if( (module.toLowerCase().contains("eric")) || (module.toLowerCase().contains("extr")) ){
                new FileHandling().createFileInDirectory(ericModuleMarker,getLog());
                break;
            }
        }
        artifactBomFileLocation = new FileHandling().createBOMSnippletTemplateFile(ericModuleMarker, project, artifactBomFileLocation, pattern, xpattern, getLog());
    }

    public void finalModuleExecutions(String artifactBomFileLocation) throws MojoExecutionException, MojoFailureException{

        int count = 0;
        final int size = reactorProjects.size();
        MavenProject lastProject = (MavenProject) reactorProjects.get(size - 1);
        if (lastProject == mavenSession.getCurrentProject()) {
            List<String> artifactList = null;
            List<BasicNameValuePair> params = null;
            try {
                List<String> buildDependencyList = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
                if(new File(ericModuleMarker).exists()){
                    artifactList = getAndPostAnomalies(artifactBomFileLocation, buildDependencyList);
                } else {
                    postAnomalies(buildDependencyList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try{
                for (MavenProject mavenProject : mavenSession.getProjects()){
                    if (mavenProject.getArtifactId().matches(pattern) || mavenProject.getArtifactId().matches(xpattern)) {
                        finalGroupId = mavenProject.getGroupId();
                        finalArtifactID = mavenProject.getArtifactId();
                        finalVersion = mavenProject.getVersion();
                        count=0;
                        break;
                    } else{
                        count++;
                    }
                }
                if (count == 0){
                    parentPOMGAVAndFileName = finalGroupId
                                    + ":" + finalArtifactID
                                    + ":" + finalVersion
                                    + ":" + artifactBomFileLocation;
                    postAnomalyToArtifactVersion(artifactList);
                    if(new File(ericModuleMarker).exists()){
                        getLog().info("Beginning process of uploading BOM snipplet to Repository");
                        new CommandHandling().uploadArtifactToRepository(nexusHUBURL, parentPOMGAVAndFileName, getLog());
                    } 
                }
            } catch (Exception error) {
                getLog().error(error);
            }
        }
    }

    public List<String> getAndPostAnomalies(String artifactBomFileLocation, List<String> buildDependencyList) throws MojoExecutionException, MojoFailureException{
        List<String> artifactList = new ArrayList<String>();
        try {
            List<String> dependencyList = Files.readAllLines(Paths.get(dependencyFileName), StandardCharsets.UTF_8);
            for(String item : dependencyList){
                if(!buildDependencyList.contains(item)){
                    String [] GAVSplit = item.split(":");
                    String restPostfix = "anomalyName/" + GAVSplit[1] +
                            "/anomalyGroupId/" + GAVSplit[0] +
                            "/anomalyVersion/" + GAVSplit[3] +
                            "/anomalyPackaging/" + GAVSplit[2] + "/";
                    getLog().debug("*** Verifing That following Anomaly exists io the CIFWK database ***");
                    getLog().debug("*** " + cifwkRESTAPIURL + restPostfix + " ***");
                    String result = new GenericRestCalls().setUpGETRestCallWithString(cifwkRESTAPIURL + restPostfix, getLog());
                    getLog().debug("*** " + result + " ***");
                    if(result.contains("true")){
                        artifactList.add(GAVSplit[0] + ":" + GAVSplit[1] + ":" + GAVSplit[2] + ":" + GAVSplit[3]);
                    }
                }
            }
            if(!artifactList.isEmpty()){
                new FileHandling().buildBuildTimeDependencyFile(null, artifactList, artifactBomFileLocation);
            }
            
        }  catch (IOException e) {
            e.printStackTrace();
        }
        return artifactList;
    }
    
    public void postAnomalies(List<String> buildDependencyList) throws MojoExecutionException, MojoFailureException{
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        List<String> anomalyList = new ArrayList<String>();
        String anomalies = "";
        for(String item : buildDependencyList){
            anomalyList.add(item);
            anomalies = anomalies + item + ",";
        }
        getLog().debug("*** Adding The following Anomaly List Info to the CIFWK database ***");
        for(String item : anomalyList){
            getLog().debug("Anomaly: " + item);
        }
        params.clear();
        if(!anomalies.isEmpty()){
            params.add(new BasicNameValuePair("anomalyList", anomalies.substring(0,anomalies.length() - 1)));
            new GenericRestCalls().setUpPOSTRestCall(params, cifwkRESTAPIURL + "processAnomalies/", getLog());
        }
    }
    
    public void postAnomalyToArtifactVersion(List<String> artifactList) throws MojoExecutionException, MojoFailureException{
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        if(!artifactList.isEmpty()){
            String artifactMapping = "";
            getLog().debug("*** Adding The following Artifact Mapping Info to the CIFWK database ***");
            for(String artifact :artifactList){
                if(!finalVersion.toLowerCase().contains("snapshot")){
                    getLog().debug("Artifact Mapping : " + artifactMapping + "#" + finalGroupId + ":" + finalArtifactID + ":" + finalVersion);
                    artifactMapping = artifactMapping + artifact + "#" + finalGroupId + ":" + finalArtifactID + ":" + finalVersion + ",";
                }
            }
            if(!artifactMapping.isEmpty()){
                params.add(new BasicNameValuePair("mappingList", artifactMapping.substring(0,artifactMapping.length() - 1)));
                new GenericRestCalls().setUpPOSTRestCall(params, cifwkRESTAPIURL + "processAnomalyPackageRevisionMapping/", getLog());
            }
        }
    }
}