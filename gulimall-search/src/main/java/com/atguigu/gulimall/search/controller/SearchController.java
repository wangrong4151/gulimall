package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.Vo.SearchParam;
import com.atguigu.gulimall.search.Vo.SearchResult;
import com.atguigu.gulimall.search.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 自动将页面提交过来的所有请求参数封装成我们指定的对象
     * @param param
     * @return
     */
    @RequestMapping(value = "/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request) {

        param.set_queryString(request.getQueryString());
        log.info("原生所有查询属性 {}",request.getQueryString());
        //1、根据传递来的页面的查询参数，去es中检索商品
        SearchResult result = searchService.getSearchResult(param);

        model.addAttribute("result",result);
        model.addAttribute("param",param);

        return "list";
    }

}