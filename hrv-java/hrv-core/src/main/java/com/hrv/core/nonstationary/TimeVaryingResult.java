package com.hrv.core.nonstationary;

import com.hrv.core.classical.TimeDomainResult;
import com.hrv.core.rri.RRi;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * 非平稳时变HRV分析结果。
 * 存储按时间窗口计算的时域HRV指标序列。
 * 对应Python原项目中的 hrv/nonstationary.py :: TimeVarying 类。
 */
public class TimeVaryingResult {

    /** 原始完整RRi序列 */
    private final RRi rri;

    /** 各时间窗口的时域分析结果列表 */
    private final List<TimeDomainResult> results;

    /** 各时间窗口的RRi分段列表 */
    private final List<RRi> rriSegments;

    /** 时间窗口大小（秒） */
    private final double segSize;

    /** 窗口重叠时长（秒） */
    private final double overlap;

    /**
     * 构造时变结果对象。
     *
     * @param rri         原始RRi序列
     * @param results     各窗口的时域分析结果
     * @param rriSegments 各窗口的RRi分段
     * @param segSize     窗口大小（秒）
     * @param overlap     重叠时长（秒）
     */
    public TimeVaryingResult(RRi rri, List<TimeDomainResult> results,
                              List<RRi> rriSegments, double segSize, double overlap) {
        this.rri = rri;
        this.results = results;
        this.rriSegments = rriSegments;
        this.segSize = segSize;
        this.overlap = overlap;
    }

    /** @return 原始RRi序列 */
    public RRi getRri() { return rri; }

    /** @return 各窗口时域分析结果列表 */
    public List<TimeDomainResult> getResults() { return results; }

    /** @return 各窗口RRi分段列表 */
    public List<RRi> getRriSegments() { return rriSegments; }

    /** @return 时间窗口大小（秒） */
    public double getSegSize() { return segSize; }

    /** @return 窗口重叠时长（秒） */
    public double getOverlap() { return overlap; }

    /**
     * 获取各时间窗口的中心时间（秒）。
     * 对应Python: build_xaxis() - 取每段时间数组的中位数
     *
     * @return 各窗口中心时间列表
     */
    public List<Double> buildXaxis() {
        List<Double> xaxis = new ArrayList<>();
        for (RRi seg : rriSegments) {
            double[] segTime = seg.getTime();
            // 取时间数组的中位数作为该段的代表时间点
            double[] sorted = segTime.clone();
            java.util.Arrays.sort(sorted);
            double median;
            int n = sorted.length;
            if (n % 2 == 0) {
                median = (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
            } else {
                median = sorted[n / 2];
            }
            xaxis.add(median);
        }
        return xaxis;
    }

    /**
     * 获取指定时域指标的时间序列。
     * 对应Python: TimeVarying.__getattr__(index)
     *
     * @param index 指标名称（rmssd/sdnn/sdsd/nn50/pnn50/mrri/mhr）
     * @return 该指标在各时间窗口的值列表
     */
    public List<Double> getIndex(String index) {
        List<Double> values = new ArrayList<>();
        for (TimeDomainResult r : results) {
            switch (index.toLowerCase()) {
                case "rmssd": values.add(r.getRmssd()); break;
                case "sdnn":  values.add(r.getSdnn()); break;
                case "sdsd":  values.add(r.getSdsd()); break;
                case "nn50":  values.add((double) r.getNn50()); break;
                case "pnn50": values.add(r.getPnn50()); break;
                case "mrri":  values.add(r.getMrri()); break;
                case "mhr":   values.add(r.getMhr()); break;
                default:
                    throw new IllegalArgumentException("index `" + index + "` does not exist.");
            }
        }
        return values;
    }

    /**
     * 返回y轴标签映射。
     * 对应Python: TimeVarying.ylabel_mapper
     *
     * @param index 指标名称
     * @return 标签字符串（含单位）
     */
    public String ylabelMapper(String index) {
        Map<String, String> mapper = new LinkedHashMap<>();
        mapper.put("rmssd", "RMSSD (ms)");
        mapper.put("sdnn",  "SDNN (ms²)");
        mapper.put("sdsd",  "SDSD (ms²)");
        mapper.put("nn50",  "nn50 (count)");
        mapper.put("pnn50", "pnn50 (%)");
        mapper.put("mrri",  "mean RRi (ms)");
        mapper.put("mhr",   "mean HR (bpm)");
        return mapper.get(index.toLowerCase());
    }

    /**
     * 以转置格式（指标→值列表）返回所有结果。
     * 对应Python: TimeVarying.transponsed
     *
     * @return Map，key为指标名，value为各窗口的值列表
     */
    public Map<String, List<Double>> getTransposed() {
        Map<String, List<Double>> transposed = new LinkedHashMap<>();
        String[] keys = {"rmssd", "sdnn", "sdsd", "nn50", "pnn50", "mrri", "mhr"};
        for (String key : keys) {
            transposed.put(key, getIndex(key));
        }
        return transposed;
    }

    @Override
    public String toString() {
        return String.format("Time Varying %.1f:%.1f - #%d", segSize, overlap, results.size());
    }
}
