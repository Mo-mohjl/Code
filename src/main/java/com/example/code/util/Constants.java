package com.example.code.util;

public class Constants {
    public enum Language{
        JAVA(1,"java"),
        Cpp(2,"cpp"),
        Python(3,"python"),
        Others(4,"Others");
        private Integer code;
        private String info;
        Language(Integer code,String info){
            this.code=code;
            this.info=info;
        }

        public Integer getCode() {
            return code;
        }

        public String getInfo() {
            return info;
        }
    }
}
