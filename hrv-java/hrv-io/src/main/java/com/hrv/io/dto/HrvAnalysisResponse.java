package com.hrv.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrv.core.classical.FrequencyDomainResult;
import com.hrv.core.classical.NonLinearResult;
import com.hrv.core.classical.TimeDomainResult;

/**
 * HRV分析JSON输出DTO。
 * 包含时域、频域和非线性分析结果，以及执行状态信息。
 * 所有字段名与Python原项目保持一致。
 *
 * <p>JSON输出示例：
 * <pre>
 * {
 *   "status": "success",
 *   "error": null,
 *   "time_domain": {
 *     "rmssd": 55.14,
 *     "sdnn": 57.82,
 *     "sdsd": 55.17,
 *     "nn50": 321,
 *     "pnn50": 35.27,
 *     "mrri": 1058.72,
 *     "mhr": 56.85
 *   },
 *   "frequency_domain": {
 *     "total_power": 2212.29,
 *     "vlf": 546.82,
 *     "lf": 770.45,
 *     "hf": 895.03,
 *     "lf_hf": 0.86,
 *     "lfnu": 46.26,
 *     "hfnu": 53.74
 *   },
 *   "non_linear": {
 *     "sd1": 39.01,
 *     "sd2": 71.86
 *   }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HrvAnalysisResponse {

    /** 执行状态："success"或"error" */
    @JsonProperty("status")
    private String status;

    /** 错误信息（成功时为null） */
    @JsonProperty("error")
    private String error;

    /** 时域分析结果 */
    @JsonProperty("time_domain")
    private TimeDomainResult timeDomain;

    /** 频域分析结果 */
    @JsonProperty("frequency_domain")
    private FrequencyDomainResult frequencyDomain;

    /** 非线性分析结果 */
    @JsonProperty("non_linear")
    private NonLinearResult nonLinear;

    public HrvAnalysisResponse() {}

    /** 成功响应构造 */
    public static HrvAnalysisResponse success(TimeDomainResult td, FrequencyDomainResult fd, NonLinearResult nl) {
        HrvAnalysisResponse resp = new HrvAnalysisResponse();
        resp.status = "success";
        resp.timeDomain = td;
        resp.frequencyDomain = fd;
        resp.nonLinear = nl;
        return resp;
    }

    /** 错误响应构造 */
    public static HrvAnalysisResponse error(String message) {
        HrvAnalysisResponse resp = new HrvAnalysisResponse();
        resp.status = "error";
        resp.error = message;
        return resp;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public TimeDomainResult getTimeDomain() { return timeDomain; }
    public void setTimeDomain(TimeDomainResult timeDomain) { this.timeDomain = timeDomain; }

    public FrequencyDomainResult getFrequencyDomain() { return frequencyDomain; }
    public void setFrequencyDomain(FrequencyDomainResult frequencyDomain) { this.frequencyDomain = frequencyDomain; }

    public NonLinearResult getNonLinear() { return nonLinear; }
    public void setNonLinear(NonLinearResult nonLinear) { this.nonLinear = nonLinear; }
}
