package com.ericsson.cifwk.maven.plugin;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @goal publish-artifact
 * @phase deploy
 * @requiresProject false
 */
public class IdentifyPackage extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    /**
     * @parameter property="cifwkPackageImportRestUrl"
     *            default-value="https://ci-portal.seli.wh.rnd.internal.ericsson.com/cifwkPackageImport/"
     */
    @Parameter
    private String cifwkPackageImportRestUrl;

    /**
     * @parameter property="processDependenciesRestUrl" default-value=
     *            "https://ci-portal.seli.wh.rnd.internal.ericsson.com/processReleaseTimeDependencies/"
     */
    @Parameter
    private String processDependenciesRestUrl;
    /**
     * @parameter property="cifwkGetArtifactUrl" default-value=
     *            "https://ci-portal.seli.wh.rnd.internal.ericsson.com/getArtifactFromLocalNexus/"
     */
    @Parameter
    private String cifwkGetArtifactUrl;
    /**
     * @parameter property="media.category" default-value="service"
     */
    @Parameter
    private String mediaCategory;

    /**
     * @parameter property="media.path" default-value=""
     */
    @Parameter
    private String mediaPath;

    private String cifwkInventoryPlugin = "inventory-maven-plugin";
    private Boolean processDependencies = false;
    private final String pkgProp = "publish_artifact";
    private final String soArtifact = "so_artifact";
    @SuppressWarnings("unused")
    private final String packagingProp = "packaging";
    private final String intendedDropProp = "delivery.drop";
    private final String repoProp = "release.repo";
    private final String productProp = "product";
    private final String autoDeliverProp = "auto.deliver.postkgb";
    private final String isoExcludeProp = "iso.exclude";
    private final String infraProp = "infra.artifact";
    private final String pkgType = "packaging.type";
    private final String groupId = "GroupId";
    private final String artifactId = "ArtifactId";
    private final String version = "Version";
    private final String type = "Type";
    private final String intendedDrop = "IntendedDrop";
    private final String repository = "Repository";
    private final String product = "Product";
    private final String projectMediaCategory = "mediaCategory";
    private final String projectMediaPath = "mediaPath";
    private final String localProjectMediaCategory = "media.category";
    private final String localProjectMediaPath = "media.path";
    private final String description = "Description";
    private final String autoDeliver = "autoDeliver";
    private final String isoExclude = "isoExclude";
    private final String infra = "infra";
    private final String pattern = "ERIC(.*)_CXP(.*)";
    private final String xpattern = "EXTR(.*)_CXP(.*)";
    private final String imageContentJSON = "imageContentJSON";
    private final String type3pp = "type3pp";
    private String imageContent = "";
    private Map<String, String> pkgGAV = new HashMap<String, String>();
    private Map<String, String> artifactDownloadGAV = new HashMap<String, String>();
    private String errorMsg;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (Boolean.valueOf(project.getProperties().getProperty(pkgProp))) {
            if (project.getArtifactId().matches(pattern) || project.getArtifactId().matches(xpattern) || Boolean.valueOf(project.getProperties().getProperty(soArtifact))) {
                pkgGAV.put(groupId, project.getGroupId());
                pkgGAV.put(artifactId, project.getArtifactId());
                pkgGAV.put(version, project.getVersion());
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
                if (Boolean.valueOf(project.getProperties().getProperty(soArtifact))){
                    pkgGAV.put(type3pp, "True");
                }
                else{
                    pkgGAV.put(type3pp, "false");
                }

                pkgGAV.put(imageContentJSON, imageContent);
                pkgGAV.put(infra, project.getProperties().getProperty(infraProp));

                getLog().info("Retrieving package Information: GroupId is "
                        + project.getGroupId() + ", ArtifactId is "
                        + project.getArtifactId() + ", Type is "
                        + project.getProperties().getProperty(pkgType)
                        + ", Version is " + project.getVersion()
                        + ", Media Category is " + mediaCategory
                        + ", Media Path is " + mediaPath);

                try {
                    new DatabaseUpdate(pkgGAV, cifwkPackageImportRestUrl, getLog()).updatePackageArtifact();
                } catch (Exception error) {
                    errorMsg = "Error updating database with package Information" + error;
                    getLog().error(errorMsg);
                    throw new MojoFailureException(errorMsg);
                }

                if (!Boolean.valueOf(project.getProperties().getProperty(soArtifact))) {
                    try {
                        artifactDownloadGAV.put(artifactId, project.getArtifactId());
                        artifactDownloadGAV.put(version, project.getVersion());
                        DownloadArtifact.downloadArtifactLocally(artifactDownloadGAV, cifwkGetArtifactUrl, getLog());
                    } catch (Exception error) {
                        errorMsg = "Error downloading artifact locally to populate Local Nexus with Artifact : " + error;
                        getLog().error(errorMsg);
                        throw new MojoExecutionException(errorMsg);
                    }
                }

                for (Object plugin : project.getPluginArtifacts()) {
                    if (plugin.toString().contains(cifwkInventoryPlugin)) {
                        try {
                            new DatabaseUpdate(pkgGAV, processDependencies, processDependenciesRestUrl, getLog()).processDependencies();
                        } catch (Exception error) {
                            errorMsg = "Error processing Release Time dependencies" + error;
                            getLog().error(errorMsg);
                            throw new MojoExecutionException(errorMsg);
                        }
                    }
                }
            } else {
                errorMsg = "Error: Package Name is Incorrect. Should be in the format ERICabc_CXP1234567";
                getLog().error(errorMsg);
                throw new MojoExecutionException(errorMsg);
            }
        }
    }
}
