package com.hrv.tests;

import com.hrv.core.preprocessing.RRiPreprocessing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RRI数据清洗测试。
 * 移植自 https://github.com/Aura-healthcare/hrv-analysis tests/test_preprocessing_methods.py。
 * 验证每个清洗函数的正确性（与Python期望值一致）。
 */
@DisplayName("RRI数据清洗测试（Aura Healthcare 移植）")
public class PreprocessingTest {

    // ==================== removeOutliers ====================

    @Test
    @DisplayName("removeOutliers：超出[300,2000]范围的值替换为NaN")
    void testRemoveOutliersDefault() {
        double[] rri = {700, 600, 2300, 200, 1000, 230, 1200};
        double[] result = RRiPreprocessing.removeOutliers(rri);

        // 2300 > 2000 → NaN；200 < 300 → NaN；230 < 300 → NaN
        assertFalse(Double.isNaN(result[0]), "700 在范围内");
        assertFalse(Double.isNaN(result[1]), "600 在范围内");
        assertTrue (Double.isNaN(result[2]), "2300 超高，应为 NaN");
        assertTrue (Double.isNaN(result[3]), "200 超低，应为 NaN");
        assertFalse(Double.isNaN(result[4]), "1000 在范围内");
        assertTrue (Double.isNaN(result[5]), "230 超低，应为 NaN");
        assertFalse(Double.isNaN(result[6]), "1200 在范围内");
    }

    @Test
    @DisplayName("removeOutliers：自定义范围[400,1500]")
    void testRemoveOutliersCustomRange() {
        double[] rri = {300, 500, 1600, 1000};
        double[] result = RRiPreprocessing.removeOutliers(rri, 400, 1500);

        assertTrue (Double.isNaN(result[0]), "300 < 400，应为 NaN");
        assertFalse(Double.isNaN(result[1]), "500 在范围内");
        assertTrue (Double.isNaN(result[2]), "1600 > 1500，应为 NaN");
        assertFalse(Double.isNaN(result[3]), "1000 在范围内");
    }

    @Test
    @DisplayName("removeOutliers：全部在范围内，无NaN")
    void testRemoveOutliersNoOutliers() {
        double[] rri = {600, 700, 800, 900};
        double[] result = RRiPreprocessing.removeOutliers(rri);
        for (double v : result) assertFalse(Double.isNaN(v), "全部在范围内");
    }

    @Test
    @DisplayName("removeOutliers：边界值300和2000视为有效（含等于）")
    void testRemoveOutliersBoundary() {
        double[] rri = {300, 2000};
        double[] result = RRiPreprocessing.removeOutliers(rri);
        assertFalse(Double.isNaN(result[0]), "300 是下边界，应保留");
        assertFalse(Double.isNaN(result[1]), "2000 是上边界，应保留");
    }

    // ==================== interpolateNanValues ====================

    @Test
    @DisplayName("interpolateNanValues：中间NaN线性插值")
    void testInterpolateNanMiddle() {
        // 600 → NaN → 1000：线性插值 600+(1000-600)*0.5=800
        double[] rri = {600, Double.NaN, 1000};
        double[] result = RRiPreprocessing.interpolateNanValues(rri);
        assertEquals(600.0, result[0], 1e-6);
        assertEquals(800.0, result[1], 1e-6, "中间NaN应插值为800");
        assertEquals(1000.0, result[2], 1e-6);
    }

    @Test
    @DisplayName("interpolateNanValues：开头NaN前向填充")
    void testInterpolateNanFrontFill() {
        double[] rri = {Double.NaN, Double.NaN, 800, 1000};
        double[] result = RRiPreprocessing.interpolateNanValues(rri);
        // 前两个 NaN 用第一个有效值 800 填充
        assertEquals(800.0, result[0], 1e-6, "开头NaN应用800填充");
        assertEquals(800.0, result[1], 1e-6, "开头NaN应用800填充");
        assertEquals(800.0, result[2], 1e-6);
        assertEquals(1000.0, result[3], 1e-6);
    }

