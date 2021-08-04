package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.model.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.Vo.AttrResponseVo;
import com.atguigu.gulimall.search.Vo.SearchParam;
import com.atguigu.gulimall.search.Vo.SearchResult;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.fegin.ProductFeignService;
import com.atguigu.gulimall.search.service.SearchService;
import com.fasterxml.jackson.databind.type.ReferenceType;
import jdk.nashorn.internal.runtime.linker.LinkerCallSite;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryparser.flexible.core.builders.QueryBuilder;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public SearchResult getSearchResult(SearchParam searchParam) {
        SearchResult searchResult=null;
        SearchRequest request =bulidSearchRequest(searchParam);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(request, GulimallElasticSearchConfig.COMMON_OPTIONS);
            // 将es响应数据封装成结果
            searchResult = bulidSearchResult(searchParam,searchResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return searchResult;
    }

    private SearchResult bulidSearchResult(SearchParam searchParam, SearchResponse searchResponse) {
        SearchResult searchResult = new SearchResult();
        SearchHits hits = searchResponse.getHits();
        //1. 封装查询到的商品信息
        if(hits.getHits()!=null && hits.getHits().length>0){
            List<SkuEsModel> list = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(searchParam.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(string);
                }
                list.add(skuEsModel);
            }
            searchResult.setProducts(list);
        }


        //2. 封装分页信息
        //2.1 当前页码
        searchResult.setPageNum(searchParam.getPageNum());
        //2.2 总记录数
        long total = hits.getTotalHits().value;
        searchResult.setTotal(total);
        //2.3 总页码
        int pageNum=(int)total%EsConstant.PRODUCT_PAGESIZE==0 ?(int)total/EsConstant.PRODUCT_PAGESIZE:(int)total/EsConstant.PRODUCT_PAGESIZE+1;
        searchResult.setPageNum(pageNum);
        //3. 查询结果涉及到的品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        Aggregations aggregations = searchResponse.getAggregations();
        //ParsedLongTerms用于接收terms聚合的结果，并且可以把key转化为Long类型的数据
        ParsedLongTerms brandAgg = aggregations.get("brandAgg");

        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            //3.1 得到品牌id
            Long brandId = bucket.getKeyAsNumber().longValue();
            Aggregations subAgg = bucket.getAggregations();


            //3.2 得到品牌图片
            ParsedStringTerms brandImgAgg = subAgg.get("brandImgAgg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            //3.3 得到品牌名字
            ParsedStringTerms brandNameAgg = subAgg.get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo(brandId,brandImg,brandName);
            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);

        //4. 查询涉及到的所有分类
        List<SearchResult.CatalogVo> catelogVos = new ArrayList<>();
        ParsedLongTerms catelogAgg = aggregations.get("catelogAgg");

        for (Terms.Bucket bucket : catelogAgg.getBuckets()) {
            //4.1 获取分类id
            Long catelogId = bucket.getKeyAsNumber().longValue();
            //4.2 获取分类名
            Aggregations subCatelog = bucket.getAggregations();
            ParsedStringTerms  parsedStringTerms = subCatelog.get("catelogName");
            String catelogName = parsedStringTerms.getBuckets().get(0).getKeyAsString();
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo(catelogId,catelogName);
            catelogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catelogVos);


        //5 查询涉及到的所有属性
        ArrayList<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested parsedNested = aggregations.get("attrs");
        //ParsedNested用于接收内置属性的聚合
        ParsedLongTerms  attrAgg = parsedNested.getAggregations().get("attrAgg");
        for (Terms.Bucket bucket : attrAgg.getBuckets()) {
            //5.1 查询属性id
            Long attrId = bucket.getKeyAsNumber().longValue();
            //5.2 查询属性名
            Aggregations subAttr = bucket.getAggregations();
            ParsedStringTerms  attrNameAgg = subAttr.get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();

            //5.3 查询属性值
            ParsedStringTerms attrValueAgg = subAttr.get("attrValueAgg");
            ArrayList<String> attrValues  = new ArrayList<>();
            for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
                String attrValue = attrValueAggBucket.getKeyAsString();
                attrValues.add(attrValue);
            }
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo(attrId,attrName,attrValues);
        }
        searchResult.setAttrs(attrVos);

        // 6. 构建面包屑导航
        List<String> attrs = searchParam.getAttrs();
        if(attrs.size()>0 && attrs!=null){
            List<SearchResult.NavVo> collect = attrs.stream().map(attr -> {
                String[] split = attr.split("_");
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //6.1 设置属性值
                navVo.setNavValue(split[1]);
                //6.2 查询并设置属性名
                try {
                    R r = productFeignService.info(Long.parseLong(split[0]));
                    if (r.getCode() == 0) {
                        AttrResponseVo attrResponseVo = JSON.parseObject(JSON.toJSONString(r.get("attr")), AttrResponseVo.class);
                        navVo.setName(attrResponseVo.getAttrName());
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    log.error("远程调用服务失败");
                }
                //6.3 设置面包屑跳转链接
                String queryString = searchParam.get_queryString();
                String replace = queryString.replace("&attrs=" + attr, "").replace("attrs=" + attr + "&", "").replace("attrs=" + attr, "");
                navVo.setLink("http://search.gulimall.com/search.html" + (replace.isEmpty() ? "" : "?" + replace));
                return navVo;
            }).collect(Collectors.toList());
            searchResult.setNavs(collect);
        }

        return searchResult;
    }

    private SearchRequest bulidSearchRequest(SearchParam searchParam) {

        // 用于构建DSL语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //1. 构建bool query
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //1.1 bool must
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTiltle",searchParam.getKeyword()));
        }

        //1.2 bool filter

        //1.2.1 catalog
        if(searchParam.getCatalog3Id()!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("catelogId",searchParam.getCatalog3Id()));
        }
        //1.2.2 brand
        if(searchParam.getBrandId()!=null&& searchParam.getBrandId().size()>0){
            boolQueryBuilder.filter(QueryBuilders.termQuery("brandId",searchParam.getBrandId()));
        }
        //1.2.3 hasStock
        if(searchParam.getHasStock()!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock",searchParam.getHasStock()==1));
        }
        //1.2.4 priceRange
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
        if(!StringUtils.isEmpty(searchParam.getSkuPrice())){
            String[] price = searchParam.getSkuPrice().split("_");
            if(price.length==1){
                if(searchParam.getSkuPrice().startsWith("_")){
                    rangeQueryBuilder.lte(Integer.parseInt(price[0]));
                }else {
                    rangeQueryBuilder.gte(Integer.parseInt(price[0]));
                }
            }else if(price.length==2){
                if(price!=null){
                    rangeQueryBuilder.gte(Integer.parseInt(price[0]));
                    rangeQueryBuilder.lte(Integer.parseInt(price[1]));
                }
            }
        boolQueryBuilder.filter(rangeQueryBuilder);
        }
        //1.2.5 attrs-nested
        //attrs=1_5寸:8寸&2_16G:8G
        List<String> attrs = searchParam.getAttrs();
        BoolQueryBuilder queryBuilder  = new BoolQueryBuilder();
        if(attrs.size()>0&& attrs!=null){
            attrs.forEach(attr->{
                String[] split = attr.split("_");
                queryBuilder.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                queryBuilder.must(QueryBuilders.termQuery("attrs.value",split[1].split(":")));
            });
        }
        NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", queryBuilder, ScoreMode.None);
        boolQueryBuilder.filter(nestedQueryBuilder);
        //1.X bool query构建完成
        searchSourceBuilder.query(boolQueryBuilder);

        //2. sort  eg:sort=saleCount_desc/asc
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String[] sortSplit= searchParam.getSort().split("_");
            searchSourceBuilder.sort(sortSplit[0],
                    sortSplit[1].equalsIgnoreCase("asc")? SortOrder.ASC : SortOrder.DESC);
        }


        //3. 分页 // 是检测结果分页
        searchSourceBuilder.from((searchParam.getPageNum()-1)* EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //4. 高亮highlight
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuId");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        //5. 聚合
        //5.1 按照brand聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg").field("brandId");
        TermsAggregationBuilder brandName = AggregationBuilders.terms("brandNameAgg").field("brandName");
        TermsAggregationBuilder brandImg = AggregationBuilders.terms("brandImgAgg").field("brandImg");
        brandAgg.subAggregation(brandImg);
        brandAgg.subAggregation(brandName);
        searchSourceBuilder.aggregation(brandAgg);

        //5.2 按照catalog聚合
        TermsAggregationBuilder catelogAgg = AggregationBuilders.terms("catelogAgg").field("catelogId");

        // 子聚合
        TermsAggregationBuilder catelogName = AggregationBuilders.terms("catelogNameAgg").field("catelogName");
        catelogAgg.subAggregation(catelogName);
        searchSourceBuilder.aggregation(catelogAgg);


        //5.3 按照attrs聚合
        NestedAggregationBuilder nestedAggregationBuilder = new NestedAggregationBuilder("attrs", "attrs");
        //按照attrId聚合     //按照attrId聚合之后再按照attrName和attrValue聚合
        TermsAggregationBuilder attrAgg = AggregationBuilders.terms("attrAgg").field("attrId");
        TermsAggregationBuilder attrNameAgg = AggregationBuilders.terms("attrNameAgg").field("attrName");
        TermsAggregationBuilder attrValueAgg = AggregationBuilders.terms("attrValueAgg").field("attrValue");
        attrAgg.subAggregation(attrNameAgg);
        attrAgg.subAggregation(attrValueAgg);
        nestedAggregationBuilder.subAggregation(attrAgg);
        searchSourceBuilder.aggregation(nestedAggregationBuilder);
        log.debug("构建的DSL语句{}",searchSourceBuilder.toString());
        SearchRequest request = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return request;
    }
}
