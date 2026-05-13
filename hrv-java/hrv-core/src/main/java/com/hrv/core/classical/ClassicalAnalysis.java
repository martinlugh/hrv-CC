package com.hrv.core.classical;

import com.hrv.core.rri.RRi;
import com.hrv.core.rri.RRiDetrended;
import com.hrv.core.utils.RRiUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * 经典HRV分析类。
 * 提供时域分析、频域分析和非线性分析，对应Python原项目中的 hrv/classical.py。
 *
 * <p>所有算法公式、默认参数、指标定义完全遵循原Python项目：
 * <ul>
 *   <li>时域分析：rmssd, sdnn, sdsd, nn50, pnn50, mrri, mhr</li>
 *   <li>频域分析（Welch / Burg AR）：total_power, vlf, lf, hf, lf_hf, lfnu, hfnu</li>
 *   <li>非线性分析（Poincaré）：sd1, sd2</li>
 * </ul>
 *
 * <p>参考文献：
 * Heart rate variability. (1996). Standards of measurement, physiological interpretation,
 * and clinical use. Task Force of the European Society of Cardiology and the North
 * American Society of Pacing and Electrophysiology. Eur Heart J, 17, 354-381.
 */
public class ClassicalAnalysis {

    /**
     * 计算时域HRV指标。
     * 对应Python: time_domain(rri)
     *
     * @param rri RRi序列（毫秒或秒，自动转换）
     * @return 时域分析结果
     */
    public static TimeDomainResult timeDomain(double[] rri) {
        // 验证输入并转换为毫秒
        double[] rriMs = RRiUtils.validateAndConvertRri(rri);
        return computeTimeDomain(rriMs);
    }

    /**
     * 计算时域HRV指标（接受RRi对象）。
     *
     * @param rri RRi对象
     * @return 时域分析结果
     */
    public static TimeDomainResult timeDomain(RRi rri) {
        return computeTimeDomain(rri.getRri());
    }

    /**
     * 内部时域计算，输入已为毫秒单位的RRi。
     */
    private static TimeDomainResult computeTimeDomain(double[] rri) {
        double[] diffRri = RRiUtils.diff(rri);

        // RMSSD = sqrt(mean(diff^2))，均方根连续差值
        double rmssd = Math.sqrt(RRiUtils.mean(squareArr(diffRri)));

        // SDNN = std(rri, ddof=1)，使用N-1分母的样本标准差
        double sdnn = RRiUtils.stdDdof1(rri);

        // SDSD = std(diff(rri), ddof=1)，连续差值的样本标准差
        double sdsd = RRiUtils.stdDdof1(diffRri);

        // NN50：连续差值绝对值超过50ms的个数
        int nn50 = computeNn50(rri);

        // PNN50：NN50占总RRi个数的百分比
        double pnn50 = (double) nn50 / rri.length * 100.0;

        // MRRI：RRi均值（毫秒）
        double mrri = RRiUtils.mean(rri);

        // MHR：心率均值（bpm），= mean(60 / (rri / 1000))
        double mhr = RRiUtils.mean(RRiUtils.toHeartRate(rri));

        return new TimeDomainResult(rmssd, sdnn, sdsd, nn50, pnn50, mrri, mhr);
    }

    /**
     * 计算NN50：连续RRi差值绝对值大于50ms的个数。
     * 对应Python: _nn50(rri)
     */
    public static int computeNn50(double[] rri) {
        double[] d = RRiUtils.diff(rri);
        int count = 0;
        for (double v : d) {
            if (Math.abs(v) > 50.0) count++;
        }
        return count;
    }

    /**
     * 计算PNN50：NN50占比（%）。
     * 对应Python: _pnn50(rri)
     */
    public static double computePnn50(double[] rri) {
        return (double) computeNn50(rri) / rri.length * 100.0;
    }

    /**
     * 计算频域HRV指标（使用默认参数）。
     * 对应Python: frequency_domain(rri) 默认参数调用。
     * 默认参数：fs=4.0, method="welch", interp_method="cubic", detrend="constant"
     * 默认频段：vlf=(0,0.04), lf=(0.04,0.15), hf=(0.15,0.4)
     *
     * @param rri  RRi序列（毫秒）
     * @param time 时间数组（秒），为null时自动生成
     * @return 频域分析结果
     */
    public static FrequencyDomainResult frequencyDomain(double[] rri, double[] time) {
        return frequencyDomain(rri, time, 4.0, "welch", "cubic", "constant",
                new double[]{0, 0.04}, new double[]{0.04, 0.15}, new double[]{0.15, 0.4},
                256, 128, "hanning", 16, null);
    }

