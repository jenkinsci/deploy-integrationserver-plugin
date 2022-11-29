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
    
    final String serverUrl = "http://localhost:5555";
    final String packageName = "CustomPackage.zip";
    final String credentialsId = "";
    
    private DeployIntegrationServer deployIntegrationServer;
    
    @Before
    public void setup() throws Exception {
    	deployIntegrationServer = new DeployIntegrationServer(serverUrl, packageName, credentialsId);
    }
    
    @Test
    public void testConfigure() {
    	Assert.assertEquals(serverUrl, deployIntegrationServer.getServerUrl());
    	Assert.assertEquals(packageName, deployIntegrationServer.getPackageName());
    	Assert.assertEquals(credentialsId, deployIntegrationServer.getCredentialsId());
    }
    
    @Test
    public void testConfigureRoundTrip() throws Exception {
    	FreeStyleProject project = jenkins.createFreeStyleProject();
    	project.getPublishersList().add(deployIntegrationServer);
    	project = jenkins.configRoundtrip(project);
    	jenkins.assertEqualDataBoundBeans(deployIntegrationServer, project.getPublishersList().get(0));
    } 

}