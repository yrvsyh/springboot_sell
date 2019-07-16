package com.linghang.enums;

public enum ProductStatusEnum {

    UP(0, "在架"), DOWN(1, "下架");

    private Integer code;

    private String msg;

    ProductStatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