    @Test
    @DisplayName("interpolateNanValues：末尾NaN用最后有效值填充")
    void testInterpolateNanEndFill() {
        double[] rri = {600, 700, Double.NaN, Double.NaN};
        double[] result = RRiPreprocessing.interpolateNanValues(rri);
        assertEquals(600.0, result[0], 1e-6);
        assertEquals(700.0, result[1], 1e-6);
        assertEquals(700.0, result[2], 1e-6, "末尾NaN应用700填充");
        assertEquals(700.0, result[3], 1e-6, "末尾NaN应用700填充");
    }

    @Test
    @DisplayName("interpolateNanValues：连续多个中间NaN均匀插值")
    void testInterpolateMultipleMiddleNaN() {
        // 700 → NaN → NaN → NaN → 1100
        // 线性：700 + k*(1100-700)/4 for k=1,2,3
        double[] rri = {700, Double.NaN, Double.NaN, Double.NaN, 1100};
        double[] result = RRiPreprocessing.interpolateNanValues(rri);
        assertEquals(700.0,  result[0], 1e-6);
        assertEquals(800.0,  result[1], 1e-6, "第2点插值=800");
        assertEquals(900.0,  result[2], 1e-6, "第3点插值=900");
        assertEquals(1000.0, result[3], 1e-6, "第4点插值=1000");
        assertEquals(1100.0, result[4], 1e-6);
    }

    @Test
    @DisplayName("interpolateNanValues：无NaN，原样返回")
    void testInterpolateNoNaN() {
        double[] rri = {600, 700, 800};
        double[] result = RRiPreprocessing.interpolateNanValues(rri);
        assertArrayEquals(rri, result, 1e-6);
    }

    // ==================== removeEctopicBeats - Malik ====================

    @Test
    @DisplayName("removeEctopicBeats (Malik)：相邻差>20%标记为NaN")
    void testMalikOneSuccessiveOutlier() {
        // 对应Python: test_1_successive_outlier_malik
        double[] rri = {100, 110, 100, 130, 100, 100, 70, 100, 120, 100};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.MALIK_RULE);

