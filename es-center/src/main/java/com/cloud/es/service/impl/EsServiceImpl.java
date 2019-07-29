package com.evergrande.esserver.service.impl;

import com.evergrande.api.exception.HtServerException;
import com.evergrande.api.page.PageInfo;
import com.evergrande.api.page.PageQuery;
import com.evergrande.core.enums.IsDeleteEnum;
import com.evergrande.core.thread.ThreadContext;
import com.evergrande.core.thread.param.UserInfo;
import com.evergrande.escore.enums.BusinessSys;
import com.evergrande.escore.enums.DBOperation;
import com.evergrande.esserver.enums.ContentStateEnum;
import com.evergrande.esserver.enums.IndexStateEnum;
import com.evergrande.esserver.mapper.CollectContentHisMapper;
import com.evergrande.esserver.mapper.CollectContentMapper;
import com.evergrande.esserver.mapper.IndexMapper;
import com.evergrande.esserver.mapper.KeyWordMapper;
import com.evergrande.esserver.mapper.StopKeyWordMapper;
import com.evergrande.esserver.model.CollectContentHisModel;
import com.evergrande.esserver.model.CollectContentModel;
import com.evergrande.esserver.model.IndexModel;
import com.evergrande.esserver.model.KeyWordModel;
import com.evergrande.esserver.model.StopKeyWordModel;
import com.evergrande.esserver.service.EsService;
import com.evergrande.esserver.util.ElasticsearchUtils;
import com.evergrande.esserver.util.IndexKeyUtil;
import com.evergrande.util.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service("esService")
public class EsServiceImpl implements EsService {

    @Autowired
    private KeyWordMapper keyWordMapper;
    @Autowired
    private StopKeyWordMapper stopKeyWordMapper;
    
    @Autowired
    private TransportClient client;
    
    @Autowired
    private Configuration configuration;

