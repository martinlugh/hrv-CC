package com.hrv.core.nonstationary;

import com.hrv.core.classical.ClassicalAnalysis;
import com.hrv.core.classical.TimeDomainResult;
import com.hrv.core.rri.RRi;

import java.util.ArrayList;
import java.util.List;

/**
 * 非平稳HRV分析。
 * 将RRi序列按时间窗口切割，对每个窗口计算时域指标，获得时变HRV参数。
 * 对应Python原项目中的 hrv/nonstationary.py :: time_varying() 函数。
 */
public class NonStationaryAnalysis {

    /**
     * 计算时变HRV指标。
     * 将RRi序列按seg_size切割为有重叠的时间窗口，对每个窗口计算时域指标。
     * 对应Python: time_varying(rri, seg_size, overlap, keep_last=False)
     *
     * @param rri      输入RRi序列
     * @param segSize  时间窗口大小（秒）
     * @param overlap  窗口重叠时长（秒），默认0
     * @param keepLast 是否保留最后一段不足segSize的数据，默认false
     * @return 时变HRV分析结果
     */
    public static TimeVaryingResult timeVarying(RRi rri, double segSize, double overlap, boolean keepLast) {
        // 按时间窗口切割RRi序列
        List<RRi> segments = rri.timeSplit(segSize, overlap, keepLast);

        // 对每个窗口计算时域HRV指标
        List<TimeDomainResult> results = new ArrayList<>();
        for (RRi segment : segments) {
            // 每个分段必须至少有2个点才能计算diff
            if (segment.size() >= 2) {
                results.add(ClassicalAnalysis.timeDomain(segment));
            }
        }

        return new TimeVaryingResult(rri, results, segments, segSize, overlap);
    }

    /** 默认参数：overlap=0, keepLast=false */
    public static TimeVaryingResult timeVarying(RRi rri, double segSize, double overlap) {
        return timeVarying(rri, segSize, overlap, false);
    }

    /** 接受原始数组的版本 */
    public static TimeVaryingResult timeVarying(double[] rri, double segSize, double overlap, boolean keepLast) {
        return timeVarying(new RRi(rri), segSize, overlap, keepLast);
    }
}
