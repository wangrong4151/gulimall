package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.Vo.SearchParam;
import com.atguigu.gulimall.search.Vo.SearchResult;

public interface SearchService {
    SearchResult getSearchResult(SearchParam searchParam);
}
