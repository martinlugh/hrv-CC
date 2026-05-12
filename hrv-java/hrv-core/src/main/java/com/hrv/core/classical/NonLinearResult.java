package com.hrv.core.classical;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 非线性分析结果（Poincaré散点图分析）。
 * 包含SD1和SD2两个指标，字段名严格与Python原项目保持一致。
 * 对应Python: non_linear() 返回的字典。
 *
 * <p>计算公式（来源：Task Force 1996标准）：
 * <pre>
 *   SD1 = sqrt(0.5 * std(diff(rri), ddof=1)^2)
 *   SD2 = sqrt(2 * std(rri, ddof=1)^2 - 0.5 * std(diff(rri), ddof=1)^2)
 * </pre>
 */
public class NonLinearResult {

    /** Poincaré图短轴（垂直y=x方向的标准差，反映短期变异性，毫秒） */
    @JsonProperty("sd1")
    private double sd1;

    /** Poincaré图长轴（沿y=x方向的标准差，反映长期变异性，毫秒） */
    @JsonProperty("sd2")
    private double sd2;

    public NonLinearResult() {}

    public NonLinearResult(double sd1, double sd2) {
        this.sd1 = sd1;
        this.sd2 = sd2;
    }

    public double getSd1() { return sd1; }
    public void setSd1(double sd1) { this.sd1 = sd1; }

    public double getSd2() { return sd2; }
    public void setSd2(double sd2) { this.sd2 = sd2; }

    @Override
    public String toString() {
        return String.format("{sd1=%.4f, sd2=%.4f}", sd1, sd2);
    }
}
