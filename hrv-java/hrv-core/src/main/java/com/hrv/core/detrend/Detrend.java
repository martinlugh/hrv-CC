package com.hrv.core.detrend;

import com.hrv.core.classical.ClassicalAnalysis;
import com.hrv.core.rri.RRi;
import com.hrv.core.rri.RRiDetrended;
import com.hrv.core.utils.RRiUtils;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * RRi序列去趋势算法。
 * 提供3种去趋势方法，对应Python原项目中的 hrv/detrend.py。
 *
 * <p>方法列表：
 * <ul>
 *   <li>{@link #polynomialDetrend(RRi, int)}：多项式拟合去趋势</li>
 *   <li>{@link #smoothnessPriors(RRi, double, double)}：平滑先验正则化去趋势（Tarvainen et al. 2002）</li>
 *   <li>{@link #sgDetrend(RRi, int, int)}：Savitzky-Golay低通滤波去趋势</li>
 * </ul>
 *
 * <p>所有方法返回 {@link RRiDetrended} 对象，标记 detrended=true。
 */
public class Detrend {

    /**
     * 多项式去趋势。
     * 对RRi序列拟合指定阶数的多项式，然后从信号中减去该趋势项。
     * 对应Python: polynomial_detrend(rri, degree=1)
     *
     * <p>算法：
     * <pre>
     *   coef = np.polyfit(time, rri, deg=degree)
     *   polynomial = np.polyval(coef, time)
     *   detrended_rri = rri - polynomial
     * </pre>
     *
     * @param rri    输入RRi序列
     * @param degree 多项式阶数，默认1（线性趋势）
     * @return 去趋势后的RRi序列（RRiDetrended）
     */
    public static RRiDetrended polynomialDetrend(RRi rri, int degree) {
        double[] rriVals = rri.getRri();
        double[] time = rri.getTime();
        return polynomialDetrend(rriVals, time, degree);
    }

    /** 多项式去趋势，接受原始数组 */
    public static RRiDetrended polynomialDetrend(double[] rri, int degree) {
        double[] time = RRiUtils.createTimeArray(rri);
        return polynomialDetrend(rri, time, degree);
    }

    /** 默认degree=1的多项式去趋势 */
    public static RRiDetrended polynomialDetrend(RRi rri) {
        return polynomialDetrend(rri, 1);
    }

    private static RRiDetrended polynomialDetrend(double[] rri, double[] time, int degree) {
        // 拟合多项式（对应numpy.polyfit）
        double[] coef = ClassicalAnalysis.polyFit(time, rri, degree);
        // 计算趋势项（对应numpy.polyval）
        double[] trend = ClassicalAnalysis.polyVal(coef, time);
        // 去除趋势
        double[] detrended = new double[rri.length];
        for (int i = 0; i < rri.length; i++) {
            detrended[i] = rri[i] - trend[i];
        }
        return new RRiDetrended(detrended, time);
    }

    /**
     * 平滑先验去趋势（Smoothness Priors）。
     * 使用正则化方法估计RRi序列的平稳成分，需要先对RRi进行三次样条插值。
     * 对应Python: smoothness_priors(rri, l=500, fs=4.0)
     *
     * <p>算法（Tarvainen et al. 2002）：
     * <pre>
     *   1. 三次样条插值至等间隔采样（fs=4Hz）
     *   2. 构建N阶单位矩阵 I
     *   3. 构建(N-2)×N二阶差分矩阵 D2
     *   4. z_stat = (I - inv(I + l^2 * D2.T @ D2)) @ z  （z_stat为平稳分量）
     *   5. 返回 z_stat
     * </pre>
     *
     * <p>参考文献：
     * M.P. Tarvainen, P.O. Ranta-aho and P.A. Karjalainen, An advanced detrending method with
     * application to HRV analysis, IEEE Transaction on Biomedical Engineering 49 (2002), 172-175.
     *
     * @param rri 输入RRi序列
     * @param l   正则化参数，默认500（越大去除低频越多）
     * @param fs  重采样频率（Hz），默认4.0
     * @return 去趋势后的RRi序列（RRiDetrended，interpolated=true）
     */
    public static RRiDetrended smoothnessPriors(RRi rri, double l, double fs) {
        double[] rriVals = rri.getRri();
        double[] time = rri.getTime();
        return smoothnessPriorsInternal(rriVals, time, l, fs);
    }

    /** 默认参数：l=500, fs=4.0 */
    public static RRiDetrended smoothnessPriors(RRi rri) {
        return smoothnessPriors(rri, 500.0, 4.0);
    }

    private static RRiDetrended smoothnessPriorsInternal(double[] rri, double[] time, double l, double fs) {
        // 1. 三次样条插值至均匀采样时间网格
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction spline = interpolator.interpolate(time, rri);

        // 生成均匀时间网格：从time[0]到time[-1]，步长=1/fs
        double step = 1.0 / fs;
        int N = (int)((time[time.length - 1] - time[0]) / step) + 1;
        double[] timeInterp = new double[N];
        double[] rriInterp = new double[N];
        double maxT = time[time.length - 1];

        for (int i = 0; i < N; i++) {
            timeInterp[i] = time[0] + i * step;
            // 防止超出样条定义域
            double t = Math.min(timeInterp[i], maxT);
            rriInterp[i] = spline.value(t);
        }

        // 2. 构建单位矩阵 I（N×N）
        double[][] identity = new double[N][N];
        for (int i = 0; i < N; i++) identity[i][i] = 1.0;

        // 3. 构建二阶差分矩阵 D2（(N-2)×N）
        // D2对应Python: B = np.dot(np.ones((N,1)), [[1,-2,1]]), D_2 = dia_matrix((B.T, [0,1,2]), shape=(N-2,N))
        // 即：D2[i][i]=1, D2[i][i+1]=-2, D2[i][i+2]=1，for i=0..N-3
        double[][] D2 = new double[N - 2][N];
        for (int i = 0; i < N - 2; i++) {
            D2[i][i]     =  1.0;
            D2[i][i + 1] = -2.0;
            D2[i][i + 2] =  1.0;
        }

        // 4. 计算 D2.T（N×(N-2)）
        double[][] D2T = transpose(D2);

        // 5. 计算 D2.T @ D2（N×N）
        double[][] D2TD2 = matMul(D2T, D2);

        // 6. 计算 I + l^2 * D2.T @ D2
        double l2 = l * l;
        double[][] IplusL2D2TD2 = new double[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                IplusL2D2TD2[i][j] = identity[i][j] + l2 * D2TD2[i][j];
            }
        }

        // 7. 计算矩阵的逆：inv(I + l^2 * D2.T @ D2)
        double[][] inv = invertMatrix(IplusL2D2TD2);

        // 8. 计算 z_stat = (I - inv) @ rri_interp
        // 先计算 (I - inv)
        double[][] IminusInv = new double[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                IminusInv[i][j] = identity[i][j] - inv[i][j];
            }
        }

        // z_stat = (I - inv) @ rri_interp（矩阵-向量乘法）
        double[] zStat = matVecMul(IminusInv, rriInterp);

        // Python原代码返回的是 rri_interp - rri_interp_detrend = z_stat
        // 即平稳分量（去趋势后的信号）
        return new RRiDetrended(zStat, timeInterp, true);
    }

    /**
     * Savitzky-Golay去趋势。
     * 用Savitzky-Golay低通滤波器估计趋势项，然后从原始信号中减去。
     * 对应Python: sg_detrend(rri, window_length=51, polyorder=3)
     *
     * <p>算法：
     * <pre>
     *   trend = savgol_filter(rri, window_length, polyorder)
     *   detrended = rri - trend
     * </pre>
     *
     * @param rri          输入RRi序列
     * @param windowLength SG滤波器窗长（必须为正奇数），默认51
     * @param polyorder    多项式阶数（必须 < windowLength），默认3
     * @return 去趋势后的RRi序列（RRiDetrended）
     */
    public static RRiDetrended sgDetrend(RRi rri, int windowLength, int polyorder) {
        double[] rriVals = rri.getRri();
        double[] time = rri.getTime();
        return sgDetrendInternal(rriVals, time, windowLength, polyorder);
    }

    /** 默认参数：windowLength=51, polyorder=3 */
    public static RRiDetrended sgDetrend(RRi rri) {
        return sgDetrend(rri, 51, 3);
    }

    private static RRiDetrended sgDetrendInternal(double[] rri, double[] time, int windowLength, int polyorder) {
        if (windowLength % 2 == 0) {
            throw new IllegalArgumentException("window_length必须为奇数，当前值: " + windowLength);
        }
        if (polyorder >= windowLength) {
            throw new IllegalArgumentException("polyorder必须小于window_length");
        }

        // 应用Savitzky-Golay滤波计算趋势
        double[] trend = savitzkyGolayFilter(rri, windowLength, polyorder);

        // 去除趋势
        double[] detrended = new double[rri.length];
        for (int i = 0; i < rri.length; i++) {
            detrended[i] = rri[i] - trend[i];
        }
        return new RRiDetrended(detrended, time);
    }

    /**
     * Savitzky-Golay滤波器实现。
     * 对应Python: scipy.signal.savgol_filter
     *
     * <p>算法：对每个点，在以该点为中心的窗口内拟合指定阶多项式，用多项式的0阶微分（即拟合值）代替原值。
     * 边界处理：使用"mirror"模式（对称延伸）。
     *
     * @param data         输入数据
     * @param windowLength 窗长（奇数）
     * @param polyorder    多项式阶数
     * @return 滤波结果
     */
    static double[] savitzkyGolayFilter(double[] data, int windowLength, int polyorder) {
        int n = data.length;
        int half = windowLength / 2;

        // 预计算SG系数（卷积核）
        // 系数通过对Vandermonde矩阵求伪逆得到
        double[] kernel = computeSgCoefficients(windowLength, polyorder);

        // 对数据进行边界扩展（镜像填充），以处理边界效应
        double[] padded = new double[n + 2 * half];
        for (int i = 0; i < half; i++) {
            padded[i] = 2 * data[0] - data[half - i]; // 左镜像
        }
        System.arraycopy(data, 0, padded, half, n);
        for (int i = 0; i < half; i++) {
            padded[n + half + i] = 2 * data[n - 1] - data[n - 2 - i]; // 右镜像
        }

        // 应用卷积核
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j < windowLength; j++) {
                sum += kernel[j] * padded[i + j];
            }
            result[i] = sum;
        }
        return result;
    }

    /**
     * 计算Savitzky-Golay卷积系数。
     * 通过对中心在0的均匀网格上的Vandermonde矩阵求伪逆得到，
     * 取第0行（对应0阶导数即平滑）。
     *
     * @param windowLength 窗长
     * @param polyorder    多项式阶数
     * @return 卷积系数数组
     */
    public static double[] computeSgCoefficients(int windowLength, int polyorder) {
        int half = windowLength / 2;
        int m = polyorder + 1;

        // 构建位置数组 x = [-half, ..., 0, ..., half]
        double[] x = new double[windowLength];
        for (int i = 0; i < windowLength; i++) x[i] = i - half;

        // 构建Vandermonde矩阵 A（windowLength × m），A[i][j] = x[i]^j
        double[][] A = new double[windowLength][m];
        for (int i = 0; i < windowLength; i++) {
            for (int j = 0; j < m; j++) {
                A[i][j] = Math.pow(x[i], j);
            }
        }

        // 计算 (A^T A)^{-1} A^T（即A的伪逆）
        // 系数 = A的伪逆 的第0行（对应多项式在中心点的值，即平滑）
        double[][] AtA = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                for (int k = 0; k < windowLength; k++) {
                    AtA[i][j] += A[k][i] * A[k][j];
                }
            }
        }
        double[][] AtAinv = invertMatrix(AtA);

        // 系数 = (A^T A)^{-1} @ A^T 的第0行
        // 即对每个窗口位置k，系数 = sum_j(AtAinv[0][j] * A[k][j])
        double[] coeff = new double[windowLength];
        for (int k = 0; k < windowLength; k++) {
            double val = 0.0;
            for (int j = 0; j < m; j++) {
                val += AtAinv[0][j] * A[k][j];
            }
            coeff[k] = val;
        }
        return coeff;
    }

    // ==================== 矩阵运算辅助方法 ====================

    /** 矩阵转置 */
    static double[][] transpose(double[][] A) {
        int rows = A.length, cols = A[0].length;
        double[][] T = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                T[j][i] = A[i][j];
            }
        }
        return T;
    }

    /** 矩阵乘法：A @ B */
    static double[][] matMul(double[][] A, double[][] B) {
        int n = A.length, m = B[0].length, k = B.length;
        double[][] C = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int l = 0; l < k; l++) {
                    C[i][j] += A[i][l] * B[l][j];
                }
            }
        }
        return C;
    }

    /** 矩阵-向量乘法：A @ v */
    static double[] matVecMul(double[][] A, double[] v) {
        int n = A.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < v.length; j++) {
                result[i] += A[i][j] * v[j];
            }
        }
        return result;
    }

    /**
     * 方阵求逆（高斯-约旦消元法）。
     *
     * @param M 输入方阵
     * @return 逆矩阵
     */
    static double[][] invertMatrix(double[][] M) {
        int n = M.length;
        // 构建增广矩阵 [M | I]
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(M[i], 0, aug[i], 0, n);
            aug[i][n + i] = 1.0; // 单位矩阵部分
        }

        // 高斯-约旦消元
        for (int col = 0; col < n; col++) {
            // 选主元（列中绝对值最大的行）
            int pivotRow = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(aug[row][col]) > Math.abs(aug[pivotRow][col])) {
                    pivotRow = row;
                }
            }
            // 交换行
            double[] tmp = aug[col];
            aug[col] = aug[pivotRow];
            aug[pivotRow] = tmp;

            double pivot = aug[col][col];
            if (Math.abs(pivot) < 1e-15) {
                throw new ArithmeticException("矩阵奇异，无法求逆");
            }

            // 归一化主行
            for (int j = col; j < 2 * n; j++) aug[col][j] /= pivot;

            // 消元
            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = aug[row][col];
                for (int j = col; j < 2 * n; j++) {
                    aug[row][j] -= factor * aug[col][j];
                }
            }
        }

        // 提取逆矩阵（右半部分）
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(aug[i], n, inv[i], 0, n);
        }
        return inv;
    }
}
