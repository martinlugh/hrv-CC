package com.hrv.core.classical;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 频域分析结果。
 * 包含7个频域HRV指标，字段名严格与Python原项目保持一致。
 * 对应Python: frequency_domain() 返回的字典。
 *
 * <p>频段定义（来源：Task Force 1996标准）：
 * <ul>
 *   <li>VLF: 0 ~ 0.04 Hz</li>
 *   <li>LF:  0.04 ~ 0.15 Hz</li>
 *   <li>HF:  0.15 ~ 0.4 Hz</li>
 * </ul>
 *
 * <p>归一化公式：
 * <pre>
 *   LFnu = LF / (total_power - VLF) * 100
 *   HFnu = HF / (total_power - VLF) * 100
 * </pre>
 */
public class FrequencyDomainResult {

    /** 总功率（VLF+LF+HF，ms²） */
    @JsonProperty("total_power")
    private double totalPower;

    /** 极低频功率（0~0.04 Hz，ms²） */
    @JsonProperty("vlf")
    private double vlf;

    /** 低频功率（0.04~0.15 Hz，ms²） */
    @JsonProperty("lf")
    private double lf;

    /** 高频功率（0.15~0.4 Hz，ms²） */
    @JsonProperty("hf")
    private double hf;

    /** LF/HF比值 */
    @JsonProperty("lf_hf")
    private double lfHf;

    /** 归一化低频功率（%） */
    @JsonProperty("lfnu")
    private double lfnu;

    /** 归一化高频功率（%） */
    @JsonProperty("hfnu")
    private double hfnu;

    public FrequencyDomainResult() {}

    public FrequencyDomainResult(double totalPower, double vlf, double lf, double hf,
                                  double lfHf, double lfnu, double hfnu) {
        this.totalPower = totalPower;
        this.vlf = vlf;
        this.lf = lf;
        this.hf = hf;
        this.lfHf = lfHf;
        this.lfnu = lfnu;
        this.hfnu = hfnu;
    }

    public double getTotalPower() { return totalPower; }
    public void setTotalPower(double totalPower) { this.totalPower = totalPower; }

    public double getVlf() { return vlf; }
    public void setVlf(double vlf) { this.vlf = vlf; }

    public double getLf() { return lf; }
    public void setLf(double lf) { this.lf = lf; }

    public double getHf() { return hf; }
    public void setHf(double hf) { this.hf = hf; }

    public double getLfHf() { return lfHf; }
    public void setLfHf(double lfHf) { this.lfHf = lfHf; }

    public double getLfnu() { return lfnu; }
    public void setLfnu(double lfnu) { this.lfnu = lfnu; }

    public double getHfnu() { return hfnu; }
    public void setHfnu(double hfnu) { this.hfnu = hfnu; }

    @Override
    public String toString() {
        return String.format(
            "{total_power=%.4f, vlf=%.4f, lf=%.4f, hf=%.4f, lf_hf=%.4f, lfnu=%.4f, hfnu=%.4f}",
            totalPower, vlf, lf, hf, lfHf, lfnu, hfnu
        );
    }
}
