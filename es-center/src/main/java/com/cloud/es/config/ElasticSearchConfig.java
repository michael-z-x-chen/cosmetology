package com.cloud.es.config;


import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.env.Environment;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.net.InetAddress;
import java.util.HashMap;

/**
 */
@Configuration
@ConfigurationProperties("spring.data.elasticsearch")
public class ElasticSearchConfig {

    private String clusterName;
    private String clusterNodes;
    private String pathHome;
    private String poolSize = "5";

    @Bean("client")
    public TransportClient getClient() throws Exception {
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName).put("client.transport.sniff", true)
                .put("thread_pool.search.size", poolSize)
                .build();//启动嗅探功能
        TransportClient client = new PreBuiltTransportClient(settings);

        for (String node : clusterNodes.split(",")) {
            String host = node.split(":")[0];
            String port = node.split(":")[1];
            TransportAddress transportAddress = new InetSocketTransportAddress(InetAddress.getByName(host), Integer.parseInt(port));
            client.addTransportAddress(transportAddress);
        }

        return client;
    }

//    @Bean("configuration")
//    public org.wltea.analyzer.cfg.Configuration getConfiguration() {
//        String os = System.getProperty("os.name");
//        if (StringUtils.isNotBlank(os) && os.toLowerCase().startsWith("win")) {
//            pathHome = "c:" + pathHome;
//        }
//        try {
//            File file = new File(pathHome);
//            if (!file.exists()) {
//                throw new RuntimeException("请联系管理员配置es文件：" + pathHome);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("请联系管理员,配置es文件：" + pathHome);
//        }
//
//        HashMap<String, Object> settings_map = new HashMap<String, Object>(2);//设置setting  
//        settings_map.put("path.home", pathHome);
//        Settings settings = Settings.builder().put(settings_map).build();
//        Environment env = new Environment(settings);
//        return new org.wltea.analyzer.cfg.Configuration(env, settings);
//    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterNodes() {
        return clusterNodes;
    }

    public void setClusterNodes(String clusterNodes) {
        this.clusterNodes = clusterNodes;
    }

    public String getPathHome() {
        return pathHome;
    }

    public void setPathHome(String pathHome) {
        this.pathHome = pathHome;
    }
}
