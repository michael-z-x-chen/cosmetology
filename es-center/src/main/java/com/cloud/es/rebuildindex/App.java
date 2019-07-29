package com.cloud.es.rebuildindex;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

/**
 * elasticsearch 不重启重建索引
 * http://ju.outofmemory.cn/entry/314127
 * 
 * 
 * 
 * https://m635674608.iteye.com/blog/2257735
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	
    	/**
    	 curl -X POST http://localhost:9200/_aliases -d'
 {
        "actions": [
            {"add": {"index": "esblog", "alias": "blog"}}
          ]
    }
    	 * */
        
	     System.out.println( "elasticsearch 重建索引" );
	        
	   	 String aliasName = "blog"; //别名
	     String aliasNameOld = "blogOld"; //上一次的别名
	   	 String indexName = "prodblog"; //新索引名前缀
	   	 
	   	 String oldIndex = ""; //老索引
	   	 String preOldIndex = "";//备份索引
		   	 
	   	 Date nowTime = new Date();
	   	 SimpleDateFormat time=new SimpleDateFormat("yyyyMMddHHmmss"); 
	   	 //生成新索引
	   	 indexName += time.format(nowTime);//System.currentTimeMillis();
	   	
	   	 System.out.println("新索引名："+indexName);
	   	
	   	 //初始化客户端
         Client client = null;
	        
			try {
				
				Settings settings = Settings.settingsBuilder()
				        .put("cluster.name", "elasticsearch")
				        .put("client.transport.ping_timeout", "120s")
				        .put("client.transport.sniff", true ).build();
				
				client = TransportClient.builder().settings(settings).build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("127.0.0.1"), 9300));
				
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				System.out.print("client error:"+e.getMessage());
			}
		
		   //获取备份的索引	
		   SearchResponse oldres = null;
			try {
				oldres = client
					        .prepareSearch(aliasNameOld)
					        .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)//.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					        .setFrom(0).setSize(1)
					        .execute()
					        .actionGet();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("无此别名："+e.getMessage());
				
			}	

			if(oldres !=null)
			{
				   SearchHit hitOld  =  oldres.getHits().getHits()[0];
				   
				   preOldIndex = hitOld.getIndex();
				   
				   System.out.println("preoldIndex:"+preOldIndex);
			}
		
			//获取mappings
			ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>>  mappingRes = client.admin().indices().prepareGetMappings(aliasName).get().getMappings(); 
			ImmutableOpenMap<String, MappingMetaData> mappings = null;
		   for( ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> cursor : mappingRes)
		   {
			    mappings = cursor.value;
			   	System.out.println("now index:"+cursor.key);
		   }
		   
		   //创建新索引
		   //client.admin().indices().prepareCreate(indexName).addMapping(typeName, indexMapping).get();
		   CreateIndexRequestBuilder create =  client.admin().indices().prepareCreate(indexName);
		   
		   for( ObjectObjectCursor<String, MappingMetaData> cursor :mappings)
		   {
			   System.out.println("typeName:"+cursor.key);
			   try {
					create.addMapping(cursor.key, cursor.value.getSourceAsMap());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("createType for mapping error:"+e.getMessage());
				}
		   }
		   create.get();
		   
		   //重建索引
		   SearchResponse scrollResp = client
			        .prepareSearch(aliasName)
			        .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)//.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setScroll(new TimeValue(60000))//.setFrom(0).setSize(10).setExplain(true)
			        .setSize(300)
			        .execute()
			        .actionGet();
		   
		    int num = 0;
		   
		 	while(true)
		 	{
		 		
		     	// 批量处理request  
	 	        BulkRequestBuilder bulkRequest = client.prepareBulk();  
	 	        
		 		for(SearchHit hit : scrollResp.getHits().getHits())
		 		{
		 			if("".equals(oldIndex))
		 			{
		 				oldIndex = hit.getIndex();
		 			}
		 			 
		 			bulkRequest.add(new IndexRequest(indexName,hit.getType(),hit.getId()).source(hit.getSource()));
		 		}
		 	    
		 		// 执行批量处理request  
		        BulkResponse bulkResponse = bulkRequest.get(); 
		        
		        num += 300;
		        
		        System.out.println("已处理 "+ num +"条");
		        
		       // 处理错误信息  
		        if (bulkResponse.hasFailures()) {  
		            System.out.println("====================批量创建索引过程中出现错误 下面是错误信息==========================");  
		            long count = 0L;  
		            for (BulkItemResponse bulkItemResponse : bulkResponse.getItems()) {  
		                System.out.println("发生错误的 索引id为 : "+bulkItemResponse.getId()+" ，错误信息为："+ bulkItemResponse.getFailureMessage());  
		                count++;  
		            }  
		            System.out.println("====================批量创建索引过程中出现错误 上面是错误信息 共有: "+count+" 条记录==========================");  
		        }  
		 		
		 		scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
		 	   
		 		//Break condition: No hits are returned
		 	    if (scrollResp.getHits().getHits().length == 0) {
		 	    	
		 	    	System.out.println("remove oldIndex:"+oldIndex  +" deletepreoldIndex:"+preOldIndex);
		 	    	
		 	    	 //别名指向新的索引,旧索引从别名上移出
		 	    	client.admin().indices().prepareAliases().addAlias(indexName, aliasName).removeAlias(oldIndex, aliasName).get();
		 	    	
		 	    	if("".equals(preOldIndex)) //第一次没有备份索引
		 	    	{
		 	    		client.admin().indices().prepareAliases().addAlias(oldIndex, aliasNameOld).get();
		 	    		
		 	    	}else{
		 	    		//备份别名指向上次一的索引，老索引删除
		 	    		client.admin().indices().prepareAliases().addAlias(oldIndex, aliasNameOld).removeAlias(preOldIndex, aliasNameOld).get();
		 	    		//删除以前备份的索引
		 	    		DeleteIndexResponse dResponse =  client.admin().indices().prepareDelete(preOldIndex).execute().actionGet();
		 	    	}
		 	    	
		 	        break;
		 	    }
		 	}
		 	
			// on shutdown
			client.close();
    }
    
    // 第三步。每天检测词库是否有更新。有则重建索引
}