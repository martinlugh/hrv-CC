package com.hrv.core.rri;

import com.hrv.core.utils.RRiUtils;

import java.util.Arrays;

/**
 * RRi序列类。
 * 表示心率变异性分析中的RR间期（毫秒）序列，提供验证、统计描述、时间切割等功能。
 * 对应Python原项目中的 hrv/rri.py :: RRi 类。
 *
 * <p>验证规则：
 * <ul>
 *   <li>rri值必须全部为正数</li>
 *   <li>若中位数小于10，认为输入为秒，自动转换为毫秒</li>
 *   <li>时间数组若未提供，由cumsum(rri)/1000生成，从0开始</li>
 *   <li>时间数组若提供，须与rri等长、单调递增、无负数、除首位外无零值</li>
 * </ul>
 */
public class RRi {

    /** RRi序列值（毫秒） */
    private final double[] rri;

    /** 时间数组（秒） */
    private final double[] time;

    /** 是否已去趋势 */
    protected final boolean detrended;

    /** 是否已插值 */
    protected final boolean interpolated;

    /**
     * 构造RRi序列，自动生成时间数组。
     *
     * @param rri RRi值数组（毫秒或秒）
     */
    public RRi(double[] rri) {
        this(rri, null);
    }

    /**
     * 构造RRi序列，指定时间数组。
     *
     * @param rri  RRi值数组（毫秒或秒）
     * @param time 时间数组（秒），为null时自动生成
     */
    public RRi(double[] rri, double[] time) {
        this(rri, time, false, false);
    }

    /**
     * 完整构造函数，供子类使用。
     *
     * @param rri          RRi值数组
     * @param time         时间数组，为null时自动生成
     * @param detrended    是否已去趋势
     * @param interpolated 是否已插值
     */
    protected RRi(double[] rri, double[] time, boolean detrended, boolean interpolated) {
        if (!detrended) {
            // 非去趋势序列需要验证：正数 + 单位转换
            this.rri = validateRri(rri);
        } else {
            // 去趋势序列可以有负值（偏差值），直接存储
            this.rri = Arrays.copyOf(rri, rri.length);
        }
        this.detrended = detrended;
        this.interpolated = interpolated;
        if (time == null) {
            // 自动生成时间数组：cumsum(rri)/1000 - 第一个值
            this.time = RRiUtils.createTimeArray(this.rri);
        } else {
            this.time = validateTime(this.rri, time);
        }
    }

    /**
     * 验证RRi序列的合法性并转换单位。
     * 对应Python: _validate_rri
     *
     * @param rri 输入RRi序列
     * @return 毫秒单位的RRi序列
     */
    public static double[] validateRri(double[] rri) {
        // 检查是否有非正数值
        for (double v : rri) {
            if (v <= 0) {
                throw new IllegalArgumentException("rri series can only have positive values");
            }
        }
        // 若中位数小于10，认为是秒单位，转换为毫秒
        double med = RRiUtils.median(rri);
        if (med < 10.0) {
            double[] ms = new double[rri.length];
            for (int i = 0; i < rri.length; i++) ms[i] = rri[i] * 1000.0;
            return ms;
        }
        return Arrays.copyOf(rri, rri.length);
    }

    /**
     * 验证时间数组的合法性。
     * 对应Python: _validate_time
     *
     * @param rri  RRi序列（用于长度比较）
     * @param time 时间数组
     * @return 复制后的时间数组
     */
    public static double[] validateTime(double[] rri, double[] time) {
        if (rri.length != time.length) {
            throw new IllegalArgumentException("rri and time series must have the same length");
        }
        // 首位之后不能有0
        for (int i = 1; i < time.length; i++) {
            if (time[i] == 0.0) {
                throw new IllegalArgumentException("time series cannot have 0 values after first position");
            }
        }
        // 必须单调递增
        for (int i = 0; i < time.length - 1; i++) {
            if (time[i + 1] <= time[i]) {
                throw new IllegalArgumentException("time series must be monotonically increasing");
            }
        }
        // 不允许负值
        for (double t : time) {
            if (t < 0) {
                throw new IllegalArgumentException("time series cannot have negative values");
            }
        }
        return Arrays.copyOf(time, time.length);
    }

    /** @return RRi序列值（毫秒） */
    public double[] getRri() { return rri.clone(); }

    /** @return RRi序列值（毫秒），别名 */
    public double[] getValues() { return rri.clone(); }

    /** @return 时间数组（秒） */
    public double[] getTime() { return time.clone(); }

    /** @return 是否已去趋势 */
    public boolean isDetrended() { return detrended; }

    /** @return 是否已插值 */
    public boolean isInterpolated() { return interpolated; }

    /** @return 序列长度 */
    public int size() { return rri.length; }

    /**
     * 计算RRi均值（对应Python: mean()）。
     * @return 均值（毫秒）
     */
    public double mean() { return RRiUtils.mean(rri); }

    /**
     * 计算RRi总体方差（对应Python: var()，即numpy.var ddof=0）。
     * @return 总体方差
     */
    public double var() { return RRiUtils.variance(rri); }

    /**
     * 计算RRi总体标准差（对应Python: std()，即numpy.std ddof=0）。
     * @return 总体标准差
     */
    public double std() { return Math.sqrt(var()); }

    /**
     * 计算RRi中位数（对应Python: median()）。
     * @return 中位数（毫秒）
     */
    public double median() { return RRiUtils.median(rri); }

