package com.hrv.core.preprocessing;

/**
 * RRI数据清洗工具类。
 * 移植自 https://github.com/Aura-healthcare/hrv-analysis 的 preprocessing.py。
 *
 * <p>核心清洗流程（{@link #getNnIntervals}，对应Python get_nn_intervals）：
 * <ol>
 *   <li>{@link #removeOutliers}：将范围外（&lt;lowRri 或 &gt;highRri）的值替换为 NaN</li>
 *   <li>{@link #interpolateNanValues}：对 NaN 进行线性插值（第一次）</li>
 *   <li>{@link #removeEctopicBeats}：按选定方法检测异常搏动，替换为 NaN</li>
 *   <li>{@link #interpolateNanValues}：再次线性插值（第二次）</li>
 * </ol>
 *
 * <p>异常搏动检测方法（对应 method 参数）：
 * <ul>
 *   <li>{@link #MALIK_RULE}：|RR[i] - RR[i+1]| &lt;= 0.2 * RR[i]</li>
 *   <li>{@link #KAMATH_RULE}：增幅 &lt;= 32.5% 且 减幅 &lt;= 24.5%（非对称）</li>
 *   <li>{@link #KARLSSON_RULE}：与前后相邻均值偏差 &lt; 20%</li>
 *   <li>{@link #ACAR_RULE}：与过去9个均值偏差 &lt; 20%</li>
 *   <li>{@link #CUSTOM_RULE}：用户指定百分比阈值</li>
 * </ul>
 *
 * <p>默认阈值（来源：Task Force 1996 + 各方法参考文献）：
 * <ul>
 *   <li>lowRri = 300 ms（对应 BPM 200，最大心率）</li>
 *   <li>highRri = 2000 ms（对应 BPM 30，最低心率）</li>
 *   <li>removingRule = 0.2（20% 容差，Malik/Karlsson/ACAR/Custom 默认）</li>
 * </ul>
 *
 * <p>参考文献：
 * <ul>
 *   <li>[Malik] Geometric Methods for HRV Assessment, Malik M et al</li>
 *   <li>[Kamath] Correction of HRV Signal for Ectopics, Kamath M.V. et al</li>
 *   <li>[Karlsson] Automatic filtering of outliers in RR-intervals, Karlsson M et al</li>
 *   <li>[ACAR] Automatic ectopic beat elimination, Acar B et al</li>
 * </ul>
 */
public class RRiPreprocessing {

    /** Malik方法标识符 */
    public static final String MALIK_RULE = "malik";

    /** Karlsson方法标识符 */
    public static final String KARLSSON_RULE = "karlsson";

    /** Kamath方法标识符 */
    public static final String KAMATH_RULE = "kamath";

    /** ACAR方法标识符 */
    public static final String ACAR_RULE = "acar";

    /** 自定义方法标识符 */
    public static final String CUSTOM_RULE = "custom";

    // ==================== 步骤1：范围过滤 ====================

    /**
     * 将超出生理范围的 RRI 值替换为 NaN（不删除，保留位置用于后续插值）。
     * 对应Python: remove_outliers(rr_intervals, low_rri=300, high_rri=2000)
     *
     * <p>规则：若 rri[i] &lt; lowRri 或 rri[i] &gt; highRri，则替换为 Double.NaN。
     *
     * @param rriIntervals 原始 RRI 数组（毫秒）
     * @param lowRri       最小有效 RRI，默认 300ms（BPM 200）
     * @param highRri      最大有效 RRI，默认 2000ms（BPM 30）
     * @return 超范围值替换为 NaN 后的新数组
     */
    public static double[] removeOutliers(double[] rriIntervals, int lowRri, int highRri) {
        double[] result = new double[rriIntervals.length];
        for (int i = 0; i < rriIntervals.length; i++) {
            double rri = rriIntervals[i];
            result[i] = (rri >= lowRri && rri <= highRri) ? rri : Double.NaN;
        }
        return result;
    }

    /** 默认范围：low=300ms，high=2000ms */
    public static double[] removeOutliers(double[] rriIntervals) {
        return removeOutliers(rriIntervals, 300, 2000);
    }

    // ==================== 步骤2/4：线性插值 ====================

