package org.xiyu.yee.createplus.settings;

/** 可以用滑块（鼠标拖动）调整的设置项。 */
public interface Slidable {
    /** 当前值在取值区间内的比例，0.0 ~ 1.0。 */
    double getFraction();

    /** 按比例设置值，输入会被限制到 0.0 ~ 1.0。 */
    void setFraction(double fraction);
}
