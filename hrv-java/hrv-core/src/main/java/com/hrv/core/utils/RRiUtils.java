package com.hrv.core.utils;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * RRi工具类。
 * 提供RRi序列的通用辅助函数，对应Python原项目中的 hrv/utils.py。
 * <p>
 * 包括：时间数组生成、单位转换、插值等功能。
 */
public final class RRiUtils {

    private RRiUtils() {}

    /**
     * 将RRi序列转换为毫秒单位。
     * 若中位数小于10，认为输入为秒，乘以1000转换为毫秒。
     * 对应Python: _transform_rri_to_miliseconds
     *
     * @param rri 输入RRi序列（毫秒或秒）
     * @return 毫秒单位的RRi序列（新数组，不修改原始数据）
     */
    public static double[] transformRriToMilliseconds(double[] rri) {
        double median = median(rri);
        if (median < 10.0) {
            // 中位数小于10，推断为秒单位，转换为毫秒
            double[] result = new double[rri.length];
            for (int i = 0; i < rri.length; i++) {
                result[i] = rri[i] * 1000.0;
            }
            return result;
        }
        // 已经是毫秒，直接复制
        return rri.clone();
    }

    /**
     * 根据RRi序列生成时间数组。
     * 时间 = cumsum(rri) / 1000（转换为秒），并从0开始。
     * 对应Python: _create_time_info / _create_time_array
     *
     * @param rri RRi序列（毫秒）
     * @return 时间数组（秒），从0开始单调递增
     */
    public static double[] createTimeArray(double[] rri) {
        double[] time = new double[rri.length];
        double cumsum = 0.0;
        for (int i = 0; i < rri.length; i++) {
            cumsum += rri[i];
            time[i] = cumsum / 1000.0; // 毫秒转秒
        }
        // 使时间从0开始：减去第一个值
        double first = time[0];
        for (int i = 0; i < time.length; i++) {
            time[i] -= first;
        }
        return time;
    }

    /**
     * 生成均匀采样时间网格，用于插值后的频域分析。
     * 对应Python: _create_interp_time
     * 时间分辨率 = 1/fs，从0到time[-1]+1/fs（不含终点）
     *
     * @param time 原始时间数组（秒）
     * @param fs   采样频率（Hz）
     * @return 均匀时间网格
     */
    public static double[] createInterpTime(double[] time, double fs) {
        double timeResolution = 1.0 / fs;
        double endTime = time[time.length - 1] + timeResolution;
        int n = (int) Math.ceil(endTime / timeResolution);
        double[] interpTime = new double[n];
        for (int i = 0; i < n; i++) {
            interpTime[i] = i * timeResolution;
        }
        return interpTime;
    }

    /**
     * 三次样条插值。
     * 对应Python: _interp_cubic_spline（使用scipy.interpolate.splrep/splev）
     * 注意：Apache Commons Math使用自然三次样条，与scipy的B样条边界条件略有差异，
     * 频域计算结果在数值上接近但非完全一致。
     *
     * @param rri  RRi序列（毫秒）
     * @param time 对应时间数组（秒）
     * @param fs   目标采样频率（Hz）
     * @return 在均匀时间网格上插值后的RRi序列
     */
    public static double[] interpCubicSpline(double[] rri, double[] time, double fs) {
        double[] interpTime = createInterpTime(time, fs);
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction spline = interpolator.interpolate(time, rri);
        double[] result = new double[interpTime.length];
        double maxT = time[time.length - 1];
        for (int i = 0; i < interpTime.length; i++) {
            // 超出范围的时间点使用边界值（外推）
            double t = Math.min(interpTime[i], maxT);
            result[i] = spline.value(t);
        }
        return result;
    }

    /**
     * 线性插值。
     * 对应Python: _interp_linear（使用np.interp）
     *
     * @param rri  RRi序列（毫秒）
     * @param time 对应时间数组（秒）
     * @param fs   目标采样频率（Hz）
     * @return 在均匀时间网格上线性插值后的RRi序列
     */
    public static double[] interpLinear(double[] rri, double[] time, double fs) {
        double[] interpTime = createInterpTime(time, fs);
        LinearInterpolator interpolator = new LinearInterpolator();
        PolynomialSplineFunction spline = interpolator.interpolate(time, rri);
        double[] result = new double[interpTime.length];
        double maxT = time[time.length - 1];
        double minT = time[0];
        for (int i = 0; i < interpTime.length; i++) {
            double t = Math.max(minT, Math.min(interpTime[i], maxT));
            result[i] = spline.value(t);
        }
        return result;
    }

    /**
     * 根据插值方法名执行RRi插值。
     * 对应Python: _interpolate_rri
     *
     * @param rri          RRi序列
     * @param time         时间数组
     * @param fs           采样频率
     * @param interpMethod 插值方法："cubic"或"linear"
     * @return 插值后的RRi序列
     */
    public static double[] interpolateRri(double[] rri, double[] time, double fs, String interpMethod) {
        switch (interpMethod) {
            case "cubic":
                return interpCubicSpline(rri, time, fs);
            case "linear":
                return interpLinear(rri, time, fs);
            default:
                throw new IllegalArgumentException("不支持的插值方法: " + interpMethod + "，请选择 'cubic' 或 'linear'");
        }
    }