    /**
     * 对数组中的 NaN 值进行线性插值。
     * 对应Python: interpolate_nan_values(rr_intervals, interpolation_method="linear")
     *
     * <p>处理规则：
     * <ul>
     *   <li>开头 NaN：用第一个有效值向前填充（pandas limit_direction="forward" 等效）</li>
     *   <li>中间 NaN：在前后有效值之间做线性插值</li>
     *   <li>末尾 NaN：用最后一个有效值向后填充</li>
     * </ul>
     *
     * @param rriIntervals 含 NaN 的 RRI 数组
     * @return 插值填补后的新数组
     */
    public static double[] interpolateNanValues(double[] rriIntervals) {
        int n = rriIntervals.length;
        double[] result = rriIntervals.clone();

        // 1. 处理开头 NaN：找到第一个有效值，向前填充
        if (Double.isNaN(result[0])) {
            int firstValid = 0;
            while (firstValid < n && Double.isNaN(result[firstValid])) firstValid++;
            if (firstValid < n) {
                for (int i = 0; i < firstValid; i++) result[i] = result[firstValid];
            }
        }

        // 2. 线性插值中间和末尾的 NaN
        int i = 0;
        while (i < n) {
            if (Double.isNaN(result[i])) {
                int left = i - 1;  // 前一个有效位置
                int right = i + 1;
                while (right < n && Double.isNaN(result[right])) right++;

                if (right >= n) {
                    // 末尾 NaN：用最后有效值填充
                    double fillVal = (left >= 0) ? result[left] : Double.NaN;
                    for (int j = i; j < n; j++) result[j] = fillVal;
                    break;
                } else if (left < 0) {
                    // 开头（理论上已处理）
                    for (int j = i; j < right; j++) result[j] = result[right];
                } else {
                    // 线性插值：在 result[left] 和 result[right] 之间插值
                    double leftVal = result[left];
                    double rightVal = result[right];
                    int span = right - left;
                    for (int j = i; j < right; j++) {
                        double ratio = (double) (j - left) / span;
                        result[j] = leftVal + ratio * (rightVal - leftVal);
                    }
                }
                i = right + 1;
            } else {
                i++;
            }
        }
        return result;
    }

    // ==================== 步骤3：异常搏动检测 ====================

    /**
     * 检查相邻两个 RRI 是否满足有效性判断。
     * 对应Python: is_rr_interval_within_bounds(rr_interval, next_rr_interval, method, custom_rule)
     *
     * <p>Malik：|RR[i] - RR[i+1]| &lt;= 0.2 * RR[i]
     * <p>Kamath：(0 &lt;= RR[i+1]-RR[i] &lt;= 0.325*RR[i]) OR (0 &lt;= RR[i]-RR[i+1] &lt;= 0.245*RR[i])
     * <p>Custom：|RR[i] - RR[i+1]| &lt;= customRule * RR[i]
     *
     * @param rri      当前 RRI
     * @param nextRri  下一个 RRI
     * @param method   方法名（malik/kamath/custom）
     * @param customRule 自定义阈值百分比
     * @return true 表示有效，false 表示异常
     */
    public static boolean isRriWithinBounds(double rri, double nextRri,
                                             String method, double customRule) {
        switch (method) {
            case MALIK_RULE:
                // |RR[i] - RR[i+1]| <= 0.2 * RR[i]
                return Math.abs(rri - nextRri) <= 0.2 * rri;

            case KAMATH_RULE: {
                // 增幅 <= 32.5% 或 减幅 <= 24.5%（非对称检测）
                double delta = nextRri - rri;
                boolean rising  = (delta >= 0) && (delta <= 0.325 * rri);
                boolean falling = (-delta >= 0) && (-delta <= 0.245 * rri);
                return rising || falling;
            }

            default:
                // custom（以及兜底）
                return Math.abs(rri - nextRri) <= customRule * rri;
        }
    }

