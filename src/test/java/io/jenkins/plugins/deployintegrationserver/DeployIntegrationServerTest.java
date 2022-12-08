package io.jenkins.plugins.deployintegrationserver;

import hudson.model.FreeStyleProject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DeployIntegrationServerTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    private String singleTargetAliases = "serveralias";
	private String groupTargetAliases = "groupalias";
	
	private String deployerHomeDirectory = "C:\\SoftwareAG\\IntegrationServer\\instances\\default\\packages\\WmDeployer\\bin";
	private String deployerHost = "localhost";
	private String deployerPort = "5555";
	private String deployerCredentialsId = "";
	
	private String repositoryAlias = "testrepoalias";
	private String repositoryDirectory = "C:\\build";
	
	private String deployAssets = "DeployTestPackage";
	
	private String projectName = "testproject";
    
    private DeployIntegrationServer deployIntegrationServer;
    
    @Before
    public void setup() throws Exception {
    	deployIntegrationServer = new DeployIntegrationServer(singleTargetAliases, groupTargetAliases, deployerHomeDirectory, deployerHost, deployerPort, deployerCredentialsId, repositoryAlias, repositoryDirectory, deployAssets, projectName);
    }
    
    @Test
    public void testConfigure() {
    	Assert.assertEquals(singleTargetAliases, deployIntegrationServer.getSingleTargetAliases());
    	Assert.assertEquals(groupTargetAliases, deployIntegrationServer.getGroupTargetAliases());
    	Assert.assertEquals(deployerHomeDirectory, deployIntegrationServer.getDeployerHomeDirectory());
    	Assert.assertEquals(deployerHost, deployIntegrationServer.getDeployerHost());
    	Assert.assertEquals(deployerPort, deployIntegrationServer.getDeployerPort());
    	Assert.assertEquals(deployerCredentialsId, deployIntegrationServer.getDeployerCredentialsId());
    	Assert.assertEquals(repositoryAlias, deployIntegrationServer.getRepositoryAlias());
    	Assert.assertEquals(deployAssets, deployIntegrationServer.getDeployAssets());
    	Assert.assertEquals(projectName, deployIntegrationServer.getProjectName());
    }
    
    @Test
    public void testConfigureRoundTrip() throws Exception {
    	FreeStyleProject project = jenkins.createFreeStyleProject();
    	project.getPublishersList().add(deployIntegrationServer);
    	project = jenkins.configRoundtrip(project);
    	jenkins.assertEqualDataBoundBeans(deployIntegrationServer, project.getPublishersList().get(0));
    } 

}