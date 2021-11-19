package main.java.model.idd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import main.java.config.ServerConfig;

public class JDDParser implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7766600941754712646L;
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
		
	private String version;

	public JDDParser(String version) {
		if (version != null) {
			this.version = version;
		} else {
			// default to 8.7
			this.version = "8.7";
		}
		
		processJdd();
	}
	
	private void processJdd() {
		try {
			BufferedReader br = new BufferedReader(
						new FileReader(new File(ServerConfig.readProperty("ResourcePath") + "idd_v" + version)));
			
			Gson gson = new Gson();
			Object json = gson.fromJson(br, Object.class);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
