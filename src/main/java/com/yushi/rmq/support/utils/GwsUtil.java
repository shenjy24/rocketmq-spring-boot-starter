/*
 * Copyright (C) 2016  HangZhou YuShi Technology Co.Ltd  Holdings Ltd. All rights reserved
 *
 * 本代码版权归杭州宇石科技所有，且受到相关的法律保护。
 * 没有经过版权所有者的书面同意，
 * 任何其他个人或组织均不得以任何形式将本文件或本文件的部分代码用于其他商业用途。
 *
 */
package com.yushi.rmq.support.utils;

import java.util.Arrays;

/**
 * 【GwsUtil】
 *
 * @author wenfei  2017年4月6日 上午11:57:29
 */
public class GwsUtil {

    /**
     * 【ID获取】
     *
     * @return
     * @author wenfei 2017年4月6日
     */
    public static Long getSeq() {
        Long maxNum = System.currentTimeMillis();
        Long randNum = Math.round(Math.random() * 1000);
        return maxNum * 1000 + randNum;
    }

    public static String nullToStr(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString().trim();
    }

    /**
     * 查找数组中与目标数最接近的数
     *
     * @param array
     * @param targetNum
     * @return
     * @author leiyongping 2017年6月5日 下午3:15:59
     */
    public static int binarysearchKey(int[] array, int targetNum) {
        Arrays.sort(array);
        int left = 0, right = 0;
        for (right = array.length - 1; left != right; ) {
            int midIndex = (right + left) / 2;
            int mid = (right - left);
            int midValue = array[midIndex];
            if (targetNum == midValue) {
                return targetNum;
            }

            if (targetNum > midValue) {
                left = midIndex;
            } else {
                right = midIndex;
            }

            if (mid <= 2) {
                break;
            }
        }
        int rightnum = array[right];
        int leftnum = array[left];
        int ret = Math.abs((rightnum - leftnum) / 2) > Math.abs(rightnum - targetNum) ? rightnum : leftnum;
        if (targetNum > ret) {
            ret = rightnum;
        }
        return ret;
    }
}
