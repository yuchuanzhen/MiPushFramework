package top.trumeet.mipushframework.xposed.model;

import com.alibaba.fastjson.JSON;

public class SimpleAppInfo {

    public boolean skipEnhance = false;


    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }



}
