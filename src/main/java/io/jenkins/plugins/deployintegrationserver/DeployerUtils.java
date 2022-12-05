package io.jenkins.plugins.deployintegrationserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import hudson.FilePath;
import hudson.model.TaskListener;

public class DeployerUtils {

	public static int deployDeploymentCandidate(String operatingSystem, String deployerHomeDirectory, String deployerHost, String deployerPort, String deployerUsername, String deployerPassword, String deploymentCandidateName, String projectName, FilePath reportFileDirectory, TaskListener listener) throws IOException, InterruptedException {
		StringBuilder command = new StringBuilder();
		command.append(deployerHomeDirectory);
		command.append(File.separator);
		if(operatingSystem.contains("windows")) {
			command.append("Deployer.bat --deploy -dc ");
		} else if(operatingSystem.contains("mac")) {
			command.append("deployerMac.sh --deploy -dc ");
		} else {
			command.append("Deployer.sh --deploy -dc ");
		}
		command.append(deploymentCandidateName);
		command.append(" -project ");
		command.append(projectName);
		command.append(" -host ");
		command.append(deployerHost);
		command.append(" -port ");
		command.append(deployerPort);
		command.append(" -user ");
		command.append(deployerUsername);
		command.append(" -pwd ");
		command.append(deployerPassword);
		command.append(" -force -reportFilePath");
		command.append(reportFileDirectory);
		
		Process process = Runtime.getRuntime().exec(command.toString());
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), "UTF-8"));

			String line;
			while ((line = reader.readLine()) != null) {
				listener.getLogger().println(line);
			}
		} finally {
			if(reader != null) {
				reader.close();
			}
		}
		
		
		return process.waitFor();
	}
}