        // 索引3: |100-130|=30 > 0.2*100=20 → NaN
        // 索引6: |100-70|=30 > 0.2*100=20 → NaN
        assertFalse(Double.isNaN(result[0]));
        assertFalse(Double.isNaN(result[1]));
        assertFalse(Double.isNaN(result[2]));
        assertTrue (Double.isNaN(result[3]), "索引3：130偏离100超20%，应为NaN");
        assertFalse(Double.isNaN(result[4]), "前一个是异常，当前无条件保留");
        assertFalse(Double.isNaN(result[5]));
        assertTrue (Double.isNaN(result[6]), "索引6：70偏离100超20%，应为NaN");
        assertFalse(Double.isNaN(result[7]), "前一个是异常，当前无条件保留");
        assertFalse(Double.isNaN(result[8]));
        assertFalse(Double.isNaN(result[9]));
    }

    @Test
    @DisplayName("removeEctopicBeats (Malik)：全部正常，无NaN")
    void testMalikNoOutliers() {
        double[] rri = {100, 110, 105, 108, 102};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.MALIK_RULE);
        for (double v : result) assertFalse(Double.isNaN(v));
    }

    // ==================== removeEctopicBeats - Kamath ====================

    @Test
    @DisplayName("removeEctopicBeats (Kamath)：非对称检测（增幅≤32.5%，减幅≤24.5%）")
    void testKamathOneSuccessiveOutlier() {
        // 对应Python: test_1_successive_outlier_kamath
        double[] rri = {101, 110, 100, 140, 100, 100, 70, 100, 130, 115, 100, 78};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.KAMATH_RULE);

        // 索引3: 140-100=40 > 0.325*100=32.5 → NaN（增幅超标）
        assertTrue (Double.isNaN(result[3]), "140：增幅超32.5%，应为NaN");
        // 索引4：前一个异常，无条件保留
        assertFalse(Double.isNaN(result[4]), "前一个异常，当前保留");
        // 索引6: 100-70=30 > 0.245*100=24.5 → NaN（减幅超标）
        assertTrue (Double.isNaN(result[6]), "70：减幅超24.5%，应为NaN");
        assertFalse(Double.isNaN(result[7]), "前一个异常，当前保留");
    }

    @Test
    @DisplayName("removeEctopicBeats (Kamath)：增幅恰好32.5%属有效")
    void testKamathBoundary() {
        // 100 -> 132.5：增幅=32.5%，刚好有效
        double[] rri = {100, 132, 100};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.KAMATH_RULE);
        assertFalse(Double.isNaN(result[1]), "增幅32%在32.5%以内，有效");
    }

    // ==================== removeEctopicBeats - Karlsson ====================

    @Test
    @DisplayName("removeEctopicBeats (Karlsson)：与前后均值偏差>20%标记为NaN")
    void testKarlssonOneSuccessiveOutlier() {
        // 对应Python: test_1_successive_outlier_karlsson
        double[] rri = {110, 100, 125, 100, 100, 70, 100, 130, 105, 100, 78, 100};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.KARLSSON_RULE);

        // 索引2: mean(110,100)=105, |105-125|=20, 20 >= 0.2*105=21 → 边界（< 不满足）→ NaN
        assertTrue (Double.isNaN(result[2]),  "索引2：125偏离均值超20%，应为NaN");
        // 索引5: mean(100,100)=100, |100-70|=30 >= 0.2*100=20 → NaN
        assertTrue (Double.isNaN(result[5]),  "索引5：70偏离均值超20%，应为NaN");
        // 索引7: mean(100,105)=102.5, |102.5-130|=27.5 >= 0.2*102.5=20.5 → NaN
        assertTrue (Double.isNaN(result[7]),  "索引7：130偏离均值超20%，应为NaN");
        // 索引10: mean(100,100)=100, |100-78|=22 >= 0.2*100=20 → NaN
        assertTrue (Double.isNaN(result[10]), "索引10：78偏离均值超20%，应为NaN");

        // 首尾始终保留
        assertFalse(Double.isNaN(result[0]),  "第一个元素始终保留");
        assertFalse(Double.isNaN(result[11]), "最后一个元素始终保留");
    }

    // ==================== removeEctopicBeats - ACAR ====================

    @Test
    @DisplayName("removeEctopicBeats (ACAR)：与过去9个均值偏差>20%标记为NaN")
    void testAcarOneSuccessiveOutlier() {
        // 对应Python: test_1_successive_outlier_Acer
        double[] rri = {100, 100, 100, 100, 100, 100, 100, 100, 110, 930, 110, 100, 10};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.ACAR_RULE);

        // 前9个直接保留
        for (int i = 0; i < 9; i++) assertFalse(Double.isNaN(result[i]), "前9个应保留");
        // 索引9: mean9≈102.22，|102.22-930|>>20% → NaN
        assertTrue (Double.isNaN(result[9]),  "930：与前9均值偏差>20%，应为NaN");
        // 索引10: 110正常
        assertFalse(Double.isNaN(result[10]), "110：正常");
        // 索引12: mean≈105, |105-10|=95>>21 → NaN
        assertTrue (Double.isNaN(result[12]), "10：与前9均值偏差>20%，应为NaN");
    }

    @Test
    @DisplayName("removeEctopicBeats (ACAR)：前9个始终保留，不管偏差多大")
    void testAcarFirst9AlwaysKept() {
        double[] rri = {100, 9999, 100, 9999, 100, 9999, 100, 9999, 100};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.ACAR_RULE);
        for (int i = 0; i < 9; i++) assertFalse(Double.isNaN(result[i]), "前9个应保留");
    }

    // ==================== Custom 方法 ====================

    @Test
    @DisplayName("removeEctopicBeats (Custom)：自定义10%阈值")
    void testCustomRule() {
        // 10%阈值：|100-115|=15 > 0.1*100=10 → NaN
        double[] rri = {100, 115, 100};
        double[] result = RRiPreprocessing.removeEctopicBeats(rri, RRiPreprocessing.CUSTOM_RULE, 0.1);
        assertTrue(Double.isNaN(result[1]), "115偏离100超10%，custom阈值下应为NaN");
    }

    @Test
    @DisplayName("removeEctopicBeats：非法方法名应抛出异常")
    void testInvalidMethodThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            RRiPreprocessing.removeEctopicBeats(new double[]{800, 810}, "invalid_method")
        );
    }

    // ==================== getNnIntervals（完整Pipeline）====================

    @Test
    @DisplayName("getNnIntervals：完整Pipeline，对应Python test_if_get_nn_intervals_creates_right_nn_intervals")
    void testGetNnIntervalsPipeline() {
        // 对应Python测试用例
        double[] rri = {700, 600, 2300, 1000, 1000, 230, 1200};
        double[] result = RRiPreprocessing.getNnIntervals(rri);

        // 步骤1: [700,600,NaN,1000,1000,NaN,1200]
        // 步骤2: 线性插值 → [700,600,800,1000,1000,1100,1200]
        // 步骤3: kamath检测 → 全部有效（800/600增幅=33.3%>32.5% → NaN?）
        // 步骤4: 再次插值
        assertEquals(7, result.length, "结果应与输入等长");
        // 首末端保留
        assertEquals(700.0,  result[0], 1e-6);
        assertEquals(1200.0, result[6], 1e-6);
        // 无 NaN
        for (double v : result) assertFalse(Double.isNaN(v), "最终结果不应有NaN");
    }

    @Test
    @DisplayName("getNnIntervals：无异常值序列，结果与输入相同")
    void testGetNnIntervalsCleanInput() {
        double[] rri = {800, 810, 820, 815, 805, 800};
        double[] result = RRiPreprocessing.getNnIntervals(rri);
        assertArrayEquals(rri, result, 1e-6, "干净序列经pipeline后应不变");
    }

    @Test
    @DisplayName("getNnIntervals：指定Malik方法")
    void testGetNnIntervalsMalik() {
        double[] rri = {800, 600, 800, 810, 800};
        double[] result = RRiPreprocessing.getNnIntervals(rri, RRiPreprocessing.MALIK_RULE);
        assertEquals(5, result.length);
        for (double v : result) assertFalse(Double.isNaN(v), "最终不应有NaN");
    }

    // ==================== isValidSample ====================

    @Test
    @DisplayName("isValidSample：样本量<240应返回false")
    void testIsValidSampleTooShort() {
        double[] nn = new double[100];
        assertFalse(RRiPreprocessing.isValidSample(nn, 0), "100点<240，应无效");
    }

    @Test
    @DisplayName("isValidSample：异常比例>4%应返回false")
    void testIsValidSampleTooManyOutliers() {
        double[] nn = new double[300];
        int outlierCount = 20; // 20/300 ≈ 6.7% > 4%
        assertFalse(RRiPreprocessing.isValidSample(nn, outlierCount), "异常>4%，应无效");
    }

    @Test
    @DisplayName("isValidSample：240点且异常<4%应返回true")
    void testIsValidSampleValid() {
        double[] nn = new double[300];
        int outlierCount = 10; // 10/300 ≈ 3.3% < 4%
        assertTrue(RRiPreprocessing.isValidSample(nn, outlierCount), "满足条件，应有效");
    }

    // ==================== countNaN ====================

    @Test
    @DisplayName("countNaN：正确统计NaN数量")
    void testCountNaN() {
        double[] arr = {1.0, Double.NaN, 3.0, Double.NaN, 5.0};
        assertEquals(2, RRiPreprocessing.countNaN(arr));
    }

    @Test
    @DisplayName("countNaN：无NaN时返回0")
    void testCountNaNZero() {
        double[] arr = {1.0, 2.0, 3.0};
        assertEquals(0, RRiPreprocessing.countNaN(arr));
    }
}