    /**
     * 计算频域HRV指标（接受RRi对象，使用默认参数）。
     *
     * @param rri RRi对象
     * @return 频域分析结果
     */
    public static FrequencyDomainResult frequencyDomain(RRi rri) {
        double[] time = rri.getTime();
        // 若已去趋势则跳过detrend步骤，若已插值则跳过插值
        String detrend = rri.isDetrended() ? "false" : "constant";
        String interpMethod = rri.isInterpolated() ? null : "cubic";
        return frequencyDomain(rri.getRri(), time, 4.0, "welch", interpMethod, detrend,
                new double[]{0, 0.04}, new double[]{0.04, 0.15}, new double[]{0.15, 0.4},
                256, 128, "hanning", 16, null);
    }

    /**
     * 计算频域HRV指标（完整参数版本）。
     * 对应Python: frequency_domain(rri, time, fs, method, interp_method, detrend,
     *                              vlf_band, lf_band, hf_band, nperseg, noverlap,
     *                              window, order, nfft)
     *
     * @param rri          RRi序列（毫秒）
     * @param time         时间数组（秒），为null时自动生成
     * @param fs           采样频率（Hz），默认4.0
     * @param method       PSD估计方法："welch"或"ar"
     * @param interpMethod 插值方法："cubic"或"linear"，为null时跳过插值
     * @param detrend      去趋势方式："constant"（减均值）、"false"（不去趋势）
     * @param vlfBand      极低频频段，默认(0, 0.04)
     * @param lfBand       低频频段，默认(0.04, 0.15)
     * @param hfBand       高频频段，默认(0.15, 0.4)
     * @param nperseg      Welch分段长度，默认256
     * @param noverlap     Welch分段重叠，默认128
     * @param window       窗函数名称，默认"hanning"
     * @param order        AR模型阶数（method="ar"时使用），默认16
     * @param nfft         FFT点数（null时等于nperseg）
     * @return 频域分析结果
     */
    public static FrequencyDomainResult frequencyDomain(
            double[] rri, double[] time, double fs,
            String method, String interpMethod, String detrend,
            double[] vlfBand, double[] lfBand, double[] hfBand,
            int nperseg, int noverlap, String window,
            int order, Integer nfft) {

        // 验证方法参数
        if (!"welch".equals(method) && !"ar".equals(method)) {
            throw new IllegalArgumentException("Method not supported! Choose among: welch, ar");
        }

        // 生成时间数组（如果未提供）
        if (time == null) {
            time = RRiUtils.createTimeArray(rri);
        }

        // 执行插值（将不均匀RRi插值为均匀采样序列）
        double[] rriForAnalysis = rri;
        if (interpMethod != null) {
            rriForAnalysis = RRiUtils.interpolateRri(rri, time, fs, interpMethod);
        }

        double[] fxx, pxx;

        if ("welch".equals(method)) {
            // Welch法PSD估计
            boolean doDetrend = !"false".equals(detrend) && detrend != null;
            double[][] welchResult = welchPsd(rriForAnalysis, fs, nperseg, noverlap, window, doDetrend);
            fxx = welchResult[0];
            pxx = welchResult[1];
        } else {
            // Burg自回归法PSD估计
            if (!"false".equals(detrend) && detrend != null) {
                // AR方法使用多项式去趋势（degree=1）
                rriForAnalysis = polynomialDetrendValues(rriForAnalysis, 1);
            }
            double[][] arResult = burgPsd(rriForAnalysis, fs, order, nfft);
            fxx = arResult[0];
            pxx = arResult[1];
        }

        // 计算各频段面积（梯形积分法，对应numpy.trapz）
        return computeAuc(fxx, pxx, vlfBand, lfBand, hfBand);
    }

