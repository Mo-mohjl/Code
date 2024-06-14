package com.example.code.controller;

import com.alibaba.fastjson.JSON;
import com.example.code.po.Dai;
import com.example.code.service.IDaiService;
import com.example.code.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class DaiController {
    private static Logger logger= LoggerFactory.getLogger(DaiController.class);
    @Resource
    private IDaiService daiService;
    @GetMapping("/user/{id}")
    public ResponseEntity<Map<String,String>> getCode(@PathVariable Integer id){
        Map<String,String> map=new HashMap<>();
        Dai dai = daiService.getCode(id);
        String code = dai.getCode();
        Integer language = dai.getLanguage();
        String type=getbylanguage(language);
        map.put("code", code);
        map.put("language", type);
        logger.info(JSON.toJSONString(map));
        return ResponseEntity.ok().body(map);
    }
    @PostMapping("/run")
    public ResponseEntity<Map<String,String>> run(@RequestBody Map<String,Object> request){
        Map<String,String> map=new HashMap<>();
        String result = daiService.updateCodeAndrun(request);
        map.put("output",result);
        logger.info(JSON.toJSONString(map));
        return ResponseEntity.ok().body(map);
    }
    public String getbylanguage(Integer language){
        switch (language){
            case 1:
                return Constants.Language.JAVA.getInfo();
            case 2:
                return Constants.Language.Cpp.getInfo();
            case 3:
                return Constants.Language.Python.getInfo();
            default:
                throw new RuntimeException("还未开启该语言使用");
        }
    }

}
