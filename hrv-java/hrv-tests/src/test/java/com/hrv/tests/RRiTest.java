package com.hrv.tests;

import com.hrv.core.rri.RRi;
import com.hrv.core.rri.RRiDetrended;
import com.hrv.core.rri.RRiDescription;
import com.hrv.core.utils.RRiUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RRi类测试。
 * 对应Python原项目中的 tests/test_rri.py。
 * 使用与Python测试相同的 FAKE_RRI = [800, 810, 815, 750] 测试数据。
 */
@DisplayName("RRi序列类测试")
public class RRiTest {

    /** 与Python原项目 tests/test_utils.py 中 FAKE_RRI 完全一致 */
    static final double[] FAKE_RRI = {800.0, 810.0, 815.0, 750.0};

    // ==================== 构造与验证测试 ====================

    @Test
    @DisplayName("RRi对象创建：值自动转换为numpy数组")
    void testTransformRriToArray() {
        double[] validated = RRi.validateRri(FAKE_RRI);
        assertArrayEquals(FAKE_RRI, validated, 1e-10, "验证后的RRi值应与输入相同");
    }

    @Test
    @DisplayName("RRi对象创建：秒单位自动转换为毫秒")
    void testTransformRriInSecondsToMilliseconds() {
        double[] rriInSeconds = {0.8, 0.9, 1.2};
        double[] validated = RRi.validateRri(rriInSeconds);
        assertArrayEquals(new double[]{800.0, 900.0, 1200.0}, validated, 1e-10,
            "秒单位RRi应自动转换为毫秒（×1000）");
    }

    @Test
    @DisplayName("RRi对象创建：基本属性验证")
    void testRriInstance() {
        RRi rri = new RRi(FAKE_RRI);
        assertFalse(rri.isDetrended(), "新建RRi默认非去趋势状态");
        assertFalse(rri.isInterpolated(), "新建RRi默认非插值状态");
        assertEquals(4, rri.size(), "序列长度应为4");
    }

    @Test
    @DisplayName("RRi对象创建：值正确存储")
    void testRriValues() {
        RRi rri = new RRi(FAKE_RRI);
        assertArrayEquals(FAKE_RRI, rri.getRri(), 1e-10, "存储的RRi值应与输入相同");
    }

    @Test
    @DisplayName("RRi时间数组：自动生成（cumsum/1000 - 起始值）")
    void testCreateTimeArray() {
        double[] time = RRiUtils.createTimeArray(FAKE_RRI);
        double[] expected = {0.0, 0.810, 1.625, 2.375};
        // cumsum = [800, 1610, 2425, 3175] / 1000 = [0.8, 1.61, 2.425, 3.175]
        // - 0.8 = [0, 0.81, 1.625, 2.375]
        assertArrayEquals(expected, time, 1e-6, "时间数组应由cumsum(rri)/1000生成并从0开始");
    }

    @Test
    @DisplayName("RRi时间数组：自动生成与RRi对象一致")
    void testRriTimeAutoCreation() {
        RRi rri = new RRi(FAKE_RRI);
        double[] expected = {0.0, 0.810, 1.625, 2.375};
        assertArrayEquals(expected, rri.getTime(), 1e-6, "RRi.getTime()应返回自动生成的时间数组");
    }

    @Test
    @DisplayName("RRi时间数组：传入自定义时间")
    void testRriTimePassedAsArgument() {
        double[] customTime = {1.0, 2.0, 3.0, 4.0};
        RRi rri = new RRi(FAKE_RRI, customTime);
        assertArrayEquals(customTime, rri.getTime(), 1e-10, "传入的自定义时间数组应被正确存储");
    }

