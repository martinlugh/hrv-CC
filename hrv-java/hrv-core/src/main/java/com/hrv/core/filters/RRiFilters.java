package com.hrv.core.filters;

import com.hrv.core.rri.RRi;
import com.hrv.core.utils.RRiUtils;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RRi序列滤波工具类。
 * 提供4种滤波/异常值处理方法，对应Python原项目中的 hrv/filters.py。
 *
 * <p>所有方法均保持原始时间信息，返回新的 {@link RRi} 对象。
 * 不修改原始输入数据。
 *
 * <p>方法列表：
 * <ul>
 *   <li>{@link #quotient(RRi)}：商值法，去除连续差值超过20%的ectopic心拍</li>
 *   <li>{@link #movingAverage(RRi, int)}：移动平均低通滤波</li>
 *   <li>{@link #movingMedian(RRi, int)}：移动中位数低通滤波</li>
 *   <li>{@link #thresholdFilter(RRi, double, int)}：阈值法+三次样条插值</li>
 * </ul>
 */
public class RRiFilters {

    /**
     * 商值法滤波（Quotient Filter）。
     * 去除连续RRi值之间比例超过20%的异常点（Piskorski & Guzik方法）。
     * 对应Python: quotient(rri)
     *
     * <p>算法：
     * 对于 i 从 0 到 L-3（L = len-1）：
     *   若 rri[i]/rri[i+1] < 0.8 或 > 1.2
     *   或 rri[i+1]/rri[i] < 0.8 或 > 1.2
     *   则将下标 i 标记为待删除
     * 删除标记的点后返回剩余序列。
     *
     * <p>参考文献：
     * Piskorski J, Guzik P. Filtering poincare plots. Comput Methods Sci Technol. 2005;11(1):39–48.
     *
     * @param rri 输入RRi序列
     * @return 过滤后的RRi序列
     */
    public static RRi quotient(RRi rri) {
        double[] rriVals = rri.getRri();
        double[] rriTime = rri.getTime();
        return quotient(rriVals, rriTime);
    }

    /**
     * 商值法滤波（接受原始数组）。
     *
     * @param rri  RRi值数组（毫秒）
     * @param time 时间数组（秒）
     * @return 过滤后的RRi序列
     */
    public static RRi quotient(double[] rri, double[] time) {
        int n = rri.length;
        int L = n - 1; // Python原代码中的L = len(rri) - 1

        // 找出需要删除的下标：比较 rri[0..L-2] 与 rri[1..L-1] 的比例
        // Python代码：rri[:L-1] / rri[1:L]  →  rri[0..L-2] / rri[1..L-1]
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < L - 1; i++) {
            double a = rri[i];
            double b = rri[i + 1];
            // 若任一方向的比例超出 [0.8, 1.2]，则标记为异常
            if (a / b < 0.8 || a / b > 1.2 || b / a < 0.8 || b / a > 1.2) {
                toRemove.add(i);
            }
        }

        // 构建过滤后的数组
        List<Double> filtRri = new ArrayList<>();
        List<Double> filtTime = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!toRemove.contains(i)) {
                filtRri.add(rri[i]);
                filtTime.add(time[i]);
            }
        }

        return new RRi(toDoubleArray(filtRri), toDoubleArray(filtTime));
    }

    /**
     * 商值法滤波（接受RRi数组，自动生成时间）。
     *
     * @param rri RRi值数组（毫秒）
     * @return 过滤后的RRi序列
     */
    public static RRi quotient(double[] rri) {
        double[] time = RRiUtils.createTimeArray(rri);
        return quotient(rri, time);
    }

    /**
     * 移动平均滤波（Moving Average Filter）。
     * 将每个RRi值替换为其邻域（±order/2个点）的均值。
     * 首尾各 ⌊order/2⌋ 个值不作处理（保持原值）。
     * 对应Python: moving_average(rri, order=3)
     *
     * <p>公式（以order=3为例）：
     * <pre>
     *   rri[j] = mean(rri[j-1], rri[j], rri[j+1])  （j=1 到 n-2）
     * </pre>
     *
     * @param rri   输入RRi序列
     * @param order 滤波阶数（邻域大小），默认3
     * @return 滤波后的RRi序列
     */
    public static RRi movingAverage(RRi rri, int order) {
        return movingFunction(rri, order, "mean");
    }

    /** 移动平均，使用默认阶数3 */
    public static RRi movingAverage(RRi rri) {
        return movingAverage(rri, 3);
    }

    /**
     * 移动中位数滤波（Moving Median Filter）。
     * 将每个RRi值替换为其邻域（±order/2个点）的中位数。
     * 首尾各 ⌊order/2⌋ 个值不作处理（保持原值）。
     * 对应Python: moving_median(rri, order=3)
     *
     * @param rri   输入RRi序列
     * @param order 滤波阶数，默认3
     * @return 滤波后的RRi序列
     */
    public static RRi movingMedian(RRi rri, int order) {
        return movingFunction(rri, order, "median");
    }

    /** 移动中位数，使用默认阶数3 */
    public static RRi movingMedian(RRi rri) {
        return movingMedian(rri, 3);
    }

    /**
     * 移动平均/中位数的内部实现。
     * 对应Python: _moving_function(rri, order, func)
     *
     * @param rri   输入RRi序列
     * @param order 阶数
     * @param func  函数类型："mean"或"median"
     * @return 滤波后的RRi序列
     */
    private static RRi movingFunction(RRi rri, int order, String func) {
        double[] rriVals = rri.getRri();
        double[] rriTime = rri.getTime();
        int n = rriVals.length;

        // offset = int(order / 2)，确定不处理的边界宽度
        int offset = order / 2;

        // 复制原始数组（边界值不修改）
        double[] filtRri = Arrays.copyOf(rriVals, n);

        // 对中间部分（index: offset 到 n-offset-1）应用函数
        for (int i = offset; i < n - offset; i++) {
            // 邻域：[i-offset, i+offset]（含两端）
            double[] window = Arrays.copyOfRange(rriVals, i - offset, i + offset + 1);
            filtRri[i] = "mean".equals(func) ? RRiUtils.mean(window) : RRiUtils.median(window);
        }

        return new RRi(filtRri, rriTime);
    }

    /**
     * 阈值法滤波（Threshold Filter）。
     * 参考Kubios® HRV分析软件的阈值伪迹校正算法。
     * 将异常RRi（与局部中位数偏差超过阈值的点）标记后通过三次样条插值替换。
     * 对应Python: threshold_filter(rri, threshold="medium", local_median_size=5)
     *
     * <p>预设阈值强度：
     * <ul>
     *   <li>"very low": 450ms</li>
     *   <li>"low": 350ms</li>
     *   <li>"medium": 250ms（默认）</li>
     *   <li>"strong": 150ms</li>
     *   <li>"very strong": 50ms</li>
     * </ul>
     *
     * <p>算法步骤：
     * <ol>
     *   <li>对于 j >= local_median_size：若 rri[j] > median(rri[j-N:j]) + threshold，标记为异常</li>
     *   <li>对于前 local_median_size 个点：若与其余点中位数偏差 > threshold，标记为异常</li>
     *   <li>删除异常点，用三次样条在原始时间点上插值填充</li>
     * </ol>
     *
     * @param rri              输入RRi序列
     * @param threshold        阈值（ms）或阈值预设名称字符串
     * @param localMedianSize  局部中位数窗口大小（默认5）
     * @return 滤波并插值后的RRi序列
     */
    public static RRi thresholdFilter(RRi rri, double threshold, int localMedianSize) {
        double[] rriVals = rri.getRri();
        double[] rriTime = rri.getTime();
        int n = rriVals.length;

        List<Integer> rriToRemove = new ArrayList<>();

        // 主循环：对下标 local_median_size 到 n-1 进行检查
        for (int j = localMedianSize; j < n; j++) {
            // 局部中位数：rri[j-local_median_size : j]
            double[] localWin = Arrays.copyOfRange(rriVals, j - localMedianSize, j);
            double localMedian = RRiUtils.median(localWin);
            // 若当前值超过局部中位数 + 阈值，标记为异常
            if (rriVals[j] > localMedian + threshold) {
                rriToRemove.add(j);
            }
        }

        // 处理前 local_median_size 个点
        // 对每个 j in [0, local_median_size)，比较其与其余前几个点中位数的差
        List<Integer> firstIdx = new ArrayList<>();
        for (int i = 0; i <= localMedianSize; i++) firstIdx.add(i);

        for (int j = 0; j < localMedianSize; j++) {
            // 去掉 j 本身，用其他点计算中位数
            List<Double> otherVals = new ArrayList<>();
            for (int idx : firstIdx) {
                if (idx != j && idx < n) otherVals.add(rriVals[idx]);
            }
            if (!otherVals.isEmpty()) {
                double localMedian = RRiUtils.median(toDoubleArray(otherVals));
                if (Math.abs(rriVals[j] - localMedian) > threshold) {
                    rriToRemove.add(j);
                }
            }
        }

        // 删除异常点，保留正常点的值和时间
        List<Double> rriTemp = new ArrayList<>();
        List<Double> timeTemp = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!rriToRemove.contains(i)) {
                rriTemp.add(rriVals[i]);
                timeTemp.add(rriTime[i]);
            }
        }

        // 三次样条插值：在原始时间点上重建被删除的点
        double[] rriClean = toDoubleArray(rriTemp);
        double[] timeClean = toDoubleArray(timeTemp);

        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction spline = interpolator.interpolate(timeClean, rriClean);

        // 在原始时间网格上求值（超出范围的用边界值）
        double[] rriInterp = new double[n];
        double minT = timeClean[0];
        double maxT = timeClean[timeClean.length - 1];
        for (int i = 0; i < n; i++) {
            double t = Math.max(minT, Math.min(rriTime[i], maxT));
            rriInterp[i] = spline.value(t);
        }

        return new RRi(rriInterp, rriTime);
    }

    /**
     * 阈值法滤波（使用字符串预设阈值名称）。
     *
     * @param rri              输入RRi序列
     * @param thresholdName    阈值预设名称："very low"/"low"/"medium"/"strong"/"very strong"
     * @param localMedianSize  局部中位数窗口大小
     * @return 滤波并插值后的RRi序列
     */
    public static RRi thresholdFilter(RRi rri, String thresholdName, int localMedianSize) {
        double threshold = parseThreshold(thresholdName);
        return thresholdFilter(rri, threshold, localMedianSize);
    }

    /** 默认参数：threshold="medium", localMedianSize=5 */
    public static RRi thresholdFilter(RRi rri) {
        return thresholdFilter(rri, 250.0, 5);
    }

    /**
     * 将阈值预设名称转换为毫秒值。
     * 对应Python: strength = {"very low": 450, "low": 350, "medium": 250, "strong": 150, "very strong": 50}
     */
    public static double parseThreshold(String name) {
        switch (name.toLowerCase()) {
            case "very low":   return 450.0;
            case "low":        return 350.0;
            case "medium":     return 250.0;
            case "strong":     return 150.0;
            case "very strong": return 50.0;
            default:
                throw new IllegalArgumentException("未知阈值预设: " + name
                    + "，可选: very low, low, medium, strong, very strong");
        }
    }

    /** 将 List<Double> 转为 double[] */
    private static double[] toDoubleArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
