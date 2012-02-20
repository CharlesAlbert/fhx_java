package com.fhx.statstream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.marketcetera.marketdata.interactivebrokers.LatestMarketData;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;

import com.fhx.util.StatStreamUtil;


public abstract class StatStreamServiceBase {

	private static Logger log = Logger.getLogger(StatStreamServiceBase.class);
	private final Properties config = new Properties();
	
	protected int basicWindowSize = -1;
	
	protected static RConnection conn;  // have a global R connection handler for simplicity
	
	public StatStreamServiceBase() {
	}
	
	public void init() {
		PropertyConfigurator.configure("conf/log4j.properties");
		
		try {
			config.load(new FileInputStream("conf/statstream.properties"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		String basicWin = config.getProperty("BASIC_WINDOW_SIZE");
		if(basicWin== null) {
			log.error("Must define basic window size");
		}
		basicWindowSize = Integer.parseInt(basicWin);
		
		setupRServe();
		setupREnvironment();
	}
	
	public void setupRServe() {
		
		try {
			config.load(new FileInputStream("conf/statstream.properties"));
		} catch (IOException e1) {
			log.error("Error loading config file\n");
			e1.printStackTrace();
			return;
		}
		
		// start up Rserve service if not running 
		log.info("result="+StatStreamUtil.checkLocalRserve());
		try {
			String host = config.getProperty("HOST");
			conn = new RConnection(host);
		} catch (Exception e) {
			log.error("Error creating new RConnection on localhost\n" );
			e.printStackTrace();
		};
	}
	
	public void setupREnvironment() {
		final String funcRFile = config.getProperty("R_SVC_SCRIPT");
		
		try {
			// source the main R file to initialize global variables referenced by R functions
			log.info("try to run R cmd: source('"+funcRFile+"')");
			conn.parseAndEval("source('"+funcRFile+"')");
			
			// check that all global variables exists 
			REXP cmd_ls = conn.parseAndEval("ls()");
			log.info("cmd_ls(debug): "+cmd_ls.toDebugString());
			
		} catch (Exception e) {
			log.error("Failed in Rserver call");
			e.printStackTrace();
			// think about recover here, i.e. Re-intialize R session and try again.
			// recover is important as all important data are stored in R.
		}
	}

	
	public abstract boolean tick(Map<String, List<LatestMarketData>> aTick, int bwNum);
		
}
