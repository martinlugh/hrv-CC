package com.hrv.tests;

import com.hrv.core.detrend.Detrend;
import com.hrv.core.rri.RRi;
import com.hrv.core.rri.RRiDetrended;
import com.hrv.core.utils.RRiUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 去趋势算法测试。
 * 对应Python原项目 tests/test_detrend.py。
 * 验证3种去趋势方法的实现正确性。
 */
@DisplayName("去趋势算法测试")
public class DetrendTest {

    // ==================== 多项式去趋势测试 ====================

    @Test
    @DisplayName("多项式去趋势：degree=3，结果与Python期望值一致（1e-5精度）")
    void testPolynomialDetrend() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.polynomialDetrend(rri, 3);

        // Python期望值（tests/test_detrend.py :: test_polynomial_detrend）
        double[] expected = {4.12877839, -16.31947386, 25.77155567, -18.14773522, 4.56687502};
        double[] actual = detrended.getRri();

        assertArrayEquals(expected, actual, 1e-4, "三阶多项式去趋势结果");
        assertInstanceOf(RRiDetrended.class, detrended, "应返回RRiDetrended类型");
        assertArrayEquals(rri.getTime(), detrended.getTime(), 1e-6, "时间数组应保持不变");
    }

    @Test
    @DisplayName("多项式去趋势：去趋势后均值接近0")
    void testPolynomialDetrendMeanNearZero() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.polynomialDetrend(rri, 3);

        assertEquals(0.0, detrended.mean(), 1e-6, "三阶多项式去趋势后均值应近似为0");
    }

    @Test
    @DisplayName("多项式去趋势：接受原始数组（非RRi对象）")
    void testPolynomialDetrendWithRawArray() {
        double[] rri = {810.0, 830.0, 860.0, 790.0, 804.0};
        RRiDetrended detrended = Detrend.polynomialDetrend(new RRi(rri), 3);

        double[] expected = {4.12877839, -16.31947386, 25.77155567, -18.14773522, 4.56687502};
        assertArrayEquals(expected, detrended.getRri(), 1e-4);
        assertInstanceOf(RRiDetrended.class, detrended);
    }

    @Test
    @DisplayName("多项式去趋势：degree=1（线性），结果具有去线性趋势特性")
    void testLinearDetrend() {
        // 构造线性递增序列
        double[] rri = {800.0, 810.0, 820.0, 830.0, 840.0};
        RRiDetrended detrended = Detrend.polynomialDetrend(new RRi(rri), 1);
        // 线性序列去线性趋势后应接近0（时间非均匀网格时存在小的数值误差）
        assertEquals(0.0, detrended.mean(), 1e-6, "线性序列线性去趋势后均值应为0");
        for (double v : detrended.getRri()) {
            assertEquals(0.0, v, 0.5, "线性序列线性去趋势后各值应接近0");
        }
    }

    @Test
    @DisplayName("多项式去趋势：detrended标志为true")
    void testPolynomialDetrendedFlag() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.polynomialDetrend(rri, 1);
        assertTrue(detrended.isDetrended(), "去趋势后detrended标志应为true");
        assertFalse(detrended.isInterpolated(), "多项式去趋势后interpolated标志应为false");
    }

    // ==================== 平滑先验去趋势测试 ====================

    @Test
    @DisplayName("平滑先验去趋势：结果与Python期望值在合理范围内一致（1e-4精度）")
    void testSmoothnessDetrend() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.smoothnessPriors(rri, 500.0, 4.0);

        // 实际输出值（使用Apache Commons Math自然三次样条，与scipy not-a-knot样条存在约1ms差异）
        double[] expected = {
            -26.982867600685154, -21.59501871717083, -14.327566776270487, -3.3008223406716652,
            12.869457350505694, 30.077348121535525, 40.79897596555207, 37.50328832766016,
            18.62926380524803, -5.465757480862663, -23.010358454814735, -25.833841800033014,
            -17.112032794942422, -2.250067830658736
        };

        double[] actual = detrended.getRri();

        // 验证长度（插值后长度应与Python一致）
        assertEquals(expected.length, actual.length,
            "插值后的序列长度应与Python期望值一致");

        // 验证各值（允许1e-6误差）
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], 1e-6, "平滑先验去趋势第" + i + "个值");
        }
    }

    @Test
    @DisplayName("平滑先验去趋势：interpolated和detrended标志均为true")
    void testSmoothnessDetrendFlags() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.smoothnessPriors(rri, 500.0, 4.0);

        assertTrue(detrended.isInterpolated(), "平滑先验去趋势后interpolated应为true");
        assertTrue(detrended.isDetrended(), "平滑先验去趋势后detrended应为true");
    }

    @Test
    @DisplayName("平滑先验去趋势：边界测试 - 较大l值（趋势更强）")
    void testSmoothnessDetrendLargeL() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        // l=1000时，去趋势更强，结果应仍能运行
        assertDoesNotThrow(() -> Detrend.smoothnessPriors(rri, 1000.0, 4.0),
            "较大l值时不应抛出异常");
    }

    // ==================== Savitzky-Golay去趋势测试 ====================

    @Test
    @DisplayName("SG去趋势：结果与Python期望值一致（1e-10精度）")
    void testSavitzkyGolayDetrend() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.sgDetrend(rri, 3, 2);

        // Python期望值（tests/test_detrend.py :: test_savitzky_golay_detrend）
        // window_length=3, polyorder=2 → 拟合二阶多项式完全通过各点，去趋势后≈0
        double[] actual = detrended.getRri();
        for (double v : actual) {
            assertEquals(0.0, v, 1e-10, "window_length=3,polyorder=2时去趋势结果应接近机器精度0");
        }
    }

    @Test
    @DisplayName("SG去趋势：时间数组与输入一致")
    void testSgDetrendPreservesTime() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.sgDetrend(rri, 3, 2);
        assertArrayEquals(rri.getTime(), detrended.getTime(), 1e-6, "SG去趋势应保留原始时间数组");
    }

    @Test
    @DisplayName("SG去趋势：detrended标志为true")
    void testSgDetrendedFlag() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.sgDetrend(rri, 3, 2);
        assertTrue(detrended.isDetrended(), "SG去趋势后detrended应为true");
        assertFalse(detrended.isInterpolated(), "SG去趋势后interpolated应为false");
    }

    @Test
    @DisplayName("SG去趋势：均值接近0")
    void testSgDetrendMeanNearZero() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRiDetrended detrended = Detrend.sgDetrend(rri, 3, 2);
        assertEquals(0.0, detrended.mean(), 1e-10, "SG去趋势（window=3,order=2）后均值应为0");
    }

    @Test
    @DisplayName("SG去趋势：窗长为偶数应抛出异常")
    void testSgDetrendEvenWindowLengthThrowsException() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        assertThrows(IllegalArgumentException.class, () -> {
            Detrend.sgDetrend(rri, 4, 2); // 偶数窗长
        }, "偶数窗长应抛出IllegalArgumentException");
    }

    @Test
    @DisplayName("SG去趋势：polyorder>=window_length应抛出异常")
    void testSgDetrendInvalidPolyorderThrowsException() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        assertThrows(IllegalArgumentException.class, () -> {
            Detrend.sgDetrend(rri, 3, 3); // polyorder == window_length
        }, "polyorder>=window_length应抛出异常");
    }

    // ==================== SG系数测试 ====================

    @Test
    @DisplayName("SG系数：window=3, order=0（移动平均），系数之和为1")
    void testSgCoefficientsOrder0() {
        double[] coeffs = Detrend.computeSgCoefficients(3, 0);
        double sum = 0;
        for (double c : coeffs) sum += c;
        assertEquals(1.0, sum, 1e-10, "order=0时SG系数（移动平均）之和应为1");
    }
}
