package com.example.code.service;

import com.example.code.po.Dai;

import java.util.Map;

public interface IDaiService {
    //获取数据库上次运行代码
    Dai getCode(Integer id);
    //更改数据库代码缓存
    String updateCodeAndrun(Map<String,Object> request);
}
