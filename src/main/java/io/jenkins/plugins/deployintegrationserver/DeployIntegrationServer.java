package io.jenkins.plugins.deployintegrationserver;

import hudson.Launcher;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.w3c.dom.Document;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

public class DeployIntegrationServer extends Recorder implements SimpleBuildStep {
	
	private String singleTargetAliases;
	private String groupTargetAliases;
	
	private String deployerHomeDirectory;
	private String deployerHost;
	private String deployerPort;
	private String deployerCredentialsId;
	
	private String repositoryAlias;
	private String repositoryDirectory;
	
	private String composites;
	
	private String projectName;
    
    @DataBoundConstructor
    public DeployIntegrationServer(String singleTargetAliases, String groupTargetAliases, String deployerHomeDirectory, String deployerHost, String deployerPort, String deployerCredentialsId, String repositoryAlias, String repositoryDirectory, String composites, String projectName) {
		this.singleTargetAliases = singleTargetAliases;
		this.groupTargetAliases = groupTargetAliases;
		this.deployerHomeDirectory = deployerHomeDirectory;
		this.deployerHost = deployerHost;
		this.deployerPort = deployerPort;
		this.deployerCredentialsId = deployerCredentialsId;
		this.repositoryAlias = repositoryAlias;
		this.repositoryDirectory = repositoryDirectory;
		this.composites = composites;
		this.projectName = projectName;
	}

	public String getSingleTargetAliases() {
		return singleTargetAliases;
	}

	@DataBoundSetter
	public void setSingleTargetAliases(String singleTargetAliases) {
		this.singleTargetAliases = singleTargetAliases;
	}

	public String getGroupTargetAliases() {
		return groupTargetAliases;
	}

	@DataBoundSetter
	public void setGroupTargetAliases(String groupTargetAliases) {
		this.groupTargetAliases = groupTargetAliases;
	}

	public String getDeployerHost() {
		return deployerHost;
	}

	@DataBoundSetter
	public void setDeployerHost(String deployerHost) {
		this.deployerHost = deployerHost;
	}

	public String getDeployerCredentialsId() {
		return deployerCredentialsId;
	}

	@DataBoundSetter
	public void setDeployerCredentialsId(String deployerCredentialsId) {
		this.deployerCredentialsId = deployerCredentialsId;
	}

	public String getRepositoryAlias() {
		return repositoryAlias;
	}

	@DataBoundSetter
	public void setRepositoryAlias(String repositoryAlias) {
		this.repositoryAlias = repositoryAlias;
	}

	public String getRepositoryDirectory() {
		return repositoryDirectory;
	}

	@DataBoundSetter
	public void setRepositoryDirectory(String repositoryDirectory) {
		this.repositoryDirectory = repositoryDirectory;
	}

	public String getDeployerHomeDirectory() {
		return deployerHomeDirectory;
	}

	@DataBoundSetter
	public void setDeployerHomeDirectory(String deployerHomeDirectory) {
		this.deployerHomeDirectory = deployerHomeDirectory;
	}

	public String getComposites() {
		return composites;
	}

	@DataBoundSetter
	public void setComposites(String composites) {
		this.composites = composites;
	}

	public String getProjectName() {
		return projectName;
	}

	@DataBoundSetter
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getDeployerPort() {
		return deployerPort;
	}

	@DataBoundSetter
	public void setDeployerPort(String deployerPort) {
		this.deployerPort = deployerPort;
	}

	@Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		StandardUsernamePasswordCredentials deployerCredentials = DescriptorImpl.lookupCredentials(run.getParent(), run.getParent().getUrl(), deployerCredentialsId);
		String operatingSystem = System.getProperty("os.name").toLowerCase();
		
		String deploymentSetName = projectName + "DS";
		String deploymentMapName = projectName + "DM";
		String deploymentCandidateName = projectName + "DC";
		
		// Generate Project Automator XML Document
		Document projectAutomatorXMLDoc = null;
		try {
			projectAutomatorXMLDoc = ProjectAutomatorUtils.generateDocument(singleTargetAliases, groupTargetAliases, deployerHost, deployerPort, deployerCredentials.getUsername(), deployerCredentials.getPassword().getPlainText(), repositoryAlias, repositoryDirectory, composites.split(","), projectName, deploymentSetName, deploymentMapName, deploymentCandidateName);
		} catch (ParserConfigurationException pce) {
			listener.getLogger().println(pce.getMessage());
			throw new AbortException();
		}
		
		// Create Project Automator File in Workspace
		try {
			ProjectAutomatorUtils.createFile(projectAutomatorXMLDoc, workspace);
		} catch (TransformerException te) {
			listener.getLogger().println(te.getMessage());
			throw new AbortException();
		} catch (IOException ioe) {
			listener.getLogger().println(ioe.getMessage());
			throw new AbortException();
		}
		
