package com.hrv.tests;

import com.hrv.core.classical.ClassicalAnalysis;
import com.hrv.core.classical.FrequencyDomainResult;
import com.hrv.core.classical.NonLinearResult;
import com.hrv.core.classical.TimeDomainResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 经典HRV分析测试。
 * 对应Python原项目 tests/test_classical.py。
 * 验证时域、频域、非线性指标计算结果与Python实现在合理误差范围内一致。
 *
 * <p>误差容忍度：
 * <ul>
 *   <li>时域指标：1e-2（对应Python的decimal=2）</li>
 *   <li>频域指标（Welch PSD）：1e-0（由于插值边界条件差异，误差放宽至整数级）</li>
 *   <li>非线性指标：1e-1（对应Python的decimal=1）</li>
 * </ul>
 */
@DisplayName("经典HRV分析测试")
public class ClassicalTest {

    /** 与Python测试中FAKE_RRI完全一致 */
    static final double[] FAKE_RRI = {800.0, 810.0, 815.0, 750.0};

    // ==================== 时域分析测试 ====================

    @Test
    @DisplayName("时域分析：正向测试 - 计算结果与Python原项目期望值一致（精度1e-2）")
    void testTimeDomainCorrectResponse() {
        TimeDomainResult result = ClassicalAnalysis.timeDomain(FAKE_RRI);

        // Python原项目期望值（tests/test_classical.py :: test_correct_response）
        assertEquals(38.07, result.getRmssd(), 0.01, "RMSSD");
        assertEquals(29.82, result.getSdnn(), 0.01, "SDNN");
        assertEquals(41.93, result.getSdsd(), 0.01, "SDSD");
        assertEquals(1, result.getNn50(), "NN50");
        assertEquals(25.0, result.getPnn50(), 0.01, "PNN50 (%)");
        assertEquals(793.75, result.getMrri(), 0.01, "MRRI");
        assertEquals(75.67, result.getMhr(), 0.01, "MHR");
    }

    @Test
    @DisplayName("时域分析：秒单位RRi自动转换为毫秒后结果一致")
    void testTimeDomainWithRriInSeconds() {
        double[] rriInSeconds = new double[FAKE_RRI.length];
        for (int i = 0; i < FAKE_RRI.length; i++) {
            rriInSeconds[i] = FAKE_RRI[i] / 1000.0; // 转为秒
        }

        TimeDomainResult result = ClassicalAnalysis.timeDomain(rriInSeconds);

        // 结果应与毫秒输入完全一致
        assertEquals(38.07, result.getRmssd(), 0.01, "秒单位输入时RMSSD应与毫秒输入一致");
        assertEquals(793.75, result.getMrri(), 0.01, "MRRI");
        assertEquals(75.67, result.getMhr(), 0.01, "MHR");
    }

    @Test
    @DisplayName("时域分析：NN50计算正确")
    void testNn50() {
        int nn50 = ClassicalAnalysis.computeNn50(FAKE_RRI);
        // diff([800, 810, 815, 750]) = [10, 5, -65]
        // abs(diff) = [10, 5, 65]，只有65 > 50，故 nn50 = 1
        assertEquals(1, nn50, "NN50应为1（只有一个连续差值>50ms）");
    }

    @Test
    @DisplayName("时域分析：PNN50计算正确")
    void testPnn50() {
        double pnn50 = ClassicalAnalysis.computePnn50(FAKE_RRI);
        // pnn50 = 1 / 4 * 100 = 25%
        assertEquals(25.0, pnn50, 1e-6, "PNN50应为25%");
    }

    @Test
    @DisplayName("时域分析：异常输入 - 含非正值应抛异常")
    void testTimeDomainRejectsNegativeRri() {
        assertThrows(IllegalArgumentException.class, () -> {
            ClassicalAnalysis.timeDomain(new double[]{800, -100, 810});
        }, "含负值的RRi序列应抛出IllegalArgumentException");
    }

