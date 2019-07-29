package com.evergrande.esserver.config;

import com.evergrande.esserver.facade.EsFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.lucene.IKAnalyzer;

@Component
public class EsDataInitInit implements CommandLineRunner {

    @Autowired
    private EsFacade esFacade;

    @Autowired
    private Configuration configuration;

    @Override
    public void run(String... args) {
        IKAnalyzer ik = new IKAnalyzer(configuration);
        //加载分词器
        Dictionary.getSingleton().reLoadMainDict(true);
        esFacade.initIndexCollectSearch();
    }
}