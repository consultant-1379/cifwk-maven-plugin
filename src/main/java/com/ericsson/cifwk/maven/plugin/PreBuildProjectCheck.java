package com.ericsson.cifwk.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @goal preBuildProjectCheck
 * @phase process-resources
 * @requiresProject false
 */

public class PreBuildProjectCheck extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    @Parameter
    private MavenProject project;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession mavenSession;
    
    /**
     * @parameter property="pomPropertiesDocsURL"
     *            http://confluence-nam.lmera.ericsson.se/display/CIOSS/cifwk-maven-plugin"
     */
    @Parameter
    private String pomPropertiesDocsURL;

    private final String pattern = "ERIC(.*)_CXP(.*)";
    private final String xpattern = "EXTR(.*)_CXP(.*)";
    private final String testwarePattern = "ERICTAF(.*)_CXP(.*)";
    private final String testwareTwPattern = "ERICTW(.*)_CXP(.*)";
    private String mediaCategory;
    private String errorMsg;

    public void execute() throws MojoExecutionException, MojoFailureException {
        mediaCategory = "media.category";
        for (MavenProject mavenProject : mavenSession.getProjects()){
            if (mavenProject.getArtifactId().matches(testwarePattern) || mavenProject.getArtifactId().matches(testwareTwPattern)){
                continue;
            }
            if (mavenProject.getArtifactId().matches(pattern) || mavenProject.getArtifactId().matches(xpattern)) {
                if (mavenProject.getProperties().getProperty(mediaCategory) == null){
                    errorMsg = "Error in local Project Setup, Effective POM missing <media.category> property please reference: "
                            + pomPropertiesDocsURL + " for details on how to solve this Failure.";
                    getLog().error(errorMsg);
                    throw new MojoFailureException(errorMsg);
                }
            }
        }
    }
}
