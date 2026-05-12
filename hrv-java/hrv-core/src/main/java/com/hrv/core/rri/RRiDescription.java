package com.hrv.core.rri;

import com.hrv.core.utils.RRiUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RRi序列描述统计信息。
 * 包含RRi及对应心率(HR)的各项统计指标。
 * 对应Python原项目中的 hrv/rri.py :: RRiDescription 和 _prepare_table 函数。
 */
public class RRiDescription {

    /** 统计指标名称 */
    private final String[] fields = {"min", "max", "mean", "var", "std", "median", "amplitude"};

    /** RRi统计值 */
    private final Map<String, Double> rriStats = new LinkedHashMap<>();

    /** 心率统计值 */
    private final Map<String, Double> hrStats = new LinkedHashMap<>();

    /**
     * 从RRi序列构造描述统计。
     *
     * @param rri 输入的RRi序列
     */
    public RRiDescription(RRi rri) {
        double[] values = rri.getRri();
        double[] hr = rri.toHr();

        // 计算各项统计指标
        rriStats.put("min", rri.min());
        rriStats.put("max", rri.max());
        rriStats.put("mean", rri.mean());
        rriStats.put("var", rri.var());
        rriStats.put("std", rri.std());
        rriStats.put("median", rri.median());
        rriStats.put("amplitude", rri.amplitude());

        hrStats.put("min", RRiUtils.min(hr));
        hrStats.put("max", RRiUtils.max(hr));
        hrStats.put("mean", RRiUtils.mean(hr));
        hrStats.put("var", RRiUtils.variance(hr));
        hrStats.put("std", Math.sqrt(RRiUtils.variance(hr)));
        hrStats.put("median", RRiUtils.median(hr));
        hrStats.put("amplitude", RRiUtils.max(hr) - RRiUtils.min(hr));
    }

    /**
     * 获取RRi的某一统计指标。
     *
     * @param field 指标名（min/max/mean/var/std/median/amplitude）
     * @return RRi对应的统计值
     */
    public double getRri(String field) {
        if (!rriStats.containsKey(field)) {
            throw new IllegalArgumentException("未知统计指标: " + field);
        }
        return rriStats.get(field);
    }

    /**
     * 获取心率(HR)的某一统计指标。
     *
     * @param field 指标名（min/max/mean/var/std/median/amplitude）
     * @return 心率对应的统计值
     */
    public double getHr(String field) {
        if (!hrStats.containsKey(field)) {
            throw new IllegalArgumentException("未知统计指标: " + field);
        }
        return hrStats.get(field);
    }

    /** @return 全部RRi统计指标Map */
    public Map<String, Double> getRriStats() { return new LinkedHashMap<>(rriStats); }

    /** @return 全部HR统计指标Map */
    public Map<String, Double> getHrStats() { return new LinkedHashMap<>(hrStats); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String dash = "-".repeat(40) + "\n";
        sb.append(dash);
        sb.append(String.format("%-10s%12s%12s%n", "", "rri", "hr"));
        sb.append(dash);
        for (String field : fields) {
            sb.append(String.format("%-10s%12.2f%12.2f%n", field, rriStats.get(field), hrStats.get(field)));
        }
        return sb.toString();
    }
}
