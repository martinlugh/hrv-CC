package com.hrv.io.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * HRV分析JSON输入DTO。
 * 所有字段名与Python原项目保持一致，通过Jackson注解维护。
 *
 * <p>JSON输入示例：
 * <pre>
 * {
 *   "rri": [800, 810, 790, 820, 815],
 *   "time": null,
 *   "fs": 4.0,
 *   "method": "welch",
 *   "interp_method": "cubic",
 *   "detrend": "constant",
 *   "vlf_band": [0, 0.04],
 *   "lf_band": [0.04, 0.15],
 *   "hf_band": [0.15, 0.4],
 *   "nperseg": 256,
 *   "noverlap": 128,
 *   "window": "hanning",
 *   "order": 16,
 *   "filter_type": null,
 *   "filter_threshold": "medium",
 *   "filter_order": 3,
 *   "detrend_method": null,
 *   "detrend_degree": 1,
 *   "seg_size": 300.0,
 *   "overlap": 0.0,
 *   "analyses": ["time_domain", "frequency_domain", "non_linear"]
 * }
 * </pre>
 */
public class HrvAnalysisRequest {

    /** RRi序列数组（毫秒或秒，自动检测单位） */
    @JsonProperty("rri")
    private List<Double> rri;

    /** 时间数组（秒），为null时自动生成 */
    @JsonProperty("time")
    private List<Double> time;

    /** 采样频率（Hz），默认4.0 */
    @JsonProperty("fs")
    private double fs = 4.0;

    /** PSD估计方法："welch"或"ar"，默认"welch" */
    @JsonProperty("method")
    private String method = "welch";

    /** 插值方法："cubic"或"linear"，默认"cubic" */
    @JsonProperty("interp_method")
    private String interpMethod = "cubic";

    /** 去趋势方式："constant"或"false"，默认"constant" */
    @JsonProperty("detrend")
    private String detrend = "constant";

    /** VLF频段 [低, 高]（Hz），默认[0, 0.04] */
    @JsonProperty("vlf_band")
    private double[] vlfBand = {0, 0.04};

    /** LF频段 [低, 高]（Hz），默认[0.04, 0.15] */
    @JsonProperty("lf_band")
    private double[] lfBand = {0.04, 0.15};

    /** HF频段 [低, 高]（Hz），默认[0.15, 0.4] */
    @JsonProperty("hf_band")
    private double[] hfBand = {0.15, 0.4};

    /** Welch分段长度，默认256 */
    @JsonProperty("nperseg")
    private int nperseg = 256;

    /** Welch分段重叠，默认128 */
    @JsonProperty("noverlap")
    private int noverlap = 128;

    /** 窗函数名称，默认"hanning" */
    @JsonProperty("window")
    private String window = "hanning";

    /** AR模型阶数（method="ar"时使用），默认16 */
    @JsonProperty("order")
    private int order = 16;

    /** 滤波类型："quotient"/"moving_average"/"moving_median"/"threshold"，null表示不滤波 */
    @JsonProperty("filter_type")
    private String filterType;

    /** threshold_filter阈值（ms）或预设名称，默认"medium" */
    @JsonProperty("filter_threshold")
    private String filterThreshold = "medium";

    /** 移动滤波阶数，默认3 */
    @JsonProperty("filter_order")
    private int filterOrder = 3;

    /** 去趋势方法："polynomial"/"smoothness_priors"/"sg"，null表示不去趋势 */
    @JsonProperty("detrend_method")
    private String detrendMethod;

    /** 多项式去趋势阶数，默认1 */
    @JsonProperty("detrend_degree")
    private int detrendDegree = 1;

    /** 平滑先验正则化参数，默认500 */
    @JsonProperty("smoothness_priors_l")
    private double smoothnessPriorsL = 500.0;

    /** SG滤波窗长，默认51 */
    @JsonProperty("sg_window_length")
    private int sgWindowLength = 51;