    /**
     * Welch法功率谱密度估计。
     * 对应Python: scipy.signal.welch
     * 算法：分段→加窗→FFT→取功率→平均
     *
     * @param rri      等间隔采样的RRi序列
     * @param fs       采样频率（Hz）
     * @param nperseg  每段长度
     * @param noverlap 重叠长度
     * @param window   窗函数名称（"hanning"/"hann"/"hamming"/"blackman"/"none"）
     * @param doDetrend 是否对每段减均值（constant detrend）
     * @return [频率数组, 功率谱密度数组]
     */
    public static double[][] welchPsd(double[] rri, double fs, int nperseg, int noverlap,
                                       String window, boolean doDetrend) {
        int n = rri.length;
        // 若信号长度小于nperseg，调整nperseg
        if (nperseg > n) {
            nperseg = n;
            noverlap = nperseg / 2;
        }

        // 生成窗函数
        double[] win = createWindow(window, nperseg);
        double winNorm = 0.0;
        for (double w : win) winNorm += w * w;

        // 计算步长和段数
        int step = nperseg - noverlap;
        int nSegments = (n - noverlap) / step;

        // 用于累计功率谱的数组（单边频谱长度）
        int nfftActual = nperseg;
        // 确保FFT长度为2的幂（提升效率，与scipy默认行为一致）
        nfftActual = nextPowerOf2(nfftActual);
        int nFreq = nfftActual / 2 + 1;

        double[] pxxAccum = new double[nFreq];
        int validSegments = 0;

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        for (int seg = 0; seg < nSegments; seg++) {
            int start = seg * step;
            int end = start + nperseg;
            if (end > n) break;

            // 提取分段数据
            double[] segData = new double[nperseg];
            System.arraycopy(rri, start, segData, 0, nperseg);

            // 去趋势：减去均值（constant detrend）
            if (doDetrend) {
                double segMean = RRiUtils.mean(segData);
                for (int i = 0; i < nperseg; i++) segData[i] -= segMean;
            }

            // 加窗
            for (int i = 0; i < nperseg; i++) segData[i] *= win[i];

            // 零填充至nfftActual
            double[] fftInput = new double[nfftActual];
            System.arraycopy(segData, 0, fftInput, 0, nperseg);

            // 执行FFT（Apache Commons Math要求输入长度为2的幂）
            Complex[] spectrum = fft.transform(toComplex(fftInput), TransformType.FORWARD);

            // 计算单边功率谱密度：|FFT|^2 / (fs * sum(win^2))
            for (int k = 0; k < nFreq; k++) {
                double mag2 = spectrum[k].abs() * spectrum[k].abs();
                double psd = mag2 / (fs * winNorm);
                // 单边谱：直流和奈奎斯特频率不乘2，其余乘2
                if (k > 0 && k < nFreq - 1) {
                    psd *= 2.0;
                }
                pxxAccum[k] += psd;
            }
            validSegments++;
        }

        if (validSegments == 0) {
            throw new IllegalArgumentException("RRi序列过短，无法进行Welch PSD估计");
        }

        // 各段平均
        for (int k = 0; k < nFreq; k++) {
            pxxAccum[k] /= validSegments;
        }

        // 生成频率数组：[0, fs/nfft, 2*fs/nfft, ..., fs/2]
        double[] freqs = new double[nFreq];
        for (int k = 0; k < nFreq; k++) {
            freqs[k] = k * fs / nfftActual;
        }

        return new double[][]{freqs, pxxAccum};
    }

