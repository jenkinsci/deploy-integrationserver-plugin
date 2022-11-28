package io.jenkins.plugins.deployintegrationserver;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.CheckForNull;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

public class DeployIntegrationServer extends Recorder implements SimpleBuildStep {
	
	private final static String CHARSET = "UTF-8";
    
    private String serverUrl;
    private String packageName;
    private String credentialsId;
    
    @DataBoundConstructor
    public DeployIntegrationServer(String serverUrl, String packageName, String credentialsId) {
		this.serverUrl = serverUrl;
		this.packageName = packageName;
		this.credentialsId = credentialsId;
	}

	
    public String getServerUrl() {
		return serverUrl;
	}

    @DataBoundSetter
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}
    
    public String getPackageName() {
		return packageName;
	}

    @DataBoundSetter
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
    
    public String getCredentialsId() {
    	return credentialsId;
    }
    
    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
    	this.credentialsId = credentialsId;
    }


	@Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("Server URL: "+serverUrl);
        
        final String workspacePath = build.getEnvironment(listener).get("WORKSPACE");
        listener.getLogger().println("Source Directory (Workspace): "+workspacePath);
        File workspaceDirectory = new File(workspacePath);
		String[] filenames = workspaceDirectory.list();
		if(filenames == null) {
			listener.getLogger().println("No files found in workspace.");
			return false;
		} else {
			// Get credentials
			StandardUsernamePasswordCredentials credentials = null;
			if(credentialsId != null && !credentialsId.isEmpty()) {
				credentials = DescriptorImpl.lookupCredentials(build.getParent(), build.getParent().getUrl(), credentialsId);
			}
			
			if(packageName.contains(",")) { // Package name is provided as comma-separated list of packages
				listener.getLogger().println("Package List: "+packageName);
				String[] packages = packageName.split(",");
				for(String pkg: packages) {
					if(checkPackageExistInFileList(pkg, filenames)) {
						if(!uploadPackage(listener, workspacePath, pkg, credentials) || !installPackage(listener, pkg, credentials)) {
							return false;
						}
					} else {
						listener.getLogger().println("Cannot find package "+pkg+" in workspace directory.");
						return false;
					}
				}
			} else { // Package name is provided a regular expression
				for(String filename: filenames) {
					if(filename.endsWith(".zip")) {
						
						Pattern pattern = Pattern.compile(packageName);
						Matcher matcher = pattern.matcher(filename);
						if(matcher.matches()) {
							listener.getLogger().println("Filename "+filename+" matched "+packageName);
				
							if(!uploadPackage(listener, workspacePath, filename, credentials) || !installPackage(listener, filename, credentials)) {
				        		return false;
				        	}
						}
					}
				}
			}
			
			return true;
		}        
    }
	
	private boolean checkPackageExistInFileList(String pkgName, String[] filenames) {
		boolean pkgExists = false;
		for(String filename: filenames) {
			if(filename.endsWith(".zip") && filename.equals(pkgName)) {
				pkgExists = true;
				break;
			}
		}
		return pkgExists;
	}
	
	private boolean uploadPackage(BuildListener listener, String workspacePath, String fileName, StandardUsernamePasswordCredentials credentials) {
		listener.getLogger().println("Uploading package "+fileName);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(serverUrl + "/restv2/wxpackagesapis/packages");
			if(credentials != null) {
				byte[] encodedAuth = Base64.getEncoder().encode((credentials.getUsername()+":"+credentials.getPassword().getPlainText()).getBytes(CHARSET));
				httpPost.addHeader("Authorization", "Basic "+ new String(encodedAuth, CHARSET));
			}
			FileBody fileBody = new FileBody(new File(workspacePath + "\\" +fileName));
			HttpEntity requestEntity = MultipartEntityBuilder.create().addPart("file", fileBody).build();
			httpPost.setEntity(requestEntity);
			CloseableHttpResponse response = httpClient.execute(httpPost);
			try {
				if(response.getStatusLine().getStatusCode() == 201) {
					listener.getLogger().println("Package " + fileName + " uploaded successfully.");
					return true;
				} else {
					listener.getLogger().println("Package " + fileName + " upload failed.");
					listener.getLogger().println(response.getStatusLine());
					HttpEntity responseEntity = response.getEntity();
					listener.getLogger().println(EntityUtils.toString(responseEntity));
					return false;
				}
			} catch(IOException ioe) {
				listener.getLogger().println(ioe.getMessage());
				return false;
			} finally {
				if(response != null) {
					try {
						response.close();
					} catch (IOException e) {
						listener.getLogger().println(e.getMessage());
					}
				}
			}
		} catch(Exception e) {
			listener.getLogger().println(e.getMessage());
			return false;
		} finally {
			try {
				if(httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				listener.getLogger().println(e.getMessage());
			}
		}
	}
	
	private boolean installPackage(BuildListener listener, String fileName, StandardUsernamePasswordCredentials credentials) throws IOException {
		listener.getLogger().println("Installing package "+fileName);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPut httpPut = new HttpPut(serverUrl + "/restv2/wxpackagesapis/packages");
			if(credentials != null) {
				byte[] encodedAuth = Base64.getEncoder().encode((credentials.getUsername()+":"+credentials.getPassword().getPlainText()).getBytes(CHARSET));
				httpPut.addHeader("Authorization", "Basic "+ new String(encodedAuth, CHARSET));
			}
			httpPut.addHeader("Content-Type", "application/json");
			StringEntity stringEntity = new StringEntity("{\"request\": {\"packageName\": \""+ fileName +"\", \"operationName\": \"install\"}}");
			httpPut.setEntity(stringEntity);
			CloseableHttpResponse response = httpClient.execute(httpPut);
			try {
				if(response.getStatusLine().getStatusCode() == 200) {
					listener.getLogger().println("Package " + fileName + " installed successfully.");
					return true;
				} else {
					listener.getLogger().println("Package " + fileName + " installation failed.");
					listener.getLogger().println(response.getStatusLine());
					HttpEntity responseEntity = response.getEntity();
					listener.getLogger().println(EntityUtils.toString(responseEntity));
					return false;
				}
			} catch(IOException ioe) {
				listener.getLogger().println(ioe.getMessage());
				return false;
			} finally {
				if(response != null) {
					try {
						response.close();
					} catch (IOException e) {
						listener.getLogger().println(e.getMessage());
					}
				}
			}
		} catch(Exception e) {
			listener.getLogger().println(e.getMessage());
			return false;
		} finally {
			try {
				if(httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				listener.getLogger().println(e.getMessage());
			}
		}
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
    	
    	public FormValidation doCheckServerUrl(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Server URL is blank.");
    		}
    		if(value.endsWith("/")) {
    			return FormValidation.error("Server URL should not end with /");
    		}
    		return FormValidation.ok();
    	}
    	
    	public FormValidation doCheckPackageName(@QueryParameter String value) {
    		if(value.length() == 0) {
    			return FormValidation.error("Package name(s) is blank.");
    		}
    		if(value.contains(",")) {
    			if(value.contains(" ")) {
    				return FormValidation.error("White-space not allowed.");
    			}
    		} else {
    			try {
        			Pattern.compile(value);
        		} catch(PatternSyntaxException pse) {
        			return FormValidation.error("Invalid regular expression.");
        		}
    		}
    		
    		return FormValidation.ok();
    	}
    	
    	public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String url, @QueryParameter String credentialsId) {
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
