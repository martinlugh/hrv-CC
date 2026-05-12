package com.hrv.io.reader;

import com.hrv.core.exception.EmptyFileException;
import com.hrv.core.rri.RRi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RRi序列文件读取器。
 * 支持从文本文件(.txt)、Polar HRM格式(.hrm)和CSV文件(.csv)读取RRi序列。
 * 对应Python原项目中的 hrv/io.py。
 *
 * <p>所有读取方法返回 {@link RRi} 对象，时间数组由cumsum(rri)/1000自动生成。
 */
public class RRiReader {

    /**
     * 从文本文件读取RRi序列。
     * 文件格式：每行一个RRi值（毫秒）。
     * 对应Python: read_from_text(pathname)
     *
     * @param pathname 文件路径
     * @return RRi序列对象
     * @throws EmptyFileException 若文件为空
     * @throws IOException        若文件读取失败
     */
    public static RRi readFromText(String pathname) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(pathname))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String fileContent = content.toString().trim();
        if (fileContent.isEmpty()) {
            throw new EmptyFileException("empty file!");
        }

        // 解析数字：匹配整数和小数（对应Python: re.findall(r"\d\.?[0-9]+", ...)）
        List<Double> values = extractNumbers(fileContent);
        if (values.isEmpty()) {
            throw new EmptyFileException("empty file!");
        }

        return new RRi(toDoubleArray(values));
    }

    /**
     * 从InputStream读取文本格式的RRi序列（用于资源文件加载）。
     *
     * @param is 输入流
     * @return RRi序列对象
     * @throws EmptyFileException 若内容为空
     * @throws IOException        若读取失败
     */
    public static RRi readFromTextStream(InputStream is) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        String fileContent = content.toString().trim();
        if (fileContent.isEmpty()) {
            throw new EmptyFileException("empty file!");
        }

        List<Double> values = extractNumbers(fileContent);
        if (values.isEmpty()) {
            throw new EmptyFileException("empty file!");
        }

        return new RRi(toDoubleArray(values));
    }

    /**
     * 从Polar HRM格式文件读取RRi序列。
     * HRM文件格式：在[HRData]节点之后包含RRi数据。
     * 对应Python: read_from_hrm(pathname)
     *
     * <p>参考：https://www.polar.com/sites/default/files/Polar_HRM_file%20format.pdf
     *
     * @param pathname 文件路径（.hrm扩展名）
     * @return RRi序列对象
     * @throws EmptyFileException 若文件无[HRData]节或数据为空
     * @throws IOException        若文件读取失败
     */
    public static RRi readFromHrm(String pathname) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(pathname))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return parseHrmContent(content.toString());
    }

    /**
     * 从InputStream读取HRM格式的RRi序列（用于资源文件加载）。
     */
    public static RRi readFromHrmStream(InputStream is) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return parseHrmContent(content.toString());
    }

    /**
     * 解析HRM文件内容。
     * 找到[HRData]节，提取其后的所有数字。
     */
    private static RRi parseHrmContent(String fileContent) {
        int hrdataIndex = fileContent.indexOf("[HRData]");
        if (hrdataIndex < 0) {
            throw new EmptyFileException("empty file!");
        }

        // 提取[HRData]之后的内容
        String hrdataSection = fileContent.substring(hrdataIndex);
        // 过滤掉下一个节（[...）之前的内容
        int nextSection = hrdataSection.indexOf('[', 1);
        if (nextSection > 0) {
            hrdataSection = hrdataSection.substring(0, nextSection);
        }

        // 提取所有数字（对应Python: re.findall(r"\d+", ...)）
        List<Double> values = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(hrdataSection.substring("[HRData]".length()));
        while (matcher.find()) {
            values.add(Double.parseDouble(matcher.group()));
        }

        if (values.isEmpty()) {
            throw new EmptyFileException("empty file!");
        }

        return new RRi(toDoubleArray(values));
    }

    /**
     * 从CSV文件读取RRi序列。
     * 对应Python: read_from_csv(pathname, rri_col_index, time_col_index, row_offset, time_parser, sep)
     *
     * @param pathname      文件路径
     * @param rriColIndex   RRi数据所在列的索引（默认0）
     * @param timeColIndex  时间数据所在列的索引（-1表示无时间列，自动生成时间）
     * @param rowOffset     跳过的行数（用于处理文件头，默认0）
     * @param sep           列分隔符（null时自动检测）
     * @return RRi序列对象
     * @throws IOException 若文件读取失败
     */
    public static RRi readFromCsv(String pathname, int rriColIndex, int timeColIndex,
                                   int rowOffset, String sep) throws IOException {
        List<String[]> rows = new ArrayList<>();
        String delimiter = sep;

        // 读取全部行
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(pathname))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        // 自动检测分隔符（使用第一行）
        if (delimiter == null && !lines.isEmpty()) {
            delimiter = detectDelimiter(lines.get(0));
        }
        if (delimiter == null) delimiter = ",";

        // 跳过前rowOffset行
        for (int i = rowOffset; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                rows.add(line.split(Pattern.quote(delimiter)));
            }
        }

        List<Double> rriValues = new ArrayList<>();
        List<Double> timeValues = new ArrayList<>();

        for (String[] row : rows) {
            if (rriColIndex < row.length) {
                rriValues.add(Double.parseDouble(row[rriColIndex].trim()));
            }
            if (timeColIndex >= 0 && timeColIndex < row.length) {
                timeValues.add(Double.parseDouble(row[timeColIndex].trim()));
            }
        }

        if (rriValues.isEmpty()) {
            throw new EmptyFileException("CSV文件中未找到RRi数据");
        }

        double[] rriArr = toDoubleArray(rriValues);
        if (timeColIndex >= 0 && !timeValues.isEmpty()) {
            return new RRi(rriArr, toDoubleArray(timeValues));
        }
        return new RRi(rriArr);
    }

    /** 简化版CSV读取（使用默认参数） */
    public static RRi readFromCsv(String pathname) throws IOException {
        return readFromCsv(pathname, 0, -1, 0, null);
    }

    /**
     * 自动检测CSV文件分隔符（简化版，检测常见分隔符）。
     */
    private static String detectDelimiter(String sampleLine) {
        // 按出现次数最多的分隔符判断
        String[] candidates = {",", ";", "\t", "|", " "};
        String best = ",";
        int bestCount = 0;
        for (String sep : candidates) {
            int count = sampleLine.split(Pattern.quote(sep), -1).length - 1;
            if (count > bestCount) {
                bestCount = count;
                best = sep;
            }
        }
        return bestCount > 0 ? best : ",";
    }

    /**
     * 从字符串中提取数字（整数或小数）。
     * 对应Python: re.findall(r"\d\.?[0-9]+", file_content)
     */
    private static List<Double> extractNumbers(String text) {
        List<Double> values = new ArrayList<>();
        // 匹配 "数字.数字" 或 "多位数字"（原Python正则: \d\.?[0-9]+）
        Pattern pattern = Pattern.compile("\\d\\.?[0-9]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                values.add(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        return values;
    }

    private static double[] toDoubleArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