		// Run Project Automator Executable
		try {
			int exitStatusCode = ProjectAutomatorUtils.runProjectAutomatorExecutable(operatingSystem, deployerHomeDirectory, workspace, listener);
			listener.getLogger().println("Project Automator exited with status: "+ exitStatusCode);
			if(exitStatusCode != 0) {
				throw new AbortException();
			}
		} catch(IOException ioe) {
			listener.getLogger().println(ioe.getMessage());
			throw new AbortException();
		} catch(InterruptedException ie) {
			listener.getLogger().println(ie.getMessage());
			throw new AbortException();
		}
		
		// Run Deployer Executable
		try {
			int statusCode = DeployerUtils.deployDeploymentCandidate(operatingSystem, deployerHomeDirectory, deployerHost, deployerPort, deployerCredentials.getUsername(), deployerCredentials.getPassword().getPlainText(), deploymentCandidateName, projectName, workspace, listener);
			listener.getLogger().println("Deployer exited with status: "+statusCode);
			if(statusCode != 0) {
				throw new AbortException();
			}
		} catch(IOException ioe) {
			listener.getLogger().println(ioe.getMessage());
			throw new AbortException();
		} catch (InterruptedException ie) {
			listener.getLogger().println(ie.getMessage());
			throw new AbortException();
		}
		
		listener.getLogger().println("Deployment successful.");
    }
	
	

    @Symbol("deployintegrationserver")
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		/**
	     * In order to load the persisted global configuration, you have to
	     * call load() in the constructor.
	     */
    	public DescriptorImpl() {
			load();
		}
    	
    	public FormValidation doCheckDeployerHomeDirectory(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Cannot be blank.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckDeployerHost(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Cannot be blank.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckDeployerPort(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Cannot be blank.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckSingleTargetAliases(@QueryParameter String value, @QueryParameter String groupTargetAliases) {
    		if(value.length() == 0 && groupTargetAliases.length() == 0) {
    			return FormValidation.error("Server and Group alias both cannot be blank.");
    		}
    		if(value.contains(" ")) {
    			return FormValidation.error("Cannot contain white-space.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckGroupTargetAliases(@QueryParameter String value, @QueryParameter String singleTargetAliases) {
    		if(value.length() == 0 && singleTargetAliases.length() == 0) {
    			return FormValidation.error("Group and Server alias both cannot be blank.");
    		}
    		if(value.contains(" ")) {
    			return FormValidation.error("Cannot contain white-space.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckRepositoryDirectory(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Cannot be blank.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckComposites(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Cannot be blank.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckProjectName(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Cannot be blank.");
    		}
    		return FormValidation.ok();
    	}
    	public FormValidation doCheckDeployerCredentialsId(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Cannot be none.");
    		}
    		return FormValidation.ok();
    	}
    	
    	public ListBoxModel doFillIsCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String url, @QueryParameter String credentialsId) {
    		StandardListBoxModel result = new StandardListBoxModel();
    		
    		if(item == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) || item != null && !item.hasAnyPermission(Item.EXTENDED_READ)) {
    			return result.includeCurrentValue(credentialsId);
    		} else {
    			return new StandardListBoxModel()
    	                .includeEmptyValue()
    	                .includeMatchingAs(
    	                        item instanceof Queue.Task
    	                                ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
    	                        item, StandardUsernameCredentials.class, URIRequirementBuilder.fromUri(url).build(),
    	                        CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class))
    	                .includeCurrentValue(credentialsId);
    		}
    	}
    	
    	public ListBoxModel doFillDeployerCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String url, @QueryParameter String credentialsId) {
    		StandardListBoxModel result = new StandardListBoxModel();
    		
    		if(item == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER) || item != null && !item.hasAnyPermission(Item.EXTENDED_READ)) {
    			return result.includeCurrentValue(credentialsId);
    		} else {
    			return new StandardListBoxModel()
    	                .includeEmptyValue()
    	                .includeMatchingAs(
    	                        item instanceof Queue.Task
    	                                ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
    	                        item, StandardUsernameCredentials.class, URIRequirementBuilder.fromUri(url).build(),
    	                        CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class))
    	                .includeCurrentValue(credentialsId);
    		}
    	}
    	
    	protected static StandardUsernamePasswordCredentials lookupCredentials(@CheckForNull Item item, String url, String credentialId) {
            return (credentialId == null) ? null : CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, item, ACL.SYSTEM, URIRequirementBuilder.fromUri(url).build()),
                    CredentialsMatchers.withId(credentialId));
        }
    	

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        	// Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Deploy to webMethods Integration Server";
        }

    }

}
