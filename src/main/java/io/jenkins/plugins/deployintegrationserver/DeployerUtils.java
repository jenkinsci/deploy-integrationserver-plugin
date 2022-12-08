package io.jenkins.plugins.deployintegrationserver;

import java.io.File;
import java.io.IOException;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;

public class DeployerUtils {

	public static int deployDeploymentCandidate(String operatingSystem, String deployerHomeDirectory, String deployerHost, String deployerPort, String deployerUsername, String deployerPassword, String deploymentCandidateName, String projectName, FilePath reportFileDirectory, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
		
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
		command.append(" -force -reportFilePath ");
		command.append(reportFileDirectory);
		
		ProcStarter ps = launcher.launch();
		Proc p = launcher.launch(ps.cmdAsSingleString(command.toString()).quiet(true).stdout(listener));
		
		int code = p.join();
		
		return code;
	}
}