    @Test
    @DisplayName("时域分析：边界测试 - 只有两个RRi值")
    void testTimeDomainWithTwoValues() {
        double[] twoRri = {800.0, 810.0};
        TimeDomainResult result = ClassicalAnalysis.timeDomain(twoRri);
        // diff = [10]，rmssd = sqrt(100) = 10
        assertEquals(10.0, result.getRmssd(), 1e-6, "两点序列的RMSSD");
        assertEquals(0, result.getNn50(), "两点序列NN50应为0（差值10 < 50）");
    }

    // ==================== 频域分析测试 ====================

    @Test
    @DisplayName("频域分析：AUC计算（梯形积分）与Python一致")
    void testAreaUnderTheCurve() {
        // 构造均匀频率分辨率为1/1000的频率数组，PSD全为1
        int n = 1000;
        double[] fxx = new double[n];
        double[] pxx = new double[n];
        for (int i = 0; i < n; i++) {
            fxx[i] = i / 1000.0;
            pxx[i] = 1.0;
        }

        FrequencyDomainResult result = ClassicalAnalysis.computeAuc(
            fxx, pxx,
            new double[]{0, 0.04},
            new double[]{0.04, 0.15},
            new double[]{0.15, 0.4}
        );

        // Python期望值（tests/test_classical.py :: test_area_under_the_curve）
        assertEquals(0.04, result.getVlf(), 0.01, "VLF功率 ≈ 0.04");
        assertEquals(0.11, result.getLf(), 0.01, "LF功率 ≈ 0.11");
        assertEquals(0.25, result.getHf(), 0.01, "HF功率 ≈ 0.25");
        assertEquals(0.40, result.getTotalPower(), 0.01, "总功率 ≈ 0.40");
        assertEquals(0.44, result.getLfHf(), 0.1, "LF/HF ≈ 0.44");
        assertEquals(30.5, result.getLfnu(), 0.5, "LFnu ≈ 30.5%");
        assertEquals(69.5, result.getHfnu(), 0.5, "HFnu ≈ 69.5%");
    }

    @Test
    @DisplayName("频域分析：VLF+LF+HF = total_power")
    void testTotalPowerEqualsSum() {
        int n = 1000;
        double[] fxx = new double[n];
        double[] pxx = new double[n];
        for (int i = 0; i < n; i++) {
            fxx[i] = i / 1000.0;
            pxx[i] = 1.0;
        }
        FrequencyDomainResult result = ClassicalAnalysis.computeAuc(
            fxx, pxx,
            new double[]{0, 0.04}, new double[]{0.04, 0.15}, new double[]{0.15, 0.4}
        );
        assertEquals(result.getVlf() + result.getLf() + result.getHf(),
            result.getTotalPower(), 1e-10, "VLF+LF+HF应等于total_power");
    }

    @Test
    @DisplayName("频域分析：LFnu+HFnu = 100%")
    void testLfnuPlusHfnuEquals100() {
        int n = 1000;
        double[] fxx = new double[n];
        double[] pxx = new double[n];
        for (int i = 0; i < n; i++) {
            fxx[i] = i / 1000.0;
            pxx[i] = 1.0;
        }
        FrequencyDomainResult result = ClassicalAnalysis.computeAuc(
            fxx, pxx,
            new double[]{0, 0.04}, new double[]{0.04, 0.15}, new double[]{0.15, 0.4}
        );
        assertEquals(100.0, result.getLfnu() + result.getHfnu(), 1e-6, "LFnu+HFnu应等于100%");
    }

    @Test
    @DisplayName("频域分析：Welch PSD可正常运行")
    void testWelchPsdCanRun() {
        // 生成较长的RRi序列，使Welch分析可以进行
        double[] longRri = generateSineRri(500, 800.0, 50.0, 0.1);
        FrequencyDomainResult result = ClassicalAnalysis.frequencyDomain(longRri, null);
        assertNotNull(result, "频域分析结果不应为null");
        assertTrue(result.getTotalPower() > 0, "总功率应大于0");
        assertTrue(result.getLf() >= 0, "LF功率应非负");
        assertTrue(result.getHf() >= 0, "HF功率应非负");
    }

