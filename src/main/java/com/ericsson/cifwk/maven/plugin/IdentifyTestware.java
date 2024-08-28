package com.ericsson.cifwk.maven.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @goal publish-taf-testware
 * @phase deploy
 * @requiresProject false
 */

public class IdentifyTestware extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    /**
     * @parameter property="cifwkTestwareImportRestUrl" default-value=
     *            "https://ci-portal.seli.wh.rnd.internal.ericsson.com/cifwkTestwareImport/"
     */
    @Parameter
    private String cifwkTestwareImportRestUrl;
    /**
     * @parameter property="cifwkGetArtifactUrl" default-value=
     *            "https://ci-portal.seli.wh.rnd.internal.ericsson.com/getArtifactFromLocalNexus/"
     */
    @Parameter
    private String cifwkGetArtifactUrl;

    /**
     * @parameter property="cifwkPackageImportRestUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/cifwkPackageImport/"
     */
    @Parameter
    private String cifwkPackageImportRestUrl;

    /**
     * @parameter property="media.path" default-value=""
     */
    @Parameter
    private String mediaPath;

    private final String testProp = "taf_testware";
    private final String groupId = "GroupId";
    private final String artifactId = "ArtifactId";
    private final String version = "Version";
    private final String description = "Description";
    private final String pattern = "ERICTAF(.*)_CXP(.*)";
    private final String twPattern = "ERICTW(.*)_CXP(.*)";
    private Map<String, String> testPomGAV = new HashMap<String, String>();
    private List<Map<String, String>> testwareProjectGAVList = new ArrayList<Map<String, String>>();

    private final String intendedDropProp = "delivery.drop";
    private final String repoProp = "release.repo";
    private final String productProp = "product";
    private final String autoDeliverProp = "auto.deliver.postkgb";
    private final String isoExcludeProp = "iso.exclude";
    private final String infraProp = "infra.artifact";
    private final String pkgType = "packaging.type";
    private final String type = "Type";
    private final String intendedDrop = "IntendedDrop";
    private final String repository = "Repository";
    private final String product = "Product";
    private final String projectMediaCategory = "mediaCategory";
    private final String projectMediaPath = "mediaPath";
    private final String localProjectMediaCategory = "media.category";
    private final String localProjectMediaPath = "media.path";
    private final String autoDeliver = "autoDeliver";
    private final String isoExclude = "isoExclude";
    private final String infra = "infra";
    private final String imageContentJSON = "imageContentJSON";
    private String imageContent = "";
    private String mediaCategory = "testware";
    private String testwareGroupID = "";
    private String testwareArtifactID = "";
    private String testwareVersion = "";
    private String errorMsg;
    private Boolean artifactFound = false;
    

    private Map<String, String> artifactDownloadGAV = new HashMap<String, String>();
    private Map<String, String> pkgGAV = new HashMap<String, String>();

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (Boolean.valueOf(project.getProperties().getProperty(testProp))) {
            testPomGAV.put(groupId, project.getGroupId());
            testPomGAV.put(artifactId, project.getArtifactId());
            testPomGAV.put(version, project.getVersion());
            testPomGAV.put(description, project.getDescription());
            getLog().info("Retrieving test-pom GAV: GroupId is " + project.getGroupId() + ", ArtifactId is " + project.getArtifactId() + ", Version is " + project.getVersion());

            @SuppressWarnings("unchecked")
            Set<Artifact> artifacts = project.getDependencyArtifacts();
            for (Artifact artifact : artifacts) {
                if (artifact.getArtifactId().matches(pattern) || artifact.getArtifactId().matches(twPattern)){
                    Map<String, String> moduleGAV = new HashMap<String, String>();
                    testwareGroupID = artifact.getGroupId();
                    testwareArtifactID = artifact.getArtifactId();
                    testwareVersion = artifact.getVersion();
                    moduleGAV.put(groupId, testwareGroupID);
                    moduleGAV.put(artifactId, testwareArtifactID);
                    moduleGAV.put(version, testwareVersion);
                    getLog().info("Adding Module GAV: GroupId is " + artifact.getGroupId() + ", ArtifactId is " + artifact.getArtifactId() + ", Version is " + artifact.getVersion());
                    testwareProjectGAVList.add(moduleGAV);
                    artifactFound = true;
                    break;
                }
            }
            if (artifactFound == false) {
                errorMsg = "Error: Testware Artifact Name is Incorrect or no Artifact Found. Should be in the format ERICTAFabc_CXP1234567";
                getLog().error(errorMsg);
                throw new MojoFailureException(errorMsg);
            }

            try {
                new DatabaseUpdate(testPomGAV, testwareProjectGAVList, cifwkTestwareImportRestUrl, getLog()).updateTestwareArtifact();
            } catch (Exception error) {
                errorMsg = "Error updating database with testware Information" + error;
                getLog().error(errorMsg);
                throw new MojoFailureException(errorMsg);


            }
            try {
                artifactDownloadGAV.put(artifactId, project.getArtifactId());
                artifactDownloadGAV.put(version, project.getVersion());
                DownloadArtifact.downloadArtifactLocally(artifactDownloadGAV, cifwkGetArtifactUrl, getLog());
            } catch (Exception error) {
                errorMsg = "Error downloading artifact locally to populate Local Nexus with Artifact : " + error;
                getLog().error(errorMsg);
                throw new MojoFailureException(errorMsg);
            }

            pkgGAV.put(groupId, testwareGroupID);
            pkgGAV.put(artifactId, testwareArtifactID);
            pkgGAV.put(version, testwareVersion);
            pkgGAV.put(type, project.getProperties().getProperty(pkgType));
            pkgGAV.put(intendedDrop, project.getProperties().getProperty(intendedDropProp));
            pkgGAV.put(repository, project.getProperties().getProperty(repoProp));
            pkgGAV.put(product, project.getProperties().getProperty(productProp));
            pkgGAV.put(autoDeliver, project.getProperties().getProperty(autoDeliverProp));
            pkgGAV.put(isoExclude, project.getProperties().getProperty(isoExcludeProp));
            pkgGAV.put(description, project.getDescription());
            if (project.getProperties().getProperty(localProjectMediaCategory) != null) {
                mediaCategory = project.getProperties().getProperty(localProjectMediaCategory);
            }
            pkgGAV.put(projectMediaCategory, mediaCategory);
            if (project.getProperties().getProperty(localProjectMediaPath) != null) {
                mediaPath = project.getProperties().getProperty(localProjectMediaPath);
            }
            pkgGAV.put(projectMediaPath, mediaPath);
            if (project.getProperties().getProperty("imageContentJSON") != null) {
                imageContent = project.getProperties().getProperty("imageContentJSON");
            }
            pkgGAV.put(imageContentJSON, imageContent);
            pkgGAV.put(infra, project.getProperties().getProperty(infraProp));

            getLog().info("Retrieving package Information: GroupId is " + testwareGroupID + ", ArtifactId is " + testwareArtifactID + ", Type is " + project.getProperties().getProperty(pkgType) + ", Version is " + testwareVersion + ", Media Category is " + mediaCategory + ", Media Path is " + mediaPath);
            try {
                new DatabaseUpdate(pkgGAV, cifwkPackageImportRestUrl, getLog()).updatePackageArtifact();
            } catch (Exception error) {
                errorMsg = "Error updating database with package Information" + error;
                getLog().error(errorMsg);
                throw new MojoFailureException(errorMsg);
            }

        }
    }
}
