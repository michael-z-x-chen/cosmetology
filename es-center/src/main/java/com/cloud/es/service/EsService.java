package com.cloud.es.service;


import java.util.List;
import java.util.Set;

public interface EsService {

    Set<String> analysisKeyWords(Set<String> contents) throws Exception;

    Set<String> checkoutKeyWordsExis(Set<String> keyWords);

    void collectKeyWords(List<KeyWordModel> list);

    
    void collectContentEs(String indexKey, String primaryKy, String contentJson, DBOperation dBOperation);
    Long dealAcceptContent(long timeStamp, BusinessSys businessSys, String spaceName, String primaryKey, String contentJson, DBOperation dBOperation);

    void updateContentState(Long contentId, ContentStateEnum contentState);

    void updateIndexKey(Long contentId, String indexKey);

    PageQuery search(BusinessSys businessSys, String spaceName, Set<String> keyWord, PageInfo pageInfo);

    List<KeyWordModel> queryList(KeyWordModel keyWordModel);

    List<StopKeyWordModel> queryList(StopKeyWordModel stopKeyWordModel);

    int updateState(KeyWordModel keyWordModel);

    int updateState(StopKeyWordModel stopKeyWordModel);

    Boolean queryContentExist(CollectContentModel collectContentModel);

    List<Long> queryMqErrorData();

    List<Long> querySwitchIndeKeyData();

    CollectContentHisModel queryByContentId(Long contentId);

    String getIndexKeySearch();

    String switchIndexKeySearch(String timeKey);

    String getIndexCollectSearch();

    String initIndexCollectSearch();

    Long switchIndexKeyCount(String indexKey);

    Long sumIndexKeyCount(String indexKey);

	void collectContentEsTest(String index, String type, String id, String json);

}
