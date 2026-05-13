package com.hrv.core.classical;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 时域分析结果。
 * 包含7个时域HRV指标，字段名严格与Python原项目保持一致。
 * 对应Python: time_domain() 返回的字典。
 */
public class TimeDomainResult {

    /** 连续差值的均方根（Root Mean Square of Successive Differences，毫秒） */
    @JsonProperty("rmssd")
    private double rmssd;

    /** RRi序列标准差（Standard Deviation of NN intervals，毫秒，ddof=1） */
    @JsonProperty("sdnn")
    private double sdnn;

    /** 连续差值的标准差（Standard Deviation of Successive Differences，毫秒，ddof=1） */
    @JsonProperty("sdsd")
    private double sdsd;

    /** 连续差值超过50ms的个数 */
    @JsonProperty("nn50")
    private int nn50;

    /** 连续差值超过50ms的百分比（%） */
    @JsonProperty("pnn50")
    private double pnn50;

    /** RRi序列均值（Mean RRi，毫秒） */
    @JsonProperty("mrri")
    private double mrri;

    /** 心率均值（Mean Heart Rate，bpm） */
    @JsonProperty("mhr")
    private double mhr;

    public TimeDomainResult() {}

    public TimeDomainResult(double rmssd, double sdnn, double sdsd, int nn50, double pnn50, double mrri, double mhr) {
        this.rmssd = rmssd;
        this.sdnn = sdnn;
        this.sdsd = sdsd;
        this.nn50 = nn50;
        this.pnn50 = pnn50;
        this.mrri = mrri;
        this.mhr = mhr;
    }

    public double getRmssd() { return rmssd; }
    public void setRmssd(double rmssd) { this.rmssd = rmssd; }

    public double getSdnn() { return sdnn; }
    public void setSdnn(double sdnn) { this.sdnn = sdnn; }

    public double getSdsd() { return sdsd; }
    public void setSdsd(double sdsd) { this.sdsd = sdsd; }

    public int getNn50() { return nn50; }
    public void setNn50(int nn50) { this.nn50 = nn50; }

    public double getPnn50() { return pnn50; }
    public void setPnn50(double pnn50) { this.pnn50 = pnn50; }

    public double getMrri() { return mrri; }
    public void setMrri(double mrri) { this.mrri = mrri; }

    public double getMhr() { return mhr; }
    public void setMhr(double mhr) { this.mhr = mhr; }

    @Override
    public String toString() {
        return String.format(
            "{rmssd=%.4f, sdnn=%.4f, sdsd=%.4f, nn50=%d, pnn50=%.4f, mrri=%.4f, mhr=%.4f}",
            rmssd, sdnn, sdsd, nn50, pnn50, mrri, mhr
        );
    }
}
