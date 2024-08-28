package com.ericsson.cifwk.maven.plugin.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FileHandling {

    private String pathBaseDir;
    private Writer writer = null;
    private final boolean append = true;

    public void addToDependencyList(String entry, String fileName, Log log) throws MojoExecutionException, MojoFailureException{
        createDependenciesFile(entry, fileName, log);
    }

    public void createDependenciesFile(String entry, String fileName, Log log) throws MojoExecutionException, MojoFailureException{
        Path path = Paths.get(fileName);
        pathBaseDir = path.getParent().toString();
        Path basePath = Paths.get(pathBaseDir);
        if (Files.notExists(basePath)){
            File dir = new File(pathBaseDir);
            dir.mkdir();
        }
        if (Files.notExists(path)){
            File file = new File(fileName);
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.info("Issue Creating Local Dependency List File: " + file);
                e.printStackTrace();
            }
        }
        writeDependencyToFile(entry, fileName, log);
    }

    public void writeDependencyToFile(String entry, String dependencies, Log log)
            throws MojoExecutionException, MojoFailureException {

        File file = new File(dependencies);
        try {
            Scanner scanner = new Scanner(file);
            boolean found = false;
            while (scanner.hasNextLine()) {
                String lineFromFile = scanner.nextLine();
                if (entry.equals(lineFromFile)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                try {
                    writer = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(
                                    dependencies, append), "utf-8"));
                    writer.write(entry + "\n");
                } catch (IOException ex) {
                    log.error("Issue Writing Dependencies to File: "
                            + dependencies);
                    ex.printStackTrace();
                } finally {
                    try {
                        writer.close();
                    } catch (IOException ex) {
                        log.error("Issue Closing Dependency List File: "
                                + dependencies);
                        ex.printStackTrace();
                    }
                }
            }
        } catch (FileNotFoundException fileNotFound) {
            log.error("Issue with scanning through file: " + fileNotFound);
        }

    }

    public void createFileInDirectory(String fileName, Log log){

        Path path = Paths.get(fileName);
        pathBaseDir = path.getParent().toString();
        Path basePath = Paths.get(pathBaseDir);
        if (Files.notExists(basePath)){
            File dir = new File(pathBaseDir);
            dir.mkdir();
        }
        if (Files.notExists(path)){
            File file = new File(fileName);
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.info("Issue Creating Local Dependency List File: " + file);
                e.printStackTrace();
            }
        }
    }

    public String createBOMSnippletTemplateFile(String ericModuleMarker, MavenProject project,String artifactBomFileLocation, String pattern, String xpattern, Log log) throws MojoExecutionException, MojoFailureException{
        if (new File(ericModuleMarker).exists()) {
            String artifactBomFileName =  project.getArtifactId() + "-DependencyBOMSnipplet-" + project.getVersion() + ".pom";
            artifactBomFileLocation = artifactBomFileLocation + artifactBomFileName;
            if(new File(artifactBomFileLocation).exists()){
                new File(artifactBomFileLocation).delete();
            }
            FileWriter fileWriter = null;
            try {
                new FileHandling().createFileInDirectory(artifactBomFileLocation,log);
                fileWriter = new FileWriter(artifactBomFileLocation,true);
                String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n";
                List<String> projectModuleList = project.getModules();
                String projectComment = "<!-- Entry Added by: No ERIC Module Found in build -->";
                for(String module: projectModuleList){
                    if (module.matches(pattern) || module.matches(xpattern)) {
                        projectComment = "<!-- Entry Added by: "
                                + module + " " + project.getVersion() + " -->\n";
                        break;
                    }
                }
                fileWriter.write(xmlHeader + projectComment);
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return artifactBomFileLocation;
    }

    public void buildUpDependencyFiles(MavenProject project, String ericModuleMarker, String fileName, String dependencyFileName, String artifactBomFileLocation, Log log) throws MojoExecutionException, MojoFailureException{
        Collection<Artifact> artifactCollection = new ArrayList<Artifact>();
        artifactCollection.add(project.getArtifact());
        if (!new File(ericModuleMarker).exists()) {
            handleArtifacts(fileName, artifactCollection, log);
        } else {
            buildBuildTimeDependencyFile(artifactCollection, null, artifactBomFileLocation);
            handleArtifacts(fileName, artifactCollection, log);
            List<Dependency> dependencies = new ArrayList<Dependency>();
            dependencies = project.getDependencies();
            for (Dependency dependency : dependencies) {
              if( (!dependency.getGroupId().toString().contains("com.ericsson.oss")) ||
                      (dependency.getVersion().toString().toLowerCase().contains("snapshot")) ||
                      (dependency.getScope().toString().contains("test")) || (dependency.getType().toString().contains("pom")) ||
                      (dependency.getType().toString().contains("tar"))){
                  continue;
              }
              String dependencyString = dependency.getGroupId().toString() + ":" +
              dependency.getArtifactId().toString() + ":" +
              dependency.getType().toString() + ":" +
              dependency.getVersion().toString();
              new FileHandling().addToDependencyList(dependencyString, dependencyFileName, log);
            }
        }
    }

    public void buildBuildTimeDependencyFile(Collection<Artifact> artifactCollection, List<String> artifactList, String artifactBomFileLocation) throws MojoExecutionException, MojoFailureException{
        Element rootElement;
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Document document = docBuilder.newDocument();
        if(artifactCollection!=null){
            for (Artifact artifact : artifactCollection) {
                String [] artifactArray = artifact.toString().split(":");
                if( (!artifactArray[2].contains("pom")) && (!artifactArray[2].contains("rpm")) && (!artifactArray[1].contains("test")) && (!artifactArray[0].contains("test")) ){
                    rootElement = buildupSnippletXML(document, artifactArray[0], artifactArray[1], artifactArray[2], artifactArray[3]);
                } else {
                    continue;
                }
                buildDOMSource(document, rootElement, artifactBomFileLocation);
            }
        }
        if(artifactList!=null){
            for (String artifact : artifactList) {
                String [] artifactArray = artifact.split(":");
                if( (!artifactArray[2].contains("pom")) && (!artifactArray[2].contains("rpm")) && (!artifactArray[1].contains("test")) && (!artifactArray[0].contains("test")) ){
                    rootElement = buildupSnippletXML(document, artifactArray[0], artifactArray[1], artifactArray[2], artifactArray[3]);
                } else {
                    continue;
                }
                buildDOMSource(document, rootElement, artifactBomFileLocation);
            }
        }
    }

    public void buildDOMSource(Document document, Element rootElement, String artifactBomFileLocation) throws MojoExecutionException, MojoFailureException{
        DOMSource source = new DOMSource(document);
        StreamResult streamResult = null;
        try {
            streamResult = new StreamResult(new FileWriter(artifactBomFileLocation, true));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            Transformer transformer = buildXMLTransformer();
            transformer.transform(source, streamResult);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        document.removeChild(rootElement);
    }

    public Element buildupSnippletXML(Document document,String artifactGroupId,String artifactArtifactId,String artifactM2Type,String artifactArtifactVersion) throws MojoExecutionException, MojoFailureException {

        Element rootElement = document.createElement("dependency");
        document.appendChild(rootElement);

        Element XMLgroupId = document.createElement("groupId");
        XMLgroupId.appendChild(document.createTextNode(artifactGroupId));
        rootElement.appendChild(XMLgroupId);

        Element XMLartifactId = document.createElement("artifactId");
        XMLartifactId.appendChild(document.createTextNode(artifactArtifactId));
        rootElement.appendChild(XMLartifactId);

        Element XMLversion = document.createElement("version");
        XMLversion.appendChild(document.createTextNode(artifactArtifactVersion));
        rootElement.appendChild(XMLversion);

        Element XMLtype = document.createElement("type");
        XMLtype.appendChild(document.createTextNode(artifactM2Type));
        rootElement.appendChild(XMLtype);

        return rootElement;
    }

    public Transformer buildXMLTransformer() throws MojoExecutionException, MojoFailureException{
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        return transformer;
    }

    public void handleArtifacts(String file, Collection<Artifact> artifacts, Log log) throws MojoExecutionException, MojoFailureException{
        for (Artifact artifact : artifacts) {
            String[] artifactArray = artifact.toString().split(":");
            String artifactGroupId = artifactArray[0];
            String artifactArtifactId = artifactArray[1];
            String artifactM2Type = artifactArray[2];
            String artifactVersion = artifactArray[3];
            if ((!artifactM2Type.contains("pom")) && (!artifactM2Type.contains("rpm"))
                    && (!artifactArtifactId.contains("test")) && (!artifactGroupId.contains("test"))
                    && (!artifactVersion.toLowerCase().contains("snapshot"))) {
                new FileHandling().addToDependencyList(artifact.toString(), file, log);
            } 
        }
    }
}
