package com.cloud.es.hostreload;


import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.wltea.analyzer.dic.Dictionary;

public class HotDictReloadThread implements Runnable {

	private static final Logger logger = ESLoggerFactory.getLogger(HotDictReloadThread.class.getName());
   
	private static boolean isStart = true;
	@Override
	public void run() {
		while(true) {
			logger.info("[==========]reload hot dict from mysql......");   
			Dictionary tmpDict = new HtDictionary();//这里需要改成单例
			tmpDict.reLoadMainDict(true);
			isStart = false;
			
		}
	}
  
}
