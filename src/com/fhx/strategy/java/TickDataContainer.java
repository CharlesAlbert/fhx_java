package com.fhx.strategy.java;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.marketcetera.marketdata.interactivebrokers.LatestMarketData;

import com.fhx.statstream.StatStreamRealtimeService;

/*
 * Singleton class that hold one sliding window worth of data across all symbols
 * When the container is full, the data is served to the StatStreamProto
 */
public enum TickDataContainer {
	INSTANCE;
	
	private static Logger log = Logger.getLogger(TickDataContainer.class);
	
	private int basicWindowCnt = 1;
	private int basicWindowSize = -1;
	private final Properties config = new Properties();
	private static StatStreamRealtimeService ssService = new StatStreamRealtimeService();

	/*
	 * Use TreeMap to guarantee ordering
	 * Example:
	 * 	IBM -> [LatestMarketData1, LatestMarketData2, LatestMarketData3, ... ] 
	 */
	private Map<String, List<LatestMarketData>> basicWindowTicks = new TreeMap<String, List<LatestMarketData>>();
	
	public void init() {
		try {
			ssService.init();
			
			PropertyConfigurator.configure("conf/log4j.properties");		
		
			config.load(new FileInputStream("conf/statstream.properties"));
			
			String basicWin = config.getProperty("BASIC_WINDOW_SIZE");
			if(basicWin== null) {
				log.error("Must define basic window size");
			}
			basicWindowSize = Integer.parseInt(basicWin);
			
		} catch (Exception e1) {
			System.out.format("ERROR HERE\n");
			log.error("Error loading config file\n");
			e1.printStackTrace();
			return;
		}	
	}
	
	public void flushBasicWindow() {
		log.info("Serving basic window # " + basicWindowCnt + " to the StatStream model" );
		
		/*
		 * format basic window data and serve it to StatStream model
		 */
		ssService.tick(basicWindowTicks, basicWindowCnt++);
		
		log.info("Flushing basic window " + basicWindowCnt + ", invoking StatStreamService");
		log.info("Size of basic window = "+ basicWindowTicks.values().iterator().next().size());
		basicWindowTicks.clear();
	}
	
	public void addATick(Map<String, LatestMarketData> aTick) {
		String symbol = "";
		LatestMarketData data;
		
		for(Map.Entry<String, LatestMarketData> tick : aTick.entrySet()) {
			symbol = tick.getKey();
			data = tick.getValue();
			
			List<LatestMarketData> ticksPerSymbol = basicWindowTicks.get(symbol);
			if(ticksPerSymbol==null) {
				log.info("initializing arraylist for symbol "+symbol);
				ticksPerSymbol = new ArrayList<LatestMarketData>();
				ticksPerSymbol.add(data);
				basicWindowTicks.put(symbol, ticksPerSymbol);
			}
			else {
				ticksPerSymbol.add(data);
			}
		}
		
		if(basicWindowTicks.get(symbol).size() >= basicWindowSize)			
			flushBasicWindow();
	}
	
	public Map<String, List<LatestMarketData>> getBasicWindowTicks() {
		// client should not be able to get access to modify the data
		return Collections.unmodifiableMap(basicWindowTicks);
	}

}