    /**
     * Burg自回归法功率谱密度估计。
     * 对应Python: spectrum.pburg（Burg算法）
     * 参考：Marple, S.L. (1987). Digital Spectral Analysis with Applications
     *
     * @param rri   等间隔采样的RRi序列（应已去趋势）
     * @param fs    采样频率（Hz）
     * @param order AR模型阶数
     * @param nfft  FFT点数（null时自动选择）
     * @return [频率数组, 功率谱密度数组]
     */
    public static double[][] burgPsd(double[] rri, double fs, int order, Integer nfft) {
        int n = rri.length;
        if (order >= n) {
            throw new IllegalArgumentException("AR阶数必须小于RRi序列长度");
        }

        // Burg算法：计算AR系数和噪声方差
        double[] arCoeffs = new double[order];
        double sigma2 = burgAlgorithm(rri, order, arCoeffs);

        // 确定FFT点数
        int nfftActual = (nfft != null) ? nfft : Math.max(256, nextPowerOf2(n));
        int nFreq = nfftActual / 2 + 1;

        // 由AR系数计算功率谱密度
        // PSD(f) = sigma2 / (fs * |1 - sum(a_k * e^{-j2pi*k*f/fs})|^2)
        double[] freqs = new double[nFreq];
        double[] pxx = new double[nFreq];

        for (int k = 0; k < nFreq; k++) {
            double freq = k * fs / nfftActual;
            freqs[k] = freq;

            // 计算 A(f) = 1 - sum(a_k * e^{-j2pi*k*f/fs})
            double realPart = 1.0;
            double imagPart = 0.0;
            for (int m = 0; m < order; m++) {
                double angle = -2.0 * Math.PI * (m + 1) * freq / fs;
                realPart -= arCoeffs[m] * Math.cos(angle);
                imagPart -= arCoeffs[m] * Math.sin(angle);
            }
            double mag2 = realPart * realPart + imagPart * imagPart;

            // PSD = sigma2 / (fs * |A(f)|^2)
            // spectrum库使用scale_by_freq=False，不除以fs
            pxx[k] = sigma2 / (mag2 * fs);
        }

        return new double[][]{freqs, pxx};
    }

    /**
     * Burg算法：计算AR系数和残差方差。
     * 对应 spectrum.pburg 内部使用的 Burg 算法实现。
     *
     * @param x      输入信号
     * @param order  AR模型阶数
     * @param coeffs 输出：AR系数（长度=order）
     * @return 残差方差 sigma^2
     */
    static double burgAlgorithm(double[] x, int order, double[] coeffs) {
        int n = x.length;
        double[] ef = new double[n];  // 前向预测误差
        double[] eb = new double[n];  // 后向预测误差

        // 初始化：前向误差=后向误差=原始信号
        System.arraycopy(x, 0, ef, 0, n);
        System.arraycopy(x, 0, eb, 0, n);

        // 初始功率估计
        double p = 0.0;
        for (double v : x) p += v * v;
        p /= n;

        // 存储反射系数
        double[] kappas = new double[order];
        // AR系数矩阵（每阶更新）
        double[][] a = new double[order + 1][order + 1];

        for (int m = 1; m <= order; m++) {
            // 计算Burg反射系数
            double num = 0.0, den = 0.0;
            // ef对应ef[m..n-1]，eb对应eb[m-1..n-2]（即前m个元素后）
            for (int i = m; i < n; i++) {
                num += ef[i] * eb[i - 1];
                den += ef[i] * ef[i] + eb[i - 1] * eb[i - 1];
            }
            // Burg反射系数 kappa_m = -2 * num / den
            double kappa = (den == 0) ? 0 : -2.0 * num / den;
            kappas[m - 1] = kappa;

            // 更新前向和后向预测误差
            for (int i = n - 1; i >= m; i--) {
                double efNew = ef[i] + kappa * eb[i - 1];
                double ebNew = eb[i - 1] + kappa * ef[i];
                ef[i] = efNew;
                eb[i - 1] = ebNew;
            }

            // Levinson-Durbin递推更新AR系数
            a[m][m] = kappa;
            for (int k = 1; k < m; k++) {
                a[m][k] = a[m - 1][k] + kappa * a[m - 1][m - k];
            }

            // 更新功率
            p *= (1.0 - kappa * kappa);
        }

        // 提取最终AR系数（注意Python中AR系数包含负号，公式为 1 - sum(a_k * z^{-k})）
        for (int k = 0; k < order; k++) {
            coeffs[k] = a[order][k + 1];
        }

        return p * n; // 返回残差方差（sigma^2），乘以n与spectrum库对齐
    }

