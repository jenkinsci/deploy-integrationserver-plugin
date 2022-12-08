package io.jenkins.plugins.deployintegrationserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.TaskListener;

public class ProjectAutomatorUtils {
	
	
	public static Document generateDocument(String singleTargetAliases, String groupTargetAliases, String deployerHost, String deployerPort, String deployerUsername, String deployerPassword, String repositoryAlias, String repositoryDirectory, String[] composites, String projectName, String deploymentSetName, String deploymentMapName, String deploymentCandidateName) throws ParserConfigurationException {
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		Document doc = docBuilder.newDocument();
		
		Element deployerSpecElement = doc.createElement("DeployerSpec");
		deployerSpecElement.setAttribute("exitOnError", "true");
		deployerSpecElement.setAttribute("sourceType", "Repository");
		doc.appendChild(deployerSpecElement);
		
		Element deployerServerElement = doc.createElement("DeployerServer");
		Element hostElement = doc.createElement("host");
		hostElement.setTextContent(deployerHost+":"+deployerPort);
		deployerServerElement.appendChild(hostElement);
		Element userElement = doc.createElement("user");
		userElement.setTextContent(deployerUsername);
		deployerServerElement.appendChild(userElement);
		Element pwdElement = doc.createElement("pwd");
		pwdElement.setTextContent(deployerPassword);
		deployerServerElement.appendChild(pwdElement);
		deployerSpecElement.appendChild(deployerServerElement);
		
		Element environmentElement = doc.createElement("Environment");
		Element repositoryElement = doc.createElement("Repository");
		Element repaliasElement = doc.createElement("repalias");
		repaliasElement.setAttribute("name", repositoryAlias);
		Element typeElement = doc.createElement("type");
		typeElement.setTextContent("FlatFile");
		repaliasElement.appendChild(typeElement);
		Element urlOrDirectoryElement = doc.createElement("urlOrDirectory");
		urlOrDirectoryElement.setTextContent(repositoryDirectory);
		repaliasElement.appendChild(urlOrDirectoryElement);
		Element testElement = doc.createElement("Test");
		testElement.setTextContent("false");
		repaliasElement.appendChild(testElement);
		repositoryElement.appendChild(repaliasElement);
		environmentElement.appendChild(repositoryElement);
		deployerSpecElement.appendChild(environmentElement);
		
		Element projectsElement = doc.createElement("Projects");
		projectsElement.setAttribute("projectPrefix", "");
		Element projectElement = doc.createElement("Project");
		projectElement.setAttribute("description", "");
		projectElement.setAttribute("ignoreMissingDependencies", "true");
		projectElement.setAttribute("name", projectName);
		projectElement.setAttribute("overwrite", "true");
		projectElement.setAttribute("type", "Repository");
		projectsElement.appendChild(projectElement);
		Element deploymentSetElement = doc.createElement("DeploymentSet");
		deploymentSetElement.setAttribute("autoResolve", "ignore");
		deploymentSetElement.setAttribute("description", "");
		deploymentSetElement.setAttribute("name", deploymentSetName);
		deploymentSetElement.setAttribute("srcAlias", repositoryAlias);
		for(String compositeName: composites) {
			Element compositeElement = doc.createElement("Composite");
			compositeElement.setAttribute("name", compositeName);
			compositeElement.setAttribute("srcAlias", repositoryAlias);
			compositeElement.setAttribute("type", "IS");
			deploymentSetElement.appendChild(compositeElement);
		}
		projectElement.appendChild(deploymentSetElement);
		Element deploymentMapElement = doc.createElement("DeploymentMap");
		deploymentMapElement.setAttribute("description", "");
		deploymentMapElement.setAttribute("name", deploymentMapName);
		projectElement.appendChild(deploymentMapElement);
		Element mapSetMappingElement = doc.createElement("MapSetMapping");
		mapSetMappingElement.setAttribute("mapName", deploymentMapName);
		mapSetMappingElement.setAttribute("setName", deploymentSetName);
		if(singleTargetAliases != null && !singleTargetAliases.trim().equals("")) {
			String[] aliases = singleTargetAliases.split(",");
			for(String alias: aliases) {
				Element mapSetMappingAliasElement = doc.createElement("alias");
				mapSetMappingAliasElement.setAttribute("type", "IS");
				mapSetMappingAliasElement.setTextContent(alias);
				mapSetMappingElement.appendChild(mapSetMappingAliasElement);
			}
		}
		if(groupTargetAliases != null && !groupTargetAliases.trim().equals("")) {
			String[] aliases = groupTargetAliases.split(",");
			for(String alias: aliases) {
				Element mapSetMappingAliasElement = doc.createElement("group");
				mapSetMappingAliasElement.setAttribute("type", "IS");
				mapSetMappingAliasElement.setTextContent(alias);
				mapSetMappingElement.appendChild(mapSetMappingAliasElement);
			}
		}
		projectElement.appendChild(mapSetMappingElement);
		Element deploymentCandidateElement = doc.createElement("DeploymentCandidate");
		deploymentCandidateElement.setAttribute("description", "");
		deploymentCandidateElement.setAttribute("mapName", deploymentMapName);
		deploymentCandidateElement.setAttribute("name", deploymentCandidateName);
		projectElement.appendChild(deploymentCandidateElement);
		projectsElement.appendChild(projectElement);
		deployerSpecElement.appendChild(projectsElement);
		
		return doc;
	}
	
	public static void createFile(Document xmlDoc, FilePath outputDirectory) throws TransformerConfigurationException, FileNotFoundException, TransformerException, IOException, InterruptedException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(xmlDoc);
		
		FilePath outputProjectAutomatorPath = new FilePath(outputDirectory, "projectAutomator.xml");
		
		StreamResult result = new StreamResult(outputProjectAutomatorPath.write());
		transformer.transform(source, result);
	}
	
	public static int runProjectAutomatorExecutable(String operatingSystem, String deployerHomeDirectory, FilePath projectAutomatorFileDirectory, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
		
		String command = deployerHomeDirectory + File.separator + "projectautomatorUnix.sh " + projectAutomatorFileDirectory + File.separator + "projectAutomator.xml";
		if(operatingSystem.contains("windows")) {
			command = deployerHomeDirectory + File.separator + "projectautomator.bat " + projectAutomatorFileDirectory + File.separator + "projectAutomator.xml";
		} else if(operatingSystem.contains("mac")) {
			command = deployerHomeDirectory + File.separator + "projectautomatorMac.sh " + projectAutomatorFileDirectory + File.separator + "projectAutomator.xml";
		}
		
		ProcStarter ps = launcher.launch();
		Proc p = launcher.launch(ps.cmdAsSingleString(command).quiet(true).stdout(listener));
		
		int exitCode = p.join();
		return exitCode;
	}
}
