package com.cjk.common;

public enum WxEnum {
    WX_TOKEN("542397apptoken");

    private String code;

    private WxEnum(String code){
        this.code = code;
    };

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