    /**
     * Karlsson方法内部实现。
     * 对应Python: _remove_outlier_karlsson(rr_intervals, removing_rule=0.2)
     *
     * <p>公式：mean_ij = (RR[i] + RR[i+2]) / 2
     * <br>有效条件：|RR[i+1] - mean_ij| &lt; removingRule * mean_ij
     *
     * <p>参考：Automatic filtering of outliers in RR-intervals before analysis of HRV
     * in Holter recordings - Karlsson et al.
     */
    private static double[] removeOutlierKarlsson(double[] rriIntervals, double removingRule) {
        int n = rriIntervals.length;
        double[] result = new double[n];
        result[0] = rriIntervals[0];  // 第一个始终保留

        for (int i = 0; i < n - 2; i++) {
            // 前后相邻均值
            double meanPrevNext = (rriIntervals[i] + rriIntervals[i + 2]) / 2.0;
            if (Math.abs(meanPrevNext - rriIntervals[i + 1]) < removingRule * meanPrevNext) {
                result[i + 1] = rriIntervals[i + 1];
            } else {
                result[i + 1] = Double.NaN;
            }
        }
        result[n - 1] = rriIntervals[n - 1];  // 最后一个始终保留
        return result;
    }

    /**
     * ACAR方法内部实现。
     * 对应Python: _remove_outlier_acar(rr_intervals, custom_rule=0.2)
     *
     * <p>公式：mean9 = nanmean(RR[i-9:i])
     * <br>有效条件：|RR[i] - mean9| &lt; customRule * mean9
     * <br>前9个元素直接保留（无前驱窗口）。
     *
     * <p>参考：Automatic ectopic beat elimination in short-term HRV measurements - Acar B et al.
     */
    private static double[] removeOutlierAcar(double[] rriIntervals, double customRule) {
        int n = rriIntervals.length;
        double[] result = new double[n];

        for (int i = 0; i < n; i++) {
            if (i < 9) {
                result[i] = rriIntervals[i];
                continue;
            }
            // 计算过去9个已处理值（忽略 NaN）的均值
            double sum = 0;
            int count = 0;
            for (int j = i - 9; j < i; j++) {
                if (!Double.isNaN(result[j])) { sum += result[j]; count++; }
            }
            if (count == 0) {
                result[i] = rriIntervals[i];
                continue;
            }
            double meanNine = sum / count;
            if (Math.abs(meanNine - rriIntervals[i]) < customRule * meanNine) {
                result[i] = rriIntervals[i];
            } else {
                result[i] = Double.NaN;
            }
        }
        return result;
    }

    /**
     * 移除异常搏动（ectopic beats），替换为 NaN。
     * 对应Python: remove_ectopic_beats(rr_intervals, method="malik", custom_removing_rule=0.2)
     *
     * <p>对于 Malik/Kamath/Custom 方法，使用"跳过模式"：
     * 若当前点被标记为异常，则下一个点无条件保留（previous_outlier 逻辑），与 Python 原版一致。
     *
     * @param rriIntervals       RRI 数组（已经过第一次插值，无 NaN）
     * @param method             检测方法（malik/kamath/karlsson/acar/custom）
     * @param customRemovingRule 自定义阈值百分比，用于 custom/karlsson/acar 方法
     * @return 异常搏动替换为 NaN 的新数组
     */
    public static double[] removeEctopicBeats(double[] rriIntervals,
                                               String method, double customRemovingRule) {
        if (!MALIK_RULE.equals(method) && !KAMATH_RULE.equals(method)
                && !KARLSSON_RULE.equals(method) && !ACAR_RULE.equals(method)
                && !CUSTOM_RULE.equals(method)) {
            throw new IllegalArgumentException(
                "不支持的方法: " + method + "，可选: malik, kamath, karlsson, acar, custom");
        }

        if (KARLSSON_RULE.equals(method)) {
            return removeOutlierKarlsson(rriIntervals, customRemovingRule);
        }
        if (ACAR_RULE.equals(method)) {
            return removeOutlierAcar(rriIntervals, customRemovingRule);
        }

        // Malik / Kamath / Custom 共用路径
        int n = rriIntervals.length;
        double[] result = new double[n];
        result[0] = rriIntervals[0];

        boolean previousOutlier = false;
        for (int i = 0; i < n - 1; i++) {
            if (previousOutlier) {
                // 上一个是异常搏动，当前无条件保留（Python 原版逻辑）
                result[i + 1] = rriIntervals[i + 1];
                previousOutlier = false;
                continue;
            }
            if (isRriWithinBounds(rriIntervals[i], rriIntervals[i + 1], method, customRemovingRule)) {
                result[i + 1] = rriIntervals[i + 1];
            } else {
                result[i + 1] = Double.NaN;
                previousOutlier = true;
            }
        }
        return result;
    }

