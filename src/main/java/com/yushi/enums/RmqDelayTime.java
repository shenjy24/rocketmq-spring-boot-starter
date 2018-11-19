/*
 * Copyright (C) 2016  HangZhou YuShi Technology Co.Ltd  Holdings Ltd. All rights reserved
 *
 * 本代码版权归杭州宇石科技所有，且受到相关的法律保护。
 * 没有经过版权所有者的书面同意，
 * 任何其他个人或组织均不得以任何形式将本文件或本文件的部分代码用于其他商业用途。
 *
 */
package com.yushi.enums;

import com.yushi.support.utils.GwsUtil;

/**
 * Rocket mq 延时时间
 *
 */
public enum RmqDelayTime {
    s1("1", 1, "1秒"),
    s5("2", 5, "5秒"),
    s10("3", 10, "10秒"),
    s30("4", 30, "30秒"),
    m1("5", 60 * 1, "1分"),
    m2("6", 60 * 2, "2分"),
    m3("7", 60 * 3, "3分"),
    m4("8", 60 * 4, "4分"),
    m5("9", 60 * 5, "5分"),
    m6("10", 60 * 6, "6分"),
    m7("11", 60 * 7, "7分"),
    m8("12", 60 * 8, "8分"),
    m9("13", 60 * 9, "9分"),
    m10("14", 60 * 10, "10分"),
    m20("15", 60 * 20, "20分"),
    m30("16", 60 * 30, "30分"),
    h1("17", 60 * 60 * 1, "1小时"),
    h2("18", 60 * 60 * 2, "2小时");

    private String delayTimeLevel;
    private int delaySecondTime;
    private String note;
    private static int[] rmqDelayTimeArrs = null;

    private RmqDelayTime(String delayTimeLevel, int delaySecondTime, String note) {
        this.delayTimeLevel = delayTimeLevel;
        this.delaySecondTime = delaySecondTime;
        this.note = note;
    }

    public static RmqDelayTime getTargetRmqDelayTime(int targetTime) {
        if (null == rmqDelayTimeArrs || rmqDelayTimeArrs.length <= 0) {
            rmqDelayTimeArrs = new int[RmqDelayTime.values().length];
            int i = 0;
            for (RmqDelayTime rmqDelayTime : RmqDelayTime.values()) {
                rmqDelayTimeArrs[i] = rmqDelayTime.getDelaySecondTime();
                System.out.println(rmqDelayTimeArrs[i]);
                i++;
            }
        }

        int ret = GwsUtil.binarysearchKey(rmqDelayTimeArrs, targetTime);
        for (RmqDelayTime rmqDelayTime : RmqDelayTime.values()) {
            if (rmqDelayTime.getDelaySecondTime() == ret) {
                return rmqDelayTime;
            }
        }
        return RmqDelayTime.h2;
    }


    /**
     * @return the delayTimeLevel
     */
    public String getDelayTimeLevel() {
        return delayTimeLevel;
    }

    /**
     * @param delayTimeLevel the delayTimeLevel to set
     */
    public void setDelayTimeLevel(String delayTimeLevel) {
        this.delayTimeLevel = delayTimeLevel;
    }


    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * @return the delaySecondTime
     */
    public int getDelaySecondTime() {
        return delaySecondTime;
    }

    /**
     * @param delaySecondTime the delaySecondTime to set
     */
    public void setDelaySecondTime(int delaySecondTime) {
        this.delaySecondTime = delaySecondTime;
    }

}
