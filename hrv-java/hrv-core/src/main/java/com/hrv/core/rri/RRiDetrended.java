package com.hrv.core.rri;

/**
 * 去趋势RRi序列类。
 * 表示经过去趋势处理后的RRi序列，值可为负数（偏差量）。
 * 对应Python原项目中的 hrv/rri.py :: RRiDetrended 类。
 *
 * <p>与 {@link RRi} 的区别：
 * <ul>
 *   <li>不对值进行正数验证（去趋势后可能为负）</li>
 *   <li>detrended 标志始终为 true</li>
 *   <li>可选标记为已插值（interpolated）</li>
 * </ul>
 */
public class RRiDetrended extends RRi {

    /**
     * 构造去趋势RRi序列（未插值）。
     *
     * @param rri  去趋势后的RRi值（可含负数）
     * @param time 对应的时间数组（秒）
     */
    public RRiDetrended(double[] rri, double[] time) {
        super(rri, time, true, false);
    }

    /**
     * 构造去趋势RRi序列，指定是否已插值。
     *
     * @param rri          去趋势后的RRi值（可含负数）
     * @param time         对应的时间数组（秒）
     * @param interpolated 是否已插值为均匀间隔序列
     */
    public RRiDetrended(double[] rri, double[] time, boolean interpolated) {
        super(rri, time, true, interpolated);
    }

    @Override
    public String toString() {
        return "RRiDetrended(detrended=" + isDetrended() + ", interpolated=" + isInterpolated() + ")";
    }
}