    @Test
    @DisplayName("边界测试：RRi和时间数组长度不匹配应抛异常")
    void testRaisesExceptionIfRriAndTimeHaveDifferentLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            RRi.validateTime(FAKE_RRI, new double[]{1.0, 2.0, 3.0});
        }, "RRi和时间数组长度不同时应抛出IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            new RRi(FAKE_RRI, new double[]{1.0, 2.0, 3.0});
        });
    }

    @Test
    @DisplayName("边界测试：时间数组首位后不能含0")
    void testTimeHasNoZeroValueBesidesFirstPosition() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            RRi.validateTime(FAKE_RRI, new double[]{1.0, 2.0, 0.0, 3.0});
        });
        assertThat(ex.getMessage()).contains("time series cannot have 0 values after first position");
    }

    @Test
    @DisplayName("边界测试：时间数组必须单调递增")
    void testTimeIsMonotonicallyIncreasing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            RRi.validateTime(FAKE_RRI, new double[]{0.0, 1.0, 4.0, 3.0});
        });
        assertThat(ex.getMessage()).contains("time series must be monotonically increasing");
    }

    @Test
    @DisplayName("边界测试：时间数组不能含负值")
    void testTimeSeriesHaveNoNegativeValues() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            RRi.validateTime(FAKE_RRI, new double[]{-1.0, 1.0, 2.0, 3.0});
        });
        assertThat(ex.getMessage()).contains("time series cannot have negative values");
    }

    @Test
    @DisplayName("边界测试：RRi序列不能含非正值")
    void testRriSeriesHaveNoNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> {
            RRi.validateRri(new double[]{0.0, 1.0, 2.0, 3.0});
        }, "含0值的RRi序列应抛出异常");

        assertThrows(IllegalArgumentException.class, () -> {
            RRi.validateRri(new double[]{1.0, 2.0, -3.0, 4.0});
        }, "含负值的RRi序列应抛出异常");
    }

    // ==================== 统计方法测试 ====================

    @Test
    @DisplayName("RRi统计方法：均值、方差、标准差、中位数、最大值、最小值、振幅、RMS")
    void testRriStatisticalValues() {
        RRi rri = new RRi(FAKE_RRI);

        assertEquals(793.75, rri.mean(), 1e-6, "均值");
        assertEquals(667.1875, rri.var(), 1e-4, "总体方差");
        assertEquals(Math.sqrt(667.1875), rri.std(), 1e-6, "总体标准差");
        assertEquals(805.0, rri.median(), 1e-6, "中位数");
        assertEquals(815.0, rri.max(), 1e-6, "最大值");
        assertEquals(750.0, rri.min(), 1e-6, "最小值");
        assertEquals(65.0, rri.amplitude(), 1e-6, "振幅 = max - min");

        double expectedRms = Math.sqrt((800*800 + 810*810 + 815*815 + 750*750) / 4.0);
        assertEquals(expectedRms, rri.rms(), 1e-4, "均方根");
    }

    @Test
    @DisplayName("RRi心率转换：to_hr() = 60/(rri/1000)")
    void testRriToHeartRate() {
        RRi rri = new RRi(FAKE_RRI);
        double[] hr = rri.toHr();
        double[] expected = {75.0, 74.07407407, 73.6196319, 80.0};
        assertArrayEquals(expected, hr, 1e-6, "心率 = 60 / (rri/1000)");
    }

    // ==================== 时间操作测试 ====================

    @Test
    @DisplayName("RRi时间区间截取：timeRange(start, end)")
    void testGetRriTimeInterval() {
        RRi rri = new RRi(
            new double[]{800, 810, 790, 795, 817, 785, 910},
            new double[]{2, 4, 6, 8, 10, 12, 14}
        );
        RRi interval = rri.timeRange(10.0, 14.0);
        assertArrayEquals(new double[]{817, 785, 910}, interval.getRri(), 1e-6, "截取区间内的RRi值");
        assertArrayEquals(new double[]{10.0, 12.0, 14.0}, interval.getTime(), 1e-6, "截取区间内的时间值");
    }

    @Test
    @DisplayName("RRi时间重置：resetTime() 从0开始")
    void testResetTimeOffset() {
        RRi rri = new RRi(FAKE_RRI, new double[]{4.0, 5.0, 6.0, 7.0});
        RRi reset = rri.resetTime();
        assertArrayEquals(FAKE_RRI, reset.getRri(), 1e-6);
        assertArrayEquals(new double[]{0.0, 1.0, 2.0, 3.0}, reset.getTime(), 1e-6, "时间数组应从0开始");
    }

    @Test
    @DisplayName("RRi时间分割：timeSplit(segSize, overlap)")
    void testSplitRriUsingTimeInformation() {
        RRi rri = new RRi(new double[]{800, 810, 790, 795}, new double[]{1, 5, 10, 20});
        List<RRi> segments = rri.timeSplit(10.0, 0.0, false);
        assertEquals(2, segments.size(), "应分为2段");
    }

    // ==================== 描述统计测试 ====================

    @Test
    @DisplayName("RRi描述统计：describe() 包含正确指标")
    void testRriDescribe() {
        RRi rri = new RRi(FAKE_RRI);
        RRiDescription descr = rri.describe();

        assertEquals(750.0, descr.getRri("min"), 1e-6, "RRi最小值");
        assertEquals(815.0, descr.getRri("max"), 1e-6, "RRi最大值");
        assertEquals(793.75, descr.getRri("mean"), 1e-6, "RRi均值");
        assertEquals(805.0, descr.getRri("median"), 1e-6, "RRi中位数");
        assertEquals(65.0, descr.getRri("amplitude"), 1e-6, "RRi振幅");

        // 心率统计
        assertEquals(73.619, descr.getHr("min"), 1e-3, "HR最小值");
        assertEquals(80.0, descr.getHr("max"), 1e-6, "HR最大值");
    }

    // ==================== RRiDetrended 测试 ====================

    @Test
    @DisplayName("RRiDetrended：去趋势标志正确设置")
    void testCreateDetrendedRri() {
        double[] vals = {-87.98, -88.22, -49.47, -109.70, -181.91};
        double[] t = {0, 1, 2, 3, 4};
        RRiDetrended det = new RRiDetrended(vals, t);
        assertTrue(det.isDetrended(), "去趋势RRi的detrended标志应为true");
        assertFalse(det.isInterpolated(), "未指定interpolated时应为false");
    }

    @Test
    @DisplayName("RRiDetrended：插值标志正确设置")
    void testRriDetrendedInterpolatedFlag() {
        double[] vals = {-20.0, 5.0, 30.0};
        double[] t = {0.0, 0.25, 0.5};
        RRiDetrended det = new RRiDetrended(vals, t, true);
        assertTrue(det.isDetrended(), "detrended应为true");
        assertTrue(det.isInterpolated(), "interpolated应为true");
    }

    // ==================== 异常输入测试 ====================

    @Test
    @DisplayName("异常输入：空数组应抛出异常")
    void testEmptyArrayThrowsException() {
        assertThrows(Exception.class, () -> {
            new RRi(new double[0]);
        }, "空RRi数组应抛出异常");
    }
}