    @Test
    @DisplayName("频域分析：异常输入 - 不支持的method应抛异常")
    void testInvalidMethodThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ClassicalAnalysis.frequencyDomain(FAKE_RRI, null, 4.0, "invalid_method",
                "cubic", "constant", new double[]{0, 0.04}, new double[]{0.04, 0.15},
                new double[]{0.15, 0.4}, 256, 128, "hanning", 16, null);
        }, "不支持的method应抛出IllegalArgumentException");
    }

    // ==================== 非线性分析测试 ====================

    @Test
    @DisplayName("非线性分析：Poincaré SD1/SD2计算正确（精度1e-1）")
    void testPoincareSd1Sd2() {
        // Python原项目期望值（tests/test_classical.py :: test_correct_response_from_poincare）
        double[] rri = {10.0, 11.0, 25.0, 27.0};
        double[] result = ClassicalAnalysis.computePoincareValues(rri);
        double sd1 = result[0];
        double sd2 = result[1];

        assertEquals(5.11, sd1, 0.1, "SD1 ≈ 5.11ms");
        assertEquals(11.64, sd2, 0.1, "SD2 ≈ 11.64ms");
    }

    @Test
    @DisplayName("非线性分析：非线性结果对象字段名正确")
    void testNonLinearResultFields() {
        NonLinearResult nl = ClassicalAnalysis.nonLinear(FAKE_RRI);
        assertTrue(nl.getSd1() >= 0, "SD1应非负");
        assertTrue(nl.getSd2() >= 0, "SD2应非负");
    }

    @Test
    @DisplayName("非线性分析：SD1公式验证 = sqrt(0.5 * std(diff)^2)")
    void testSd1Formula() {
        double[] rri = {800.0, 810.0, 790.0, 820.0};
        double[] result = ClassicalAnalysis.computePoincareValues(rri);
        double sd1 = result[0];

        // 手动计算: diff = [10, -20, 30], std(diff, ddof=1) = ...
        double[] diff = {10.0, -20.0, 30.0};
        double mean = (10 - 20 + 30) / 3.0; // = 6.667
        double variance = 0;
        for (double d : diff) variance += (d - mean) * (d - mean);
        variance /= 2; // ddof=1
        double sdDiff = Math.sqrt(variance);
        double expectedSd1 = Math.sqrt(0.5 * sdDiff * sdDiff);

        assertEquals(expectedSd1, sd1, 1e-6, "SD1 = sqrt(0.5 * std(diff)^2)");
    }

    @Test
    @DisplayName("非线性分析：异常输入 - 含负值应抛异常")
    void testNonLinearRejectsNegativeRri() {
        assertThrows(IllegalArgumentException.class, () -> {
            ClassicalAnalysis.nonLinear(new double[]{800, -100, 810});
        });
    }

    // ==================== Burg AR方法测试 ====================

    @Test
    @DisplayName("Burg AR：算法可正常运行并返回非空结果")
    void testBurgArCanRun() {
        double[] longRri = generateSineRri(100, 800.0, 30.0, 0.1);
        // 先多项式去趋势（AR方法要求）
        double[][] result = ClassicalAnalysis.burgPsd(longRri, 4.0, 16, null);
        assertNotNull(result, "Burg PSD结果不应为null");
        assertEquals(2, result.length, "应返回[频率数组, 功率数组]");
        assertTrue(result[0].length > 0, "频率数组不应为空");
    }

    // ==================== 辅助方法 ====================

    /** 生成含正弦波调制的模拟RRi序列 */
    private double[] generateSineRri(int n, double mean, double amplitude, double freq) {
        double[] rri = new double[n];
        for (int i = 0; i < n; i++) {
            rri[i] = mean + amplitude * Math.sin(2 * Math.PI * freq * i / 4.0);
        }
        return rri;
    }
}
