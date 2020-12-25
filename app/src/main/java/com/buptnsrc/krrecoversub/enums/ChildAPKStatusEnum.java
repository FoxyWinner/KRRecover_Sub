package com.buptnsrc.krrecoversub.enums;

/**
 * 该枚举类用来存放一些全局性信息
 */
public enum ChildAPKStatusEnum implements  CodeEnum
{
    // 没安装、安装却没授权、已授权
    NOT_INSTALLED(0),
    INSTALLED_BUT_NOT_GRANTED(1),
    GRANTED(2);
    ;
    private int code;


    ChildAPKStatusEnum(int code)
    {
        this.code = code;
    }

    @Override
    public int getInt()
    {
        return this.code;
    }
}