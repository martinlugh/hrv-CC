package com.hrv.tests;

import com.hrv.core.exception.EmptyFileException;
import com.hrv.core.rri.RRi;
import com.hrv.io.reader.RRiReader;
import com.hrv.io.reader.SampleDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * IO模块测试。
 * 对应Python原项目 tests/test_io.py 和 tests/test_load_sampledata.py。
 * 验证TXT/HRM/CSV格式文件读取和样本数据加载功能。
 */
@DisplayName("IO模块测试")
public class IoTest {

    @TempDir
    Path tempDir;

    // ==================== TXT格式读取测试 ====================

    @Test
    @DisplayName("读取TXT文件：每行一个RRi值")
    void testReadFromText() throws IOException {
        File f = tempDir.resolve("test.txt").toFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("800\n810\n820\n830\n");
        }
        RRi rri = RRiReader.readFromText(f.getAbsolutePath());
        assertArrayEquals(new double[]{800, 810, 820, 830}, rri.getRri(), 1e-6);
        assertInstanceOf(RRi.class, rri);
    }

    @Test
    @DisplayName("读取TXT文件：空文件应抛出EmptyFileException")
    void testReadFromTextEmptyFile() throws IOException {
        File f = tempDir.resolve("empty.txt").toFile();
        f.createNewFile();
        assertThrows(EmptyFileException.class, () -> {
            RRiReader.readFromText(f.getAbsolutePath());
        }, "空文件应抛出EmptyFileException");
    }

    @Test
    @DisplayName("读取TXT文件：时间由cumsum自动生成")
    void testReadFromTextAutoGeneratesTime() throws IOException {
        File f = tempDir.resolve("test_time.txt").toFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("1000\n1000\n1000\n");
        }
        RRi rri = RRiReader.readFromText(f.getAbsolutePath());
        // time = cumsum([1000,1000,1000])/1000 - 1.0 = [0, 1, 2]
        double[] expectedTime = {0.0, 1.0, 2.0};
        assertArrayEquals(expectedTime, rri.getTime(), 1e-6, "时间应由cumsum自动生成");
    }

    // ==================== HRM格式读取测试 ====================

    @Test
    @DisplayName("读取HRM文件：从[HRData]节提取数据")
    void testReadFromHrm() throws IOException {
        File f = tempDir.resolve("test.hrm").toFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("[HRData]\n");
            fw.write("800\n810\n820\n");
        }
        RRi rri = RRiReader.readFromHrm(f.getAbsolutePath());
        assertArrayEquals(new double[]{800, 810, 820}, rri.getRri(), 1e-6);
    }

    @Test
    @DisplayName("读取HRM文件：缺少[HRData]节应抛出EmptyFileException")
    void testReadFromHrmMissingHrData() throws IOException {
        File f = tempDir.resolve("bad.hrm").toFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("[Params]\nVersion=106\n");
        }
        assertThrows(EmptyFileException.class, () -> {
            RRiReader.readFromHrm(f.getAbsolutePath());
        }, "缺少[HRData]节应抛出EmptyFileException");
    }

    // ==================== CSV格式读取测试 ====================

    @Test
    @DisplayName("读取CSV文件：逗号分隔，第0列为RRi")
    void testReadFromCsvDefault() throws IOException {
        File f = tempDir.resolve("test.csv").toFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("800\n810\n820\n830\n");
        }
        RRi rri = RRiReader.readFromCsv(f.getAbsolutePath());
        assertArrayEquals(new double[]{800, 810, 820, 830}, rri.getRri(), 1e-6);
    }

    @Test
    @DisplayName("读取CSV文件：指定时间列")
    void testReadFromCsvWithTimeColumn() throws IOException {
        File f = tempDir.resolve("test_time.csv").toFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("0,800\n1,810\n2,820\n");
        }
        RRi rri = RRiReader.readFromCsv(f.getAbsolutePath(), 1, 0, 0, ",");
        assertArrayEquals(new double[]{800, 810, 820}, rri.getRri(), 1e-6, "第1列为RRi值");
        assertArrayEquals(new double[]{0.0, 1.0, 2.0}, rri.getTime(), 1e-6, "第0列为时间值");
    }

    @Test
    @DisplayName("读取CSV文件：跳过标题行（rowOffset=1）")
    void testReadFromCsvWithRowOffset() throws IOException {
        File f = tempDir.resolve("header.csv").toFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("rri_ms\n800\n810\n820\n");
        }
        RRi rri = RRiReader.readFromCsv(f.getAbsolutePath(), 0, -1, 1, null);
        assertArrayEquals(new double[]{800, 810, 820}, rri.getRri(), 1e-6, "跳过标题行");
    }

    // ==================== 样本数据加载测试 ====================

    @Test
    @DisplayName("样本数据：loadRestRri() 成功加载")
    void testLoadRestRri() throws IOException {
        RRi rri = SampleDataLoader.loadRestRri();
        assertNotNull(rri, "loadRestRri()不应返回null");
        assertTrue(rri.size() > 0, "静息RRi序列长度应大于0");
        // Python原项目：first value = 1114ms
        assertEquals(1114.0, rri.get(0), 0.01, "静息RRi序列首值应为1114ms");
    }

    @Test
    @DisplayName("样本数据：loadNoisyRri() 成功加载")
    void testLoadNoisyRri() throws IOException {
        RRi rri = SampleDataLoader.loadNoisyRri();
        assertNotNull(rri, "loadNoisyRri()不应返回null");
        assertTrue(rri.size() > 0, "噪声RRi序列长度应大于0");
        // Python原项目：first value = 904ms
        assertEquals(904.0, rri.get(0), 0.01, "噪声RRi序列首值应为904ms");
    }

    @Test
    @DisplayName("样本数据：loadExerciseRri() 成功加载")
    void testLoadExerciseRri() throws IOException {
        RRi rri = SampleDataLoader.loadExerciseRri();
        assertNotNull(rri, "loadExerciseRri()不应返回null");
        assertTrue(rri.size() > 0, "运动RRi序列长度应大于0");
    }

    @Test
    @DisplayName("样本数据：rest_rri 时域分析与Python原项目期望值一致")
    void testRestRriTimeDomainConsistency() throws IOException {
        RRi restRri = SampleDataLoader.loadRestRri();
        com.hrv.core.classical.TimeDomainResult result =
            com.hrv.core.classical.ClassicalAnalysis.timeDomain(restRri);

        // Python原项目文档中的期望值：
        // {'rmssd': 55.13744203126742, 'sdnn': 57.81817771970009, ...
        //  'mrri': 1058.7186813186813, 'mhr': 56.85278105637358}
        assertEquals(55.137, result.getRmssd(), 0.01, "rest_rri RMSSD与Python期望值一致");
        assertEquals(57.818, result.getSdnn(), 0.01, "rest_rri SDNN与Python期望值一致");
        assertEquals(1058.719, result.getMrri(), 0.01, "rest_rri MRRI与Python期望值一致");
        assertEquals(56.853, result.getMhr(), 0.01, "rest_rri MHR与Python期望值一致");
    }

    @Test
    @DisplayName("样本数据：rest_rri 非线性分析与Python原项目期望值一致")
    void testRestRriNonLinearConsistency() throws IOException {
        RRi restRri = SampleDataLoader.loadRestRri();
        com.hrv.core.classical.NonLinearResult nl =
            com.hrv.core.classical.ClassicalAnalysis.nonLinear(restRri);

        // Python原项目文档中的期望值：
        // {'sd1': 39.00945528912225, 'sd2': 71.86199098062633}
        assertEquals(39.009, nl.getSd1(), 0.01, "rest_rri SD1与Python期望值一致");
        assertEquals(71.862, nl.getSd2(), 0.01, "rest_rri SD2与Python期望值一致");
    }
}
