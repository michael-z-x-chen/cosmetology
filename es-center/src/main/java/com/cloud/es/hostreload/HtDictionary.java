package com.cloud.es.hostreload;


import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.io.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.DictSegment;
import org.wltea.analyzer.dic.Dictionary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class HtDictionary extends Dictionary {

    @Autowired
    private Configuration configuration;
    @Autowired
    private EsService esService;

    public void reLoadMainDict(boolean isStart) {
        // 新开一个实例加载词典，减少加载过程对当前词典使用的影响
        Dictionary tmpDict = new HtDictionary(configuration, esService);

        tmpDict.configuration = Dictionary.getSingleton().configuration;
        tmpDict.loadMainDict(true);
        tmpDict.loadStopWordDict(true);
        _MainDict = tmpDict._MainDict;
        _StopWords = tmpDict._StopWords;
    }

    /**
     * 加载主词典及扩展词典
     */
    protected HtDictionary() {
    }

    protected HtDictionary(Configuration cfg) {
        super(cfg);
        // TODO Auto-generated constructor stub
    }

    protected HtDictionary(Configuration cfg, EsService esService) {
        super(cfg);
        // TODO Auto-generated constructor stub
        this.esService = esService;
    }

    /**
     * 加载主词典及扩展词典
     */
    public void loadMainDict(boolean isStart) {
        // 建立一个主词典实例
        _MainDict = new DictSegment((char) 0);

        // 读取主词典文件
        Path file = PathUtils.get(getDictRoot(), Dictionary.PATH_DIC_MAIN);

        InputStream is = null;
        try {
            is = new FileInputStream(file.toFile());
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
            String theWord = null;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    _MainDict.fillSegment(theWord.trim().toCharArray());
                }
            } while (theWord != null);
        } catch (IOException e) {
            log.error("ik-analyzer",  e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                log.error("ik-analyzer", e);
            }
        }
        // 加载扩展词典
        this.loadExtDict();
        // 加载远程自定义词库
        this.loadRemoteExtDict();

        // 从mysql加载词典
        this.loadMySQLExtDict(isStart);
    }

    /**
     * 从mysql加载热更新词典
     */
    private void loadMySQLExtDict(boolean isStart) {

        try {
            KeyWordModel keyWordModel = new KeyWordModel();
            if (isStart) {
                keyWordModel.setKeyState(0);
            }

            List<KeyWordModel> queryList = this.esService.queryList(keyWordModel);
            for (KeyWordModel keyWordModel2 : queryList) {
                _MainDict.fillSegment(keyWordModel2.getKeyWord().trim().toCharArray());
            }

            keyWordModel.setKeyState(0);
            this.esService.updateState(keyWordModel);

            //Thread.sleep(5000);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 加载用户扩展的停止词词典
     */
    public void loadStopWordDict(boolean isStart) {
        // 建立主词典实例
        _StopWords = new DictSegment((char) 0);

        // 读取主词典文件
        Path file = PathUtils.get(getDictRoot(), Dictionary.PATH_DIC_STOP);

        InputStream is = null;
        try {
            is = new FileInputStream(file.toFile());
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
            String theWord = null;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    _StopWords.fillSegment(theWord.trim().toCharArray());
                }
            } while (theWord != null);
        } catch (IOException e) {
            log.error("ik-analyzer", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                log.error("ik-analyzer", e);
            }
        }

        // 加载扩展停止词典
        List<String> extStopWordDictFiles = getExtStopWordDictionarys();
        if (extStopWordDictFiles != null) {
            is = null;
            for (String extStopWordDictName : extStopWordDictFiles) {
                log.info("[Dict Loading] " + extStopWordDictName);

                // 读取扩展词典文件
                file = PathUtils.get(getDictRoot(), extStopWordDictName);
                try {
                    is = new FileInputStream(file.toFile());
                } catch (FileNotFoundException e) {
                    log.error("ik-analyzer", e);
                }
                // 如果找不到扩展的字典，则忽略
                if (is == null) {
                    continue;
                }
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
                    String theWord = null;
                    do {
                        theWord = br.readLine();
                        if (theWord != null && !"".equals(theWord.trim())) {
                            // 加载扩展停止词典数据到内存中
                            _StopWords.fillSegment(theWord.trim().toCharArray());
                        }
                    } while (theWord != null);
                } catch (IOException e) {
                    log.error("ik-analyzer", e);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                            is = null;
                        }
                    } catch (IOException e) {
                        log.error("ik-analyzer", e);
                    }
                }
            }
        }

        // 加载远程停用词典
        List<String> remoteExtStopWordDictFiles = getRemoteExtStopWordDictionarys();
        for (String location : remoteExtStopWordDictFiles) {
            log.info("[Dict Loading] " + location);
            List<String> lists = getRemoteWords(location);
            // 如果找不到扩展的字典，则忽略
            if (lists == null) {
                log.error("[Dict Loading] " + location + "加载失败");
                continue;
            }
            for (String theWord : lists) {
                if (theWord != null && !"".equals(theWord.trim())) {
                    // 加载远程词典数据到主内存中
                    log.info(theWord);
                    _StopWords.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            }
        }

        this.loadMySQLStopwordDict(isStart);
    }

    /**
     * 从mysql加载停用词
     */
    private void loadMySQLStopwordDict(boolean isStart) {
        // 加载远程停用词典

        try {
            StopKeyWordModel stopKeyWordModel = new StopKeyWordModel();
            if (isStart) {
                stopKeyWordModel.setKeyState(0);
            }
            List<StopKeyWordModel> queryList = this.esService.queryList(stopKeyWordModel);

            for (StopKeyWordModel stopKeyWordModel2 : queryList) {
                _StopWords.fillSegment(stopKeyWordModel2.getKeyWord().trim().toCharArray());
            }

            stopKeyWordModel.setKeyState(0);
            this.esService.updateState(stopKeyWordModel);
            //Thread.sleep(5000);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