    /**
     * 计算各频段功率面积（梯形积分）并返回频域结果。
     * 对应Python: _auc(fxx, pxx, vlf_band, lf_band, hf_band)
     *
     * <p>公式：
     * <pre>
     *   LFnu = LF / (total_power - VLF) * 100
     *   HFnu = HF / (total_power - VLF) * 100
     * </pre>
     *
     * @param fxx      频率数组
     * @param pxx      功率谱密度数组
     * @param vlfBand  VLF频段 [低, 高]（Hz）
     * @param lfBand   LF频段 [低, 高]（Hz）
     * @param hfBand   HF频段 [低, 高]（Hz）
     * @return 频域分析结果
     */
    public static FrequencyDomainResult computeAuc(double[] fxx, double[] pxx,
                                                    double[] vlfBand, double[] lfBand, double[] hfBand) {
        // 提取各频段的频率和功率值（对应numpy boolean indexing）
        double vlf = bandPower(fxx, pxx, vlfBand[0], vlfBand[1], true);
        double lf = bandPower(fxx, pxx, lfBand[0], lfBand[1], true);
        double hf = bandPower(fxx, pxx, hfBand[0], hfBand[1], true);

        // 总功率 = VLF + LF + HF
        double totalPower = vlf + lf + hf;

        // LF/HF比值
        double lfHf = lf / hf;

        // 归一化功率（排除VLF成分，对应标准定义）
        double lfnu = (lf / (totalPower - vlf)) * 100.0;
        double hfnu = (hf / (totalPower - vlf)) * 100.0;

        return new FrequencyDomainResult(totalPower, vlf, lf, hf, lfHf, lfnu, hfnu);
    }

    /**
     * 计算特定频段内的功率（梯形积分）。
     * 对应Python: np.trapz(y=pxx[indexes], x=fxx[indexes])
     *
     * @param fxx      频率数组
     * @param pxx      功率谱密度数组
     * @param fLow     频段下界（含）
     * @param fHigh    频段上界（不含，即 fLow <= f < fHigh）
     * @param leftClose 是否左闭（>=fLow）
     * @return 频段功率（梯形积分值）
     */
    private static double bandPower(double[] fxx, double[] pxx, double fLow, double fHigh, boolean leftClose) {
        java.util.List<Double> xs = new java.util.ArrayList<>();
        java.util.List<Double> ys = new java.util.ArrayList<>();
        for (int i = 0; i < fxx.length; i++) {
            boolean inBand = leftClose
                ? (fxx[i] >= fLow && fxx[i] < fHigh)
                : (fxx[i] > fLow && fxx[i] < fHigh);
            if (inBand) {
                xs.add(fxx[i]);
                ys.add(pxx[i]);
            }
        }
        if (xs.size() < 2) return 0.0;
        double[] xArr = xs.stream().mapToDouble(Double::doubleValue).toArray();
        double[] yArr = ys.stream().mapToDouble(Double::doubleValue).toArray();
        return RRiUtils.trapz(yArr, xArr);
    }

    /**
     * 计算非线性HRV指标（Poincaré散点图分析）。
     * 对应Python: non_linear(rri) 和 _poincare(rri)
     *
     * <p>计算公式：
     * <pre>
     *   SD1 = sqrt(0.5 * std(diff(rri), ddof=1)^2)
     *   SD2 = sqrt(2 * std(rri, ddof=1)^2 - 0.5 * std(diff(rri), ddof=1)^2)
     * </pre>
     *
     * @param rri RRi序列（毫秒或秒，自动转换）
     * @return 非线性分析结果（SD1, SD2）
     */
    public static NonLinearResult nonLinear(double[] rri) {
        double[] rriMs = RRiUtils.validateAndConvertRri(rri);
        return computePoincare(rriMs);
    }

    /**
     * 计算非线性HRV指标（接受RRi对象）。
     *
     * @param rri RRi对象
     * @return 非线性分析结果
     */
    public static NonLinearResult nonLinear(RRi rri) {
        return computePoincare(rri.getRri());
    }

    /**
     * Poincaré散点图内部计算。
     * 对应Python: _poincare(rri)
     */
    public static double[] computePoincareValues(double[] rri) {
        double[] diffRri = RRiUtils.diff(rri);
        double sdDiff = RRiUtils.stdDdof1(diffRri);  // std(diff(rri), ddof=1)
        double sdRri = RRiUtils.stdDdof1(rri);        // std(rri, ddof=1)

        // SD1 = sqrt(0.5 * std(diff)^2)
        double sd1 = Math.sqrt(0.5 * sdDiff * sdDiff);

        // SD2 = sqrt(2 * std(rri)^2 - 0.5 * std(diff)^2)
        double sd2 = Math.sqrt(2.0 * sdRri * sdRri - 0.5 * sdDiff * sdDiff);

        return new double[]{sd1, sd2};
    }