    /** SG滤波多项式阶数，默认3 */
    @JsonProperty("sg_polyorder")
    private int sgPolyorder = 3;

    /** 时变分析时间窗口大小（秒） */
    @JsonProperty("seg_size")
    private double segSize = 300.0;

    /** 时变分析窗口重叠（秒） */
    @JsonProperty("overlap")
    private double overlap = 0.0;

    /** 要执行的分析列表，默认全部 */
    @JsonProperty("analyses")
    private List<String> analyses;

    // Getters and Setters
    public List<Double> getRri() { return rri; }
    public void setRri(List<Double> rri) { this.rri = rri; }

    public List<Double> getTime() { return time; }
    public void setTime(List<Double> time) { this.time = time; }

    public double getFs() { return fs; }
    public void setFs(double fs) { this.fs = fs; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getInterpMethod() { return interpMethod; }
    public void setInterpMethod(String interpMethod) { this.interpMethod = interpMethod; }

    public String getDetrend() { return detrend; }
    public void setDetrend(String detrend) { this.detrend = detrend; }

    public double[] getVlfBand() { return vlfBand; }
    public void setVlfBand(double[] vlfBand) { this.vlfBand = vlfBand; }

    public double[] getLfBand() { return lfBand; }
    public void setLfBand(double[] lfBand) { this.lfBand = lfBand; }

    public double[] getHfBand() { return hfBand; }
    public void setHfBand(double[] hfBand) { this.hfBand = hfBand; }

    public int getNperseg() { return nperseg; }
    public void setNperseg(int nperseg) { this.nperseg = nperseg; }

    public int getNoverlap() { return noverlap; }
    public void setNoverlap(int noverlap) { this.noverlap = noverlap; }

    public String getWindow() { return window; }
    public void setWindow(String window) { this.window = window; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public String getFilterType() { return filterType; }
    public void setFilterType(String filterType) { this.filterType = filterType; }

    public String getFilterThreshold() { return filterThreshold; }
    public void setFilterThreshold(String filterThreshold) { this.filterThreshold = filterThreshold; }

    public int getFilterOrder() { return filterOrder; }
    public void setFilterOrder(int filterOrder) { this.filterOrder = filterOrder; }

    public String getDetrendMethod() { return detrendMethod; }
    public void setDetrendMethod(String detrendMethod) { this.detrendMethod = detrendMethod; }

    public int getDetrendDegree() { return detrendDegree; }
    public void setDetrendDegree(int detrendDegree) { this.detrendDegree = detrendDegree; }

    public double getSegSize() { return segSize; }
    public void setSegSize(double segSize) { this.segSize = segSize; }

    public double getOverlap() { return overlap; }
    public void setOverlap(double overlap) { this.overlap = overlap; }

    public List<String> getAnalyses() { return analyses; }
    public void setAnalyses(List<String> analyses) { this.analyses = analyses; }

    public double getSmoothnesssPriorsL() { return smoothnessPriorsL; }
    public void setSmoothnesssPriorsL(double l) { this.smoothnessPriorsL = l; }

    public int getSgWindowLength() { return sgWindowLength; }
    public void setSgWindowLength(int sgWindowLength) { this.sgWindowLength = sgWindowLength; }

    public int getSgPolyorder() { return sgPolyorder; }
    public void setSgPolyorder(int sgPolyorder) { this.sgPolyorder = sgPolyorder; }

    /** 将List<Double>格式的RRi转为double[] */
    public double[] getRriArray() {
        if (rri == null) return new double[0];
        double[] arr = new double[rri.size()];
        for (int i = 0; i < rri.size(); i++) arr[i] = rri.get(i);
        return arr;
    }

    /** 将List<Double>格式的time转为double[]，无时间信息时返回null */
    public double[] getTimeArray() {
        if (time == null || time.isEmpty()) return null;
        double[] arr = new double[time.size()];
        for (int i = 0; i < time.size(); i++) arr[i] = time.get(i);
        return arr;
    }
}