    /**
     * 计算数组的中位数。
     *
     * @param arr 输入数组
     * @return 中位数
     */
    public static double median(double[] arr) {
        if (arr.length == 0) {
            throw new IllegalArgumentException("输入数组不能为空");
        }
        double[] sorted = arr.clone();
        java.util.Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 0) {
            return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
        } else {
            return sorted[n / 2];
        }
    }

    /**
     * 计算数组的算术均值。
     *
     * @param arr 输入数组
     * @return 均值
     */
    public static double mean(double[] arr) {
        double sum = 0.0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    /**
     * 计算数组的方差（总体方差，ddof=0）。
     *
     * @param arr 输入数组
     * @return 总体方差
     */
    public static double variance(double[] arr) {
        double m = mean(arr);
        double sum = 0.0;
        for (double v : arr) sum += (v - m) * (v - m);
        return sum / arr.length;
    }

    /**
     * 计算数组的样本标准差（ddof=1，对应numpy.std(ddof=1)）。
     *
     * @param arr 输入数组
     * @return 样本标准差
     */
    public static double stdDdof1(double[] arr) {
        double m = mean(arr);
        double sum = 0.0;
        for (double v : arr) sum += (v - m) * (v - m);
        return Math.sqrt(sum / (arr.length - 1));
    }

    /**
     * 计算相邻元素之差（对应numpy.diff）。
     *
     * @param arr 输入数组
     * @return 差分数组，长度为arr.length-1
     */
    public static double[] diff(double[] arr) {
        double[] result = new double[arr.length - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = arr[i + 1] - arr[i];
        }
        return result;
    }

    /**
     * 计算数组最大值。
     */
    public static double max(double[] arr) {
        double m = arr[0];
        for (double v : arr) if (v > m) m = v;
        return m;
    }

    /**
     * 计算数组最小值。
     */
    public static double min(double[] arr) {
        double m = arr[0];
        for (double v : arr) if (v < m) m = v;
        return m;
    }

    /**
     * 计算数组均方根（RMS）。
     */
    public static double rms(double[] arr) {
        double sum = 0.0;
        for (double v : arr) sum += v * v;
        return Math.sqrt(sum / arr.length);
    }

    /**
     * 梯形积分（对应numpy.trapz）。
     * AUC = sum((x[i+1]-x[i]) * (y[i]+y[i+1]) / 2)
     *
     * @param y 函数值数组
     * @param x 对应自变量数组
     * @return 梯形法积分结果
     */
    public static double trapz(double[] y, double[] x) {
        double area = 0.0;
        for (int i = 0; i < x.length - 1; i++) {
            area += (x[i + 1] - x[i]) * (y[i] + y[i + 1]) / 2.0;
        }
        return area;
    }

    /**
     * 验证RRi序列合法性（仅包含正数）并转换为毫秒单位。
     * 对应Python中的validate_rri装饰器和_validate_rri函数。
     *
     * @param rri 输入RRi序列
     * @return 转换后的毫秒单位RRi序列
     * @throws IllegalArgumentException 若包含非正数值
     */
    public static double[] validateAndConvertRri(double[] rri) {
        for (double v : rri) {
            if (v <= 0) {
                throw new IllegalArgumentException(
                    "rri must be a list or array of positive and non-zero numbers"
                );
            }
        }
        return transformRriToMilliseconds(rri);
    }

    /**
     * 根据RRi序列计算心率（bpm）数组。
     * HR = 60 / (rri / 1000)
     *
     * @param rri RRi序列（毫秒）
     * @return 心率数组（bpm）
     */
    public static double[] toHeartRate(double[] rri) {
        double[] hr = new double[rri.length];
        for (int i = 0; i < rri.length; i++) {
            hr[i] = 60.0 / (rri[i] / 1000.0);
        }
        return hr;
    }

    /**
     * 三次样条插值（使用Commons Math，用于detrend模块）。
     * 返回指定时间点上的插值结果。
     *
     * @param srcTime   原始时间数组
     * @param srcValues 原始值数组
     * @param dstTime   目标时间数组
     * @return 目标时间上的插值结果
     */
    public static double[] cubicSplineEval(double[] srcTime, double[] srcValues, double[] dstTime) {
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction spline = interpolator.interpolate(srcTime, srcValues);
        double maxT = srcTime[srcTime.length - 1];
        double minT = srcTime[0];
        double[] result = new double[dstTime.length];
        for (int i = 0; i < dstTime.length; i++) {
            double t = Math.max(minT, Math.min(dstTime[i], maxT));
            result[i] = spline.value(t);
        }
        return result;
    }
}