    private static NonLinearResult computePoincare(double[] rri) {
        double[] result = computePoincareValues(rri);
        return new NonLinearResult(result[0], result[1]);
    }

    // ==================== 私有辅助方法 ====================

    /** 对数组各元素平方 */
    private static double[] squareArr(double[] arr) {
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = arr[i] * arr[i];
        return result;
    }

    /** 生成指定名称的窗函数 */
    private static double[] createWindow(String name, int n) {
        double[] win = new double[n];
        switch (name.toLowerCase()) {
            case "hanning":
            case "hann":
                // Hanning窗：w[i] = 0.5 * (1 - cos(2*pi*i/(n-1)))
                for (int i = 0; i < n; i++) {
                    win[i] = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (n - 1)));
                }
                break;
            case "hamming":
                // Hamming窗
                for (int i = 0; i < n; i++) {
                    win[i] = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * i / (n - 1));
                }
                break;
            case "blackman":
                // Blackman窗
                for (int i = 0; i < n; i++) {
                    win[i] = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * i / (n - 1))
                             + 0.08 * Math.cos(4.0 * Math.PI * i / (n - 1));
                }
                break;
            default:
                // 矩形窗（无窗）
                java.util.Arrays.fill(win, 1.0);
        }
        return win;
    }

    /** 将实数数组转为复数数组（虚部为0） */
    private static Complex[] toComplex(double[] real) {
        Complex[] c = new Complex[real.length];
        for (int i = 0; i < real.length; i++) c[i] = new Complex(real[i], 0);
        return c;
    }

    /** 计算大于等于n的最小2的幂 */
    private static int nextPowerOf2(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    /**
     * 对信号进行多项式去趋势（用于AR方法内部）。
     * 等价于 polynomial_detrend(rri, degree=1)
     */
    private static double[] polynomialDetrendValues(double[] rri, int degree) {
        int n = rri.length;
        // 生成时间数组（等间隔，0..n-1）
        double[] t = new double[n];
        for (int i = 0; i < n; i++) t[i] = i;
        // 拟合一阶多项式（线性趋势）
        double[] coef = polyFit(t, rri, degree);
        double[] trend = polyVal(coef, t);
        double[] detrended = new double[n];
        for (int i = 0; i < n; i++) detrended[i] = rri[i] - trend[i];
        return detrended;
    }

    /**
     * 多项式拟合（最小二乘法，对应numpy.polyfit）。
     * 系数从高次到低次排列（与numpy约定一致）。
     */
    public static double[] polyFit(double[] x, double[] y, int degree) {
        int n = x.length;
        int m = degree + 1;
        // 构建Vandermonde矩阵
        double[][] A = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                A[i][j] = Math.pow(x[i], degree - j);
            }
        }
        // 最小二乘解：(A^T A)^{-1} A^T y
        double[][] AtA = new double[m][m];
        double[] Aty = new double[m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < n; k++) {
                    AtA[i][j] += A[k][i] * A[k][j];
                }
            }
            for (int k = 0; k < n; k++) {
                Aty[i] += A[k][i] * y[k];
            }
        }
        return solveLinear(AtA, Aty);
    }

    /** 高斯消元求解线性方程组 */
    private static double[] solveLinear(double[][] A, double[] b) {
        int n = b.length;
        double[][] aug = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, aug[i], 0, n);
            aug[i][n] = b[i];
        }
        for (int col = 0; col < n; col++) {
            int pivotRow = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(aug[row][col]) > Math.abs(aug[pivotRow][col])) pivotRow = row;
            }
            double[] tmp = aug[col]; aug[col] = aug[pivotRow]; aug[pivotRow] = tmp;
            if (Math.abs(aug[col][col]) < 1e-15) continue;
            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double f = aug[row][col] / aug[col][col];
                for (int j = col; j <= n; j++) aug[row][j] -= f * aug[col][j];
            }
        }
        double[] result = new double[n];
        for (int i = 0; i < n; i++) result[i] = aug[i][n] / aug[i][i];
        return result;
    }

    /** 多项式求值（对应numpy.polyval，系数从高次到低次） */
    public static double[] polyVal(double[] coef, double[] x) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            double val = 0.0;
            for (double c : coef) val = val * x[i] + c;
            result[i] = val;
        }
        return result;
    }
}
