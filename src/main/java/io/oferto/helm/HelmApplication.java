package io.oferto.helm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.vdurmont.semver4j.Semver;

import io.oferto.helm.model.Chart;
import io.oferto.helm.model.Release;

@SpringBootApplication
public class HelmApplication implements CommandLineRunner {

	private final String HELM_COMMAND = "/opt/homebrew/bin/helm";
	private final String HELM_REPO= "chartmuseum";
	
	private static Logger LOG = LoggerFactory
		      .getLogger(HelmApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(HelmApplication.class, args);
	}

	public String updateChartRepositories() throws IOException {
		ProcessBuilder pb = new ProcessBuilder("sh", "-c", HELM_COMMAND + " repo update 2>&1; true");
		Process process = pb.start();
				
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
        StringBuffer output = new StringBuffer();
        String line;    
        
        while ((line = reader.readLine()) != null) {
        	output.append(line + "\n");
        }
        
        return output.toString();
	}
	
	public Chart[] getCharts() throws IOException {
		ProcessBuilder pb = new ProcessBuilder("sh", "-c", HELM_COMMAND + " search repo " + HELM_REPO + " -o json 2>&1; true");
		Process process = pb.start();
				
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
        StringBuffer output = new StringBuffer();
        String line;    
        
        while ((line = reader.readLine()) != null) {
        	output.append(line + "\n");
        }
        
        //System.out.printf(output.toString());
        
        Gson gson = new Gson();
        Chart[] charts = gson.fromJson(output.toString(), Chart[].class); 
        
        return charts;
	}
	
	public Release[] getReleases() throws IOException {
		ProcessBuilder pb = new ProcessBuilder("sh", "-c", HELM_COMMAND + " list -o json 2>&1; true");
		Process process = pb.start();
				
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
        StringBuffer output = new StringBuffer();
        String line;    
        
        while ((line = reader.readLine()) != null) {
        	output.append(line + "\n");
        }
        
        //System.out.printf(output.toString());
        
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        Release[] releases = gson.fromJson(output.toString(), Release[].class); 
        
        return releases;
	}
	
	public String upgradeRelease(String chartName, String releaseName) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("sh", "-c", HELM_COMMAND + " upgrade " + chartName + " " + releaseName + " 2>&1; true");
		Process process = pb.start();
				
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
        StringBuffer output = new StringBuffer();
        String line;    
        
        while ((line = reader.readLine()) != null) {
        	output.append(line + "\n");
        }
        
        return output.toString();
	}
	
	@Override
	public void run(String... args) throws Exception {		        
		try {	   
			String result;
			
			LOG.info("STEP01: Updating Chart Repositories");
			result = updateChartRepositories();
			System.out.println(result);
			
			LOG.info("STEP02: Get Charts from Helm Chart Repository");
			List<Chart> charts = Arrays.asList(getCharts());
			LOG.info("Exist " + charts.size() + " Charts published");
			System.out.println();
			
	        /*for(Chart chart : charts) {
	        	System.out.println(chart.getName());
	        }*/
	        
	        LOG.info("STEP03: Get Releases from Kubernetes Cluster");
	        List<Release> releases = Arrays.asList(getReleases());	        
	        LOG.info("Exist " + releases.size() + " Releases deployed");
	        System.out.println();
	        
	        LOG.info("STEP04: List release to be upgrade");
	        for(Release release : releases) {	        		        	
	        	Chart chart = charts.stream().filter(
	        			ch -> {
	        				Semver chartVersion = new Semver(ch.getApp_version());	        				        				
	        				Semver releaseVersion = new Semver(release.getApp_version());
	        				
	        				//System.out.printf(chartVersion.getValue());
	        				//System.out.printf(releaseVersion.getValue());
	        				
	        				return ch.getName().split("/")[1].equals(release.getName()) &&
	        					   chartVersion.isGreaterThan(releaseVersion);
	        			})
	        		.findAny()
	        		.orElse(null);
	        	
	        	if (chart != null) {
	        		System.out.println("Updating release" + chart.getName() + " to version: " + chart.getApp_version());
	        		
	        		result = upgradeRelease(release.getName(), chart.getName());
	        		
	        		System.out.println(result);
	        		System.out.println("Release" + chart.getName() + " to version: " + chart.getApp_version() + " Upgraded");
	        	}
	        } 
		} catch (Exception ex) {
			 System.out.println(ex.getMessage());
		}
	}
}

