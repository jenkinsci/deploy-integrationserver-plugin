package io.jenkins.plugins.deployintegrationserver;

import java.io.IOException;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;

public class DeployerUtils {

	public static int deployDeploymentCandidate(String operatingSystem, FilePath deployerHomeDirectory, String deployerHost, String deployerPort, String deployerUsername, String deployerPassword, String deploymentCandidateName, String projectName, FilePath reportFileDirectory, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
		FilePath deployerExecutable = new FilePath(deployerHomeDirectory, "Deployer.sh");
		if(operatingSystem.contains("windows")) {
			deployerExecutable = new FilePath(deployerHomeDirectory, "Deployer.bat");
		} else if(operatingSystem.contains("mac")) {
			deployerExecutable = new FilePath(deployerHomeDirectory, "deployerMac.sh");
		}
		if(!deployerExecutable.exists()) {
			listener.getLogger().println(deployerExecutable + " not found.");
			return -1;
		}
		
		StringBuilder command = new StringBuilder();
		command.append(deployerExecutable);
		command.append(" --deploy -dc ");
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
