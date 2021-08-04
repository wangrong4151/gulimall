package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.Vo.SearchParam;
import com.atguigu.gulimall.search.Vo.SearchResult;
import com.atguigu.gulimall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class SearchController {
    @Autowired
    private SearchService searchService;

    @GetMapping(value = {"/search.html"})
    public String getSearchPage(SearchParam searchParam, // 检索参数，
                                Model model, HttpServletRequest request) {
        searchParam.set_queryString(request.getQueryString());//_queryString是个字段
        SearchResult result=searchService.getSearchResult(searchParam);
        model.addAttribute("result", result);
        return "search";
    }

}
