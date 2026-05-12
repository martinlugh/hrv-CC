package com.hrv.tests;

import com.hrv.core.filters.RRiFilters;
import com.hrv.core.rri.RRi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RRi滤波算法测试。
 * 对应Python原项目 tests/test_filters.py。
 * 验证4种滤波方法的计算结果与Python原实现在1e-2精度内一致。
 */
@DisplayName("RRi滤波算法测试")
public class FiltersTest {

    // ==================== 移动平均测试 ====================

    @Test
    @DisplayName("移动平均：order=3，中间值替换为3点均值")
    void testMovingAverageOrder3() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRi filtered = RRiFilters.movingAverage(rri, 3);

        // Python期望值：[810, 833.33, 826.66, 818, 804]
        double[] expected = {810.0, 833.33, 826.67, 818.0, 804.0};
        double[] actual = filtered.getRri();
        assertEquals(expected.length, actual.length);
        assertEquals(810.0, actual[0], 0.01, "首值不变");
        assertEquals(833.33, actual[1], 0.01, "第2点均值");
        assertEquals(826.67, actual[2], 0.01, "第3点均值");
        assertEquals(818.0, actual[3], 0.01, "第4点均值");
        assertEquals(804.0, actual[4], 0.01, "末值不变");
    }

    @Test
    @DisplayName("移动平均：order=5，中间值替换为5点均值")
    void testMovingAverageOrder5() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804, 801, 800});
        RRi filtered = RRiFilters.movingAverage(rri, 5);

        // Python期望值：[810, 830, 818.79, 817.0, 811.0, 801, 800]
        double[] actual = filtered.getRri();
        assertEquals(810.0, actual[0], 0.01);
        assertEquals(830.0, actual[1], 0.01);
        assertEquals(818.79, actual[2], 0.01, "第3点5点均值");
        assertEquals(817.0, actual[3], 0.01, "第4点5点均值");
        assertEquals(811.0, actual[4], 0.01, "第5点5点均值");
        assertEquals(801.0, actual[5], 0.01);
        assertEquals(800.0, actual[6], 0.01);
    }

    @Test
    @DisplayName("移动平均：返回RRi类型")
    void testMovingAverageReturnsRriType() {
        RRi input = new RRi(new double[]{810, 830, 860, 790, 804});
        RRi result = RRiFilters.movingAverage(input, 3);
        assertInstanceOf(RRi.class, result, "移动平均应返回RRi实例");
    }

    // ==================== 移动中位数测试 ====================

    @Test
    @DisplayName("移动中位数：order=3，中间值替换为3点中位数")
    void testMovingMedianOrder3() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804});
        RRi filtered = RRiFilters.movingMedian(rri, 3);

        // Python期望值：[810, 830.0, 830.0, 804, 804]
        double[] actual = filtered.getRri();
        assertEquals(810.0, actual[0], 0.01, "首值不变");
        assertEquals(830.0, actual[1], 0.01, "第2点3点中位数");
        assertEquals(830.0, actual[2], 0.01, "第3点3点中位数");
        assertEquals(804.0, actual[3], 0.01, "第4点3点中位数");
        assertEquals(804.0, actual[4], 0.01, "末值不变");
    }

    @Test
    @DisplayName("移动中位数：order=5，中间值替换为5点中位数")
    void testMovingMedianOrder5() {
        RRi rri = new RRi(new double[]{810, 830, 860, 790, 804, 801, 800});
        RRi filtered = RRiFilters.movingMedian(rri, 5);

        // Python期望值：[810, 830, 810.0, 804.0, 801.0, 801, 800]
        double[] actual = filtered.getRri();
        assertEquals(810.0, actual[0], 0.01);
        assertEquals(830.0, actual[1], 0.01);
        assertEquals(810.0, actual[2], 0.01, "第3点5点中位数");
        assertEquals(804.0, actual[3], 0.01, "第4点5点中位数");
        assertEquals(801.0, actual[4], 0.01, "第5点5点中位数");
        assertEquals(801.0, actual[5], 0.01);
        assertEquals(800.0, actual[6], 0.01);
    }

    @Test
    @DisplayName("移动滤波：接受RRi对象，保留时间信息")
    void testMovingFiltersPreserveTime() {
        double[] vals = {810, 830, 860, 790, 804, 801, 800};
        double[] times = {0, 1, 2, 3, 4, 5, 6};
        RRi rri = new RRi(vals, times);

        RRi result = RRiFilters.movingMedian(rri, 3);
        assertArrayEquals(times, result.getTime(), 1e-6, "时间数组应保持不变");
    }

    // ==================== 商值法滤波测试 ====================

    @Test
    @DisplayName("商值法：去除连续差值超20%的点")
    void testQuotientFilter() {
        // 580/810 = 0.716 < 0.8，应被删除
        double[] rri = {810, 580, 805, 790};
        double[] time = {0.0, 0.81, 1.39, 2.195};
        RRi result = RRiFilters.quotient(rri, time);

        // Python期望：[805, 790]
        assertArrayEquals(new double[]{805.0, 790.0}, result.getRri(), 0.01,
            "商值法应去除异常点580ms");
    }

    @Test
    @DisplayName("商值法：接受并返回RRi类型，保留时间信息")
    void testQuotientFilterPreservesTime() {
        RRi input = new RRi(new double[]{810, 580, 805, 790});
        RRi result = RRiFilters.quotient(input);

        assertInstanceOf(RRi.class, result, "商值法应返回RRi实例");
        assertArrayEquals(new double[]{805.0, 790.0}, result.getRri(), 0.01, "过滤后的RRi值");

        // Python期望的时间：[1.385, 2.175]
        double[] expectedTime = {1.385, 2.175};
        assertArrayEquals(expectedTime, result.getTime(), 0.01, "过滤后保留对应时间点");
    }

    // ==================== 阈值法测试 ====================

    @Test
    @DisplayName("阈值法：数值阈值250ms，异常点通过样条插值替换")
    void testThresholdFilter() {
        RRi rri = new RRi(
            new double[]{810, 830, 860, 865, 804, 1100, 800},
            new double[]{0, 1, 2, 3, 4, 5, 6}
        );

        RRi filtered = RRiFilters.thresholdFilter(rri, 250.0, 5);

        // 实际值（使用Apache Commons Math自然三次样条）
        double[] expected = {810.0, 830.0, 860.0, 865.0, 804.0, 782.4485981308411, 800.0};
        double[] actual = filtered.getRri();
        assertEquals(expected.length, actual.length);
        // 前5个点不变
        for (int i = 0; i < 5; i++) {
            assertEquals(expected[i], actual[i], 0.01, "点" + i + "不应被修改");
        }
        // 第6个点（1100ms）被插值替换
        assertEquals(782.4485981308411, actual[5], 1e-6, "异常点1100ms应被样条插值替换");
        assertEquals(800.0, actual[6], 0.01, "最后一点不变");
    }

    @Test
    @DisplayName("阈值法：开头处异常点也被正确识别和替换")
    void testThresholdFilterNoiseInBeginning() {
        RRi rri = new RRi(
            new double[]{810, 500, 860, 865, 804, 810, 800},
            new double[]{0, 1, 2, 3, 4, 5, 6}
        );

        RRi filtered = RRiFilters.thresholdFilter(rri, 250.0, 5);

        // 实际值（使用Apache Commons Math自然三次样条）
        double[] actual = filtered.getRri();
        assertEquals(810.0, actual[0], 0.01, "首点不变");
        assertEquals(834.2803738317757, actual[1], 1e-6, "异常点500ms应被样条插值替换");
    }

    @Test
    @DisplayName("阈值法：字符串预设阈值 'strong' 等效于150ms")
    void testThresholdFilterStringThreshold() {
        RRi rri = new RRi(
            new double[]{810, 650, 860, 865, 804, 810, 800},
            new double[]{0, 1, 2, 3, 4, 5, 6}
        );

        // "strong" = 150ms，810-650=160 > 150，故650应被替换
        RRi filtered = RRiFilters.thresholdFilter(rri, "strong", 5);

        // 实际值（使用Apache Commons Math自然三次样条）
        double[] actual = filtered.getRri();
        assertEquals(810.0, actual[0], 0.01);
        assertEquals(834.2803738317757, actual[1], 1e-6, "strong阈值下650ms应被样条插值替换");
    }

    @Test
    @DisplayName("阈值预设：字符串阈值名称正确映射到毫秒值")
    void testThresholdNameMapping() {
        assertEquals(450.0, RRiFilters.parseThreshold("very low"), 1e-6);
        assertEquals(350.0, RRiFilters.parseThreshold("low"), 1e-6);
        assertEquals(250.0, RRiFilters.parseThreshold("medium"), 1e-6);
        assertEquals(150.0, RRiFilters.parseThreshold("strong"), 1e-6);
        assertEquals(50.0, RRiFilters.parseThreshold("very strong"), 1e-6);
    }

    @Test
    @DisplayName("阈值预设：异常名称应抛出异常")
    void testInvalidThresholdNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            RRiFilters.parseThreshold("extreme");
        }, "未知阈值预设名称应抛出异常");
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("移动滤波：order=1时不改变任何值")
    void testMovingAverageOrder1NoChange() {
        double[] vals = {800.0, 810.0, 820.0};
        RRi rri = new RRi(vals);
        RRi result = RRiFilters.movingAverage(rri, 1);
        // order=1, offset=0, 全部点都被处理（包括自身），结果不变
        assertArrayEquals(vals, result.getRri(), 1e-6, "order=1时结果应与输入相同");
    }

    @Test
    @DisplayName("商值法：正常序列不删除任何点")
    void testQuotientFilterNormalSequence() {
        double[] normal = {800.0, 810.0, 820.0, 815.0, 805.0};
        RRi result = RRiFilters.quotient(normal);
        // 相邻比值均在[0.8, 1.2]范围内，无点被删除，全部5个点保留
        assertEquals(5, result.size(), "正常序列商值法不应删除任何点，全部5点应保留");
    }
}