    /**
     * 计算RRi最大值（对应Python: max()）。
     * @return 最大值（毫秒）
     */
    public double max() { return RRiUtils.max(rri); }

    /**
     * 计算RRi最小值（对应Python: min()）。
     * @return 最小值（毫秒）
     */
    public double min() { return RRiUtils.min(rri); }

    /**
     * 计算RRi振幅（对应Python: amplitude() = max - min）。
     * @return 振幅（毫秒）
     */
    public double amplitude() { return max() - min(); }

    /**
     * 计算RRi均方根（对应Python: rms()）。
     * @return 均方根（毫秒）
     */
    public double rms() { return RRiUtils.rms(rri); }

    /**
     * 将RRi转换为心率（bpm）数组。
     * HR = 60 / (rri / 1000)
     * 对应Python: to_hr()
     *
     * @return 心率数组（bpm）
     */
    public double[] toHr() { return RRiUtils.toHeartRate(rri); }

    /**
     * 根据时间区间截取RRi序列。
     * 对应Python: time_range(start, end)
     *
     * @param start 起始时间（秒，含）
     * @param end   终止时间（秒，含）
     * @return 截取后的RRi序列
     */
    public RRi timeRange(double start, double end) {
        int count = 0;
        for (double t : time) {
            if (t >= start && t <= end) count++;
        }
        double[] newRri = new double[count];
        double[] newTime = new double[count];
        int idx = 0;
        for (int i = 0; i < time.length; i++) {
            if (time[i] >= start && time[i] <= end) {
                newRri[idx] = rri[i];
                newTime[idx] = time[i];
                idx++;
            }
        }
        return new RRi(newRri, newTime);
    }

    /**
     * 将时间数组从第一个点重置为0。
     * 对应Python: reset_time(inplace=False)
     *
     * @return 时间重置后的新RRi序列
     */
    public RRi resetTime() {
        double[] newTime = time.clone();
        double first = newTime[0];
        for (int i = 0; i < newTime.length; i++) newTime[i] -= first;
        return new RRi(rri.clone(), newTime);
    }

    /**
     * 按时间窗口切割RRi序列为多段。
     * 对应Python: time_split(seg_size, overlap, keep_last)
     *
     * @param segSize  每段时长（秒）
     * @param overlap  相邻段之间的重叠时长（秒），默认为0
     * @param keepLast 是否保留末尾不足一段的部分
     * @return 切割后的RRi段列表
     */
    public java.util.List<RRi> timeSplit(double segSize, double overlap, boolean keepLast) {
        double rriDuration = time[time.length - 1];
        if (overlap > segSize) {
            throw new IllegalArgumentException("`overlap` can not be bigger than `seg_size`");
        }
        if (segSize > rriDuration) {
            throw new IllegalArgumentException("`seg_size` is longer than RRi duration.");
        }

        java.util.List<RRi> segments = new java.util.ArrayList<>();
        double step = segSize - overlap;
        int nSplits = (int) ((rriDuration - segSize) / step) + 1;

        double begin = 0;
        double end = segSize;
        for (int i = 0; i < nSplits; i++) {
            boolean isLast = (i + 1 == nSplits);
            java.util.List<Double> segRri = new java.util.ArrayList<>();
            java.util.List<Double> segTime = new java.util.ArrayList<>();
            for (int j = 0; j < time.length; j++) {
                // 最后一段包含end点（<=），其他段不包含（<）
                boolean inSeg = isLast
                    ? (time[j] >= begin && time[j] <= end)
                    : (time[j] >= begin && time[j] < end);
                if (inSeg) {
                    segRri.add(rri[j]);
                    segTime.add(time[j]);
                }
            }
            if (!segRri.isEmpty()) {
                segments.add(new RRi(toArray(segRri), toArray(segTime)));
            }
            begin += step;
            end += step;
        }

        // 保留最后不足一段的剩余部分
        if (keepLast) {
            RRi last = segments.get(segments.size() - 1);
            double lastTime = last.time[last.time.length - 1];
            if (lastTime < rriDuration) {
                double finalBegin = begin;
                java.util.List<Double> tailRri = new java.util.ArrayList<>();
                java.util.List<Double> tailTime = new java.util.ArrayList<>();
                for (int j = 0; j < time.length; j++) {
                    if (time[j] > finalBegin) {
                        tailRri.add(rri[j]);
                        tailTime.add(time[j]);
                    }
                }
                if (!tailRri.isEmpty()) {
                    segments.add(new RRi(toArray(tailRri), toArray(tailTime)));
                }
            }
        }
        return segments;
    }

    private static double[] toArray(java.util.List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    /**
     * 获取指定索引处的RRi值。
     *
     * @param index 索引
     * @return 该位置的RRi值（毫秒）
     */
    public double get(int index) { return rri[index]; }

    /**
     * 获取切片后的新RRi序列（闭区间[from, to)）。
     *
     * @param from 起始索引（含）
     * @param to   终止索引（不含）
     * @return 切片后的RRi序列
     */
    public RRi slice(int from, int to) {
        double[] newRri = Arrays.copyOfRange(rri, from, to);
        double[] newTime = Arrays.copyOfRange(time, from, to);
        return new RRi(newRri, newTime);
    }

    /**
     * 生成RRi描述统计信息。
     * 对应Python: describe()
     *
     * @return 包含各项统计指标的描述对象
     */
    public RRiDescription describe() {
        return new RRiDescription(this);
    }

    @Override
    public String toString() {
        return "RRi " + Arrays.toString(rri);
    }
}
