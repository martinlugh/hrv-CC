package com.hrv.tests;

import com.hrv.core.nonstationary.NonStationaryAnalysis;
import com.hrv.core.nonstationary.TimeVaryingResult;
import com.hrv.core.rri.RRi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 非平稳HRV分析测试。
 * 对应Python原项目 tests/test_nonstationary.py。
 * 验证时变HRV分析的正确性。
 */
@DisplayName("非平稳HRV分析测试")
public class NonStationaryTest {

    @Test
    @DisplayName("时变分析：正向测试 - 按指定窗口切割并计算时域指标")
    void testTimeVaryingBasic() {
        // 创建一个61点（0-60秒）的模拟RRi序列，总时长60秒
        double[] rriVals = new double[61];
        double[] timeVals = new double[61];
        for (int i = 0; i < 61; i++) {
            rriVals[i] = 800.0 + (i % 10) * 5.0; // 800~845ms的循环序列
            timeVals[i] = i * 1.0; // 每秒一个点，0到60秒
        }

        RRi rri = new RRi(rriVals, timeVals);
        TimeVaryingResult result = NonStationaryAnalysis.timeVarying(rri, 20.0, 0.0);

        assertNotNull(result, "时变分析结果不应为null");
        assertFalse(result.getResults().isEmpty(), "应有至少1个时间窗口的结果");
        assertEquals(3, result.getResults().size(), "61点（0-60秒）序列以20秒窗口应分为3段");
    }

    @Test
    @DisplayName("时变分析：各窗口时域指标键完整")
    void testTimeVaryingResultKeys() {
        double[] rriVals = createTestRri(50, 800.0);
        double[] timeVals = createTimeArr(50, 1.5);
        RRi rri = new RRi(rriVals, timeVals);

        TimeVaryingResult result = NonStationaryAnalysis.timeVarying(rri, 20.0, 0.0);

        // 验证所有指标都存在
        assertNotNull(result.getIndex("rmssd"), "应有rmssd指标");
        assertNotNull(result.getIndex("sdnn"), "应有sdnn指标");
        assertNotNull(result.getIndex("sdsd"), "应有sdsd指标");
        assertNotNull(result.getIndex("nn50"), "应有nn50指标");
        assertNotNull(result.getIndex("pnn50"), "应有pnn50指标");
        assertNotNull(result.getIndex("mrri"), "应有mrri指标");
        assertNotNull(result.getIndex("mhr"), "应有mhr指标");
    }

    @Test
    @DisplayName("时变分析：X轴（各段中心时间）长度与结果数量一致")
    void testTimeVaryingXaxis() {
        double[] rriVals = createTestRri(50, 800.0);
        double[] timeVals = createTimeArr(50, 1.5);
        RRi rri = new RRi(rriVals, timeVals);

        TimeVaryingResult result = NonStationaryAnalysis.timeVarying(rri, 20.0, 0.0);
        List<Double> xaxis = result.buildXaxis();

        assertEquals(result.getResults().size(), xaxis.size(),
            "X轴点数应与时间窗口数量一致");
        for (Double t : xaxis) {
            assertTrue(t >= 0, "X轴时间值应为非负数");
        }
    }

    @Test
    @DisplayName("时变分析：seg_size和overlap正确存储")
    void testTimeVaryingSegSizeAndOverlap() {
        double[] rriVals = createTestRri(50, 800.0);
        double[] timeVals = createTimeArr(50, 1.5);
        RRi rri = new RRi(rriVals, timeVals);

        TimeVaryingResult result = NonStationaryAnalysis.timeVarying(rri, 20.0, 5.0);
        assertEquals(20.0, result.getSegSize(), 1e-6, "seg_size应被正确存储");
        assertEquals(5.0, result.getOverlap(), 1e-6, "overlap应被正确存储");
    }

    @Test
    @DisplayName("时变分析：ylabel_mapper返回正确标签")
    void testYlabelMapper() {
        TimeVaryingResult result = NonStationaryAnalysis.timeVarying(
            new RRi(createTestRri(50, 800.0), createTimeArr(50, 1.5)),
            20.0, 0.0
        );
        assertEquals("RMSSD (ms)", result.ylabelMapper("rmssd"));
        assertEquals("SDNN (ms²)", result.ylabelMapper("sdnn"));
        assertEquals("mean HR (bpm)", result.ylabelMapper("mhr"));
        assertEquals("mean RRi (ms)", result.ylabelMapper("mrri"));
    }

    @Test
    @DisplayName("时变分析：toString格式正确")
    void testTimeVaryingToString() {
        TimeVaryingResult result = NonStationaryAnalysis.timeVarying(
            new RRi(createTestRri(50, 800.0), createTimeArr(50, 1.5)),
            20.0, 0.0
        );
        String str = result.toString();
        assertTrue(str.contains("20.0"), "toString应包含seg_size");
        assertTrue(str.contains("0.0"), "toString应包含overlap");
    }

    @Test
    @DisplayName("时变分析：带重叠的窗口切割产生更多段")
    void testTimeVaryingWithOverlap() {
        double[] rriVals = createTestRri(50, 800.0);
        double[] timeVals = createTimeArr(50, 1.5);
        RRi rri = new RRi(rriVals, timeVals);

        TimeVaryingResult noOverlap = NonStationaryAnalysis.timeVarying(rri, 20.0, 0.0);
        TimeVaryingResult withOverlap = NonStationaryAnalysis.timeVarying(rri, 20.0, 10.0);

        assertTrue(withOverlap.getResults().size() >= noOverlap.getResults().size(),
            "有重叠时段数应 >= 无重叠时段数");
    }

    @Test
    @DisplayName("时变分析：异常输入 - seg_size超过序列总时长应抛异常")
    void testTimeVaryingSegSizeTooLarge() {
        RRi rri = new RRi(new double[]{800, 810, 820}, new double[]{0, 1, 2});
        assertThrows(Exception.class, () -> {
            NonStationaryAnalysis.timeVarying(rri, 100.0, 0.0);
        }, "seg_size超过序列总时长应抛出异常");
    }

    @Test
    @DisplayName("时变分析：未知指标名应抛异常")
    void testInvalidIndexThrowsException() {
        TimeVaryingResult result = NonStationaryAnalysis.timeVarying(
            new RRi(createTestRri(50, 800.0), createTimeArr(50, 1.5)),
            20.0, 0.0
        );
        assertThrows(IllegalArgumentException.class, () -> {
            result.getIndex("nonexistent_index");
        }, "未知指标名应抛出IllegalArgumentException");
    }

    // ==================== 辅助方法 ====================

    private double[] createTestRri(int n, double base) {
        double[] rri = new double[n];
        for (int i = 0; i < n; i++) {
            rri[i] = base + (i % 5) * 10.0;
        }
        return rri;
    }

    private double[] createTimeArr(int n, double interval) {
        double[] time = new double[n];
        for (int i = 0; i < n; i++) {
            time[i] = i * interval;
        }
        return time;
    }
}