    /** 使用默认 customRemovingRule=0.2 */
    public static double[] removeEctopicBeats(double[] rriIntervals, String method) {
        return removeEctopicBeats(rriIntervals, method, 0.2);
    }

    /** 默认方法：malik，阈值：0.2 */
    public static double[] removeEctopicBeats(double[] rriIntervals) {
        return removeEctopicBeats(rriIntervals, MALIK_RULE, 0.2);
    }

    // ==================== 完整 Pipeline ====================

    /**
     * 完整 NN 间隔获取 Pipeline（4步清洗）。
     * 对应Python: get_nn_intervals(rr_intervals, low_rri=300, high_rri=2000,
     *                              ectopic_beats_removal_method="kamath")
     *
     * <p>执行顺序：
     * <ol>
     *   <li>removeOutliers(low, high) → 范围外值替换为 NaN</li>
     *   <li>interpolateNanValues() → 第一次线性插值</li>
     *   <li>removeEctopicBeats(method) → 异常搏动替换为 NaN</li>
     *   <li>interpolateNanValues() → 第二次线性插值</li>
     * </ol>
     *
     * @param rriIntervals              原始 RRI 数组（毫秒）
     * @param lowRri                    最小有效 RRI（默认 300）
     * @param highRri                   最大有效 RRI（默认 2000）
     * @param ectopicBeatsRemovalMethod 异常搏动检测方法（默认 "kamath"）
     * @param customRemovingRule        自定义阈值（customRule/karlsson/acar 时使用，默认 0.2）
     * @return 清洗并插值后的 NN 间隔数组
     */
    public static double[] getNnIntervals(double[] rriIntervals, int lowRri, int highRri,
                                           String ectopicBeatsRemovalMethod, double customRemovingRule) {
        // 步骤1：范围过滤
        double[] step1 = removeOutliers(rriIntervals, lowRri, highRri);
        // 步骤2：第一次插值
        double[] step2 = interpolateNanValues(step1);
        // 步骤3：异常搏动移除
        double[] step3 = removeEctopicBeats(step2, ectopicBeatsRemovalMethod, customRemovingRule);
        // 步骤4：第二次插值
        return interpolateNanValues(step3);
    }

    /** 默认参数：low=300, high=2000, method=kamath, rule=0.2 */
    public static double[] getNnIntervals(double[] rriIntervals) {
        return getNnIntervals(rriIntervals, 300, 2000, KAMATH_RULE, 0.2);
    }

    /** 指定方法，其余默认 */
    public static double[] getNnIntervals(double[] rriIntervals, String method) {
        return getNnIntervals(rriIntervals, 300, 2000, method, 0.2);
    }

    // ==================== 样本验证 ====================

    /**
     * 验证清洗后的 NN 间隔样本是否满足分析条件。
     * 对应Python: is_valid_sample(nn_intervals, outlier_count, removing_rule=0.04)
     *
     * <p>验证条件：
     * <ul>
     *   <li>异常比例：outlierCount / len(nnIntervals) &lt;= 0.04（≤4%）</li>
     *   <li>最小样本量：len(nnIntervals) &gt;= 240（满足 Nyquist 准则）</li>
     * </ul>
     *
     * @param nnIntervals  清洗后的 NN 间隔数组
     * @param outlierCount 被移除（替换为 NaN 后插值）的异常值总数
     * @param removingRule 最大允许异常比例（默认 0.04）
     * @return true 表示样本有效，可用于 HRV 分析
     */
    public static boolean isValidSample(double[] nnIntervals, int outlierCount, double removingRule) {
        if ((double) outlierCount / nnIntervals.length > removingRule) return false;
        if (nnIntervals.length < 240) return false;
        return true;
    }

    /** 默认 removingRule=0.04 */
    public static boolean isValidSample(double[] nnIntervals, int outlierCount) {
        return isValidSample(nnIntervals, outlierCount, 0.04);
    }

    /**
     * 统计数组中 NaN 的数量（用于计算 outlierCount）。
     *
     * @param arr 输入数组
     * @return NaN 的个数
     */
    public static int countNaN(double[] arr) {
        int count = 0;
        for (double v : arr) if (Double.isNaN(v)) count++;
        return count;
    }
}