    @Override
    public Set<String> analysisKeyWords(Set<String> contents) throws Exception {
        IKAnalyzer ik = new IKAnalyzer(configuration);

        Set<String> setTmp = new HashSet<String>();

        for (String text : contents) {
            StringReader stringReader = new StringReader(text);
            TokenStream tokenStream = ik.tokenStream("", stringReader);
            tokenStream.reset();

            CharTermAttribute term = tokenStream.getAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                setTmp.add(term.toString());
            }
            stringReader.close();
            tokenStream.close();
        }
        return setTmp;
    }

    @Override
    public Set<String> checkoutKeyWordsExis(Set<String> keyWords) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void collectKeyWords(List<KeyWordModel> list) {
        this.keyWordMapper.saveList(list);
    }

    @Override
    public void collectContentEs(String indexKey, String primaryKey, String contentJson, DBOperation dBOperation) {
        int code = dBOperation.getCode();
        DocWriteRequest.OpType opType = DocWriteRequest.OpType.fromString(code + "");
        client.prepareIndex(indexKey, IndexKeyUtil.Keyword).setOpType(opType).setId(primaryKey).setSource(contentJson, XContentType.JSON).get();
    }

    public Long dealAcceptContent(long timeStamp, BusinessSys businessSys, String spaceName, String primaryKey, String contentJson, DBOperation dBOperation) {
        CollectContentModel collectContentModel = new CollectContentModel();
        collectContentModel.setSpaceName(spaceName);
        collectContentModel.setContentState(ContentStateEnum.Not_In_Mq.getMsgCode());
        collectContentModel.setPrimaryKey(primaryKey);
        collectContentModel.setContentJson(contentJson);
        collectContentModel.setIsDelete(IsDeleteEnum.no.getId());
        collectContentModel.setTimeStamp(timeStamp);
        collectContentModel.setBusinessSysCode(businessSys.getCode());
        collectContentModel.setDbUpdatedTime(new Date());
        collectContentModel.setIndexKey(getIndexCollectSearch());
        UserInfo userInfo = ThreadContext.getUserInfo();
        if (userInfo != null) {
            collectContentModel.setCreatedBy(userInfo.getUserId());
        }
        if (dBOperation == null || dBOperation == DBOperation.Index) {
            throw new HtServerException("入参dBOperation有误");
        }
        CollectContentHisModel collectContentHisModel = new CollectContentHisModel();
        if (dBOperation == DBOperation.Increase) {
            collectContentMapper.save(collectContentModel);
            BeanUtils.copyProperties(collectContentModel, collectContentHisModel);
        } else if (dBOperation == DBOperation.Delete) {
            CollectContentModel model = new CollectContentModel();
            model.setTimeStamp(timeStamp);
            model.setSpaceName(spaceName);
            model.setPrimaryKey(primaryKey);
            model.setBusinessSysCode(businessSys.getCode());
            CollectContentModel contentModel = collectContentMapper.queryDetail(model);
            if (contentModel != null) {
                CollectContentModel modelDel = new CollectContentModel();
                modelDel.setContentId(contentModel.getContentId());
                collectContentMapper.delete(modelDel);
                BeanUtils.copyProperties(contentModel, collectContentHisModel);
            }
        } else {
            CollectContentModel model = new CollectContentModel();
            model.setTimeStamp(timeStamp);
            model.setSpaceName(spaceName);
            model.setPrimaryKey(primaryKey);
            model.setBusinessSysCode(businessSys.getCode());
            CollectContentModel contentModel = collectContentMapper.queryDetail(model);
            if (contentModel != null) {
                collectContentModel.setContentId(contentModel.getContentId());
                if (userInfo != null) {
                    collectContentModel.setUpdatedBy(userInfo.getUserId());
                }
                collectContentMapper.update(collectContentModel);
                BeanUtils.copyProperties(collectContentModel, collectContentHisModel);
            }
        }
        collectContentHisModel.setDbOperation(dBOperation.getCode());
        collectContentHisMapper.save(collectContentHisModel);
        return collectContentHisModel.getContentId();
    }

    @Override
    public void updateContentState(Long contentId, ContentStateEnum contentState) {
        CollectContentModel collectContentModel = new CollectContentModel();
        collectContentModel.setContentId(contentId);
        collectContentModel.setContentState(contentState.getMsgCode());
        collectContentMapper.update(collectContentModel);
    }

    @Override
    public void updateIndexKey(Long contentId, String indexKey){
        CollectContentModel collectContentModel = new CollectContentModel();
        collectContentModel.setContentId(contentId);
        collectContentModel.setIndexKey(indexKey);
        collectContentMapper.update(collectContentModel);
    }

    
    @Autowired
    ElasticsearchUtils elasticsearchUtils;
    
    @Override
    public PageQuery search(BusinessSys businessSys, String spaceName, Set<String> keyWord, PageInfo pageInfo) {
        return elasticsearchUtils.queryListByPage(IndexKeyUtil.getIndexSearchKey(businessSys, spaceName, getIndexKeySearch()), IndexKeyUtil.Keyword,
        									pageInfo.getCurrPage(), pageInfo.getPageSize(), 
        									0l, 0l, 
        									null, null, 
        									false, null, 
        									null, keyWord.iterator().next());

    }

    @Override
    public List<KeyWordModel> queryList(KeyWordModel keyWordModel) {
        return this.keyWordMapper.queryList(keyWordModel);
    }

    @Override
    public List<StopKeyWordModel> queryList(StopKeyWordModel stopKeyWordModel) {
        // TODO Auto-generated method stub
        return this.stopKeyWordMapper.queryList(stopKeyWordModel);
    }

    @Override
    public int updateState(KeyWordModel keyWordModel) {
        return this.keyWordMapper.updateState(keyWordModel);
    }

    
    @Override
    public int updateState(StopKeyWordModel stopKeyWordModel) {
        return this.stopKeyWordMapper.updateState(stopKeyWordModel);
    }

    @Override
    public Boolean queryContentExist(CollectContentModel collectContentModel) {
        if (collectContentModel == null || collectContentModel.getTimeStamp() <= 0) {
            throw new HtServerException("入参有误:TimeStamp必填");
        }
        collectContentModel.setIsDelete(IsDeleteEnum.no.getId());
        Long num = collectContentMapper.queryCount(collectContentModel);
        if (num != null && num.longValue() > 0l) {
            return false;
        }
        return true;
    }

    @Override
    public List<Long> querySwitchIndeKeyData() {
        return collectContentMapper.querySwitchIndeKeyData(getIndexCollectSearch());
    }

    @Override
    public List<Long> queryMqErrorData() {
        return collectContentMapper.queryMqErrorData(getIndexCollectSearch());
    }

    @Override
    public CollectContentHisModel queryByContentId(Long contentId) {
        return collectContentHisMapper.queryByContentId(contentId);
    }

    @Override
    @Cacheable(value = "es_indexSearchKey", sync = true)
    public String getIndexKeySearch() {
        IndexModel indexModel = indexMapper.getIndexKeyByIndexState(IndexStateEnum.Effective.getMsgCode());
        if (indexModel == null || StringUtils.isBlank(indexModel.getTimeKey())) {
            log.warn("内部错误，无有效timeKey");
            return "";
        }
        return indexModel.getTimeKey();
    }

    @Override
    @CachePut(value = "es_indexSearchKey")
    public String switchIndexKeySearch(String timeKey) {
        IndexModel indexModel = indexMapper.getIndexKeyByTimeKey(timeKey, IndexStateEnum.Construction.getMsgCode());
        if (indexModel != null) {
            indexMapper.updateIndexStateByInvalid(indexModel.getIndexId());
            indexMapper.updateIndexStateByEffective(indexModel.getIndexId());
        } else {
            log.error("内部错误，timeKey:" + timeKey);
            throw new HtServerException("无构建任务，timeKey:" + timeKey);
        }
        return timeKey;
    }

    @Override
    @Cacheable(value = "es_indexCollectKey", sync = true)
    public String getIndexCollectSearch() {
        return initIndexCollectSearch();
    }

    @Override
    @CachePut(value = "es_indexCollectKey")
    public String initIndexCollectSearch() {
        String timeKey = DateUtil.convertDateToStr(new Date(), "yyyy.MM.dd");
        IndexModel indexModel = indexMapper.getIndexKeyByIndexState(IndexStateEnum.Construction.getMsgCode());
        if (indexModel != null) {
            if(timeKey.equals(indexModel.getTimeKey())){
                return timeKey;
            }else{
                indexMapper.updateIndexStateByStruInvalid();
            }
        }
        IndexModel model = new IndexModel();
        model.setIndexState(IndexStateEnum.Construction.getMsgCode());
        UserInfo userInfo = ThreadContext.getUserInfo();
        if (userInfo != null) {
            model.setCreatedBy(userInfo.getUserId());
        }
        model.setTimeKey(timeKey);
        indexMapper.save(model);
        return timeKey;
    }

    @Override
    public Long switchIndexKeyCount(String indexKey){
        return collectContentMapper.switchIndexKeyCount(indexKey);
    }

    @Override
    public Long sumIndexKeyCount(String indexKey){
        return collectContentMapper.switchIndexKeyCount(indexKey);
    }

	@Override
	public void collectContentEsTest(String index, String type, String id, String json) {
		  client.prepareIndex(index, IndexKeyUtil.Keyword).setOpType(DocWriteRequest.OpType.CREATE).setId(id).setSource(json, XContentType.JSON).get();
	}
}
