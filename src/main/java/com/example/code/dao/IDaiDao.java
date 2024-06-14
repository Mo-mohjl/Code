package com.example.code.dao;

import com.example.code.po.Dai;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IDaiDao {
    //根据用户ID查询缓存代码
    Dai selectById(Integer id);
    //修改语言或代码
    void updateById(Dai dai);
}
