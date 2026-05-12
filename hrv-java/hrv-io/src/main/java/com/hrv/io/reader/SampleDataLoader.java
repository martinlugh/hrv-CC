package com.hrv.io.reader;

import com.hrv.core.rri.RRi;

import java.io.IOException;
import java.io.InputStream;

/**
 * 样本数据加载器。
 * 从classpath中加载内置样本数据文件。
 * 对应Python原项目中的 hrv/sampledata/_load.py。
 *
 * <p>包含3个样本数据集：
 * <ul>
 *   <li>rest_rri：约900秒平卧静息期RRi序列（无ectopic心拍）</li>
 *   <li>exercise_rri：约2400秒运动协议RRi序列（含少量ectopic心拍）</li>
 *   <li>noisy_rri：约2400秒运动数据（含大量ectopic心拍，用于滤波测试）</li>
 * </ul>
 */
public class SampleDataLoader {

    /** 样本数据文件在classpath中的路径前缀 */
    private static final String RESOURCE_PREFIX = "/sampledata/";

    /**
     * 加载静息RRi样本数据。
     * 约900秒（15分钟）平卧静息RRi序列，无ectopic心拍，源于窦房结。
     * 对应Python: load_rest_rri()
     *
     * @return 静息RRi序列
     * @throws IOException 若资源文件读取失败
     */
    public static RRi loadRestRri() throws IOException {
        return loadSampleData("rest_rri.txt");
    }

    /**
     * 加载运动RRi样本数据。
     * 约2400秒运动协议RRi序列，包含：
     * - 约300秒坐姿静息（运动前）
     * - 约1800秒次最大运动
     * - 约300秒被动恢复
     * 对应Python: load_exercise_rri()
     *
     * @return 运动RRi序列
     * @throws IOException 若资源文件读取失败
     */
    public static RRi loadExerciseRri() throws IOException {
        return loadSampleData("exercise_rri.hrm");
    }

    /**
     * 加载含噪声RRi样本数据。
     * 与exercise_rri相同的运动协议，但含大量ectopic心拍，
     * 适用于测试各种滤波算法。
     * 对应Python: load_noisy_rri()
     *
     * @return 含噪声运动RRi序列
     * @throws IOException 若资源文件读取失败
     */
    public static RRi loadNoisyRri() throws IOException {
        return loadSampleData("noisy_rri.hrm");
    }

    /**
     * 通用样本数据加载方法。
     * 根据文件扩展名选择对应的读取器。
     * 对应Python: load_sample_data(filename)
     *
     * @param filename 样本数据文件名（在resources/sampledata/目录下）
     * @return RRi序列对象
     * @throws IOException 若资源文件不存在或读取失败
     */
    public static RRi loadSampleData(String filename) throws IOException {
        String resourcePath = RESOURCE_PREFIX + filename;
        InputStream is = SampleDataLoader.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("样本数据资源文件未找到: " + resourcePath);
        }

        String extension = filename.substring(filename.lastIndexOf('.'));
        try {
            switch (extension.toLowerCase()) {
                case ".txt":
                    return RRiReader.readFromTextStream(is);
                case ".hrm":
                    return RRiReader.readFromHrmStream(is);
                default:
                    throw new com.hrv.core.exception.FileNotSupportedException(
                        "不支持的样本数据文件格式: " + extension
                    );
            }
        } finally {
            is.close();
        }
    }
}
