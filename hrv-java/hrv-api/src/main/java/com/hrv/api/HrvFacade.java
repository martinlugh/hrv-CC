package com.hrv.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrv.core.classical.ClassicalAnalysis;
import com.hrv.core.classical.FrequencyDomainResult;
import com.hrv.core.classical.NonLinearResult;
import com.hrv.core.classical.TimeDomainResult;
import com.hrv.core.detrend.Detrend;
import com.hrv.core.filters.RRiFilters;
import com.hrv.core.nonstationary.NonStationaryAnalysis;
import com.hrv.core.nonstationary.TimeVaryingResult;
import com.hrv.core.rri.RRi;
import com.hrv.core.rri.RRiDetrended;
import com.hrv.core.utils.RRiUtils;
import com.hrv.io.dto.HrvAnalysisRequest;
import com.hrv.io.dto.HrvAnalysisResponse;
import com.hrv.io.reader.RRiReader;
import com.hrv.visualization.ChartGenerator;
import com.hrv.visualization.ChartOutput;
import com.hrv.visualization.ChartRequest;

import java.io.IOException;

/**
 * HRV分析统一Facade接口。
 * 提供JSON字符串输入/输出的完整HRV分析流程。
 * 整合了核心算法、IO、可视化等所有模块。
 *
 * <p>使用示例：
 * <pre>
 *   HrvFacade facade = new HrvFacade();
 *
 *   // JSON输入分析
 *   String json = "{\"rri\": [800, 810, 790, 820], \"analyses\": [\"time_domain\", \"non_linear\"]}";
 *   String result = facade.analyzeJson(json);
 *
 *   // 从文件加载并分析
 *   String result2 = facade.analyzeFile("/path/to/rri.txt", "time_domain");
 *
 *   // 生成图表
 *   String chart = facade.generateChart("{\"chart_type\": \"rri\", \"rri\": [800, 810, ...]}");
 * </pre>
 */
public class HrvFacade {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 接受JSON字符串输入，执行HRV分析，返回JSON字符串结果。
     * 支持的分析类型（analyses字段）：
     * - "time_domain"
     * - "frequency_domain"
     * - "non_linear"
     * 默认执行全部三种分析。
     *
     * @param jsonInput JSON格式的分析请求
     * @return JSON格式的分析结果
     */
    public String analyzeJson(String jsonInput) {
        try {
            HrvAnalysisRequest request = objectMapper.readValue(jsonInput, HrvAnalysisRequest.class);
            HrvAnalysisResponse response = analyze(request);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            try {
                HrvAnalysisResponse errResp = HrvAnalysisResponse.error(e.getMessage());
                return objectMapper.writeValueAsString(errResp);
            } catch (Exception ex) {
                return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }

    /**
     * 从文件路径读取RRi序列并执行HRV分析。
     * 支持.txt、.hrm、.csv格式。
     *
     * @param filePath  RRi数据文件路径
     * @param analysisType 分析类型（"time_domain"/"frequency_domain"/"non_linear"/"all"）
     * @return JSON格式的分析结果
     */
    public String analyzeFile(String filePath, String analysisType) {
        try {
            RRi rri = readFile(filePath);
            HrvAnalysisRequest request = buildRequestFromRRi(rri, analysisType);
            HrvAnalysisResponse response = analyze(request);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(HrvAnalysisResponse.error(e.getMessage()));
            } catch (Exception ex) {
                return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }

    /**
     * 生成图表，返回JSON格式的图表结果（Base64 PNG或文件路径）。
     *
     * @param chartRequestJson JSON格式的图表请求
     * @return JSON格式的图表输出
     */
    public String generateChart(String chartRequestJson) {
        try {
            ChartRequest request = objectMapper.readValue(chartRequestJson, ChartRequest.class);
            ChartOutput output = ChartGenerator.generate(request);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(ChartOutput.error(e.getMessage()));
            } catch (Exception ex) {
                return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }

    /**
     * 时域分析快捷方法。
     *
     * @param rri RRi数组（毫秒）
     * @return 时域分析结果JSON
     */
    public String timeDomain(double[] rri) {
        try {
            TimeDomainResult result = ClassicalAnalysis.timeDomain(rri);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 频域分析快捷方法（使用默认参数）。
     *
     * @param rri RRi数组（毫秒）
     * @return 频域分析结果JSON
     */
    public String frequencyDomain(double[] rri) {
        try {
            double[] time = RRiUtils.createTimeArray(rri);
            FrequencyDomainResult result = ClassicalAnalysis.frequencyDomain(rri, time);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 非线性分析快捷方法。
     *
     * @param rri RRi数组（毫秒）
     * @return 非线性分析结果JSON
     */
    public String nonLinear(double[] rri) {
        try {
            NonLinearResult result = ClassicalAnalysis.nonLinear(rri);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 时变HRV分析快捷方法。
     *
     * @param rri     RRi数组（毫秒）
     * @param segSize 时间窗口大小（秒）
     * @param overlap 重叠时长（秒）
     * @return 时变分析结果JSON（各窗口的时域指标）
     */
    public String timeVarying(double[] rri, double segSize, double overlap) {
        try {
            TimeVaryingResult result = NonStationaryAnalysis.timeVarying(new RRi(rri), segSize, overlap);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.getTransposed());
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 执行完整的HRV分析流程。
     */
    private HrvAnalysisResponse analyze(HrvAnalysisRequest request) {
        try {
            double[] rriArr = request.getRriArray();
            if (rriArr == null || rriArr.length < 2) {
                return HrvAnalysisResponse.error("RRi序列为空或长度不足（至少需要2个值）");
            }

            // 构建RRi对象（验证 + 单位转换）
            double[] timeArr = request.getTimeArray();
            RRi rri = timeArr != null ? new RRi(rriArr, timeArr) : new RRi(rriArr);

            // 可选：应用滤波
            if (request.getFilterType() != null) {
                rri = applyFilter(rri, request);
            }

            // 可选：去趋势
            RRiDetrended rriDetrended = null;
            if (request.getDetrendMethod() != null) {
                rriDetrended = applyDetrend(rri, request);
            }

            // 确定用于频域计算的RRi
            RRi rriForFreq = (rriDetrended != null) ? rriDetrended : rri;

            // 执行指定分析
            TimeDomainResult td = null;
            FrequencyDomainResult fd = null;
            NonLinearResult nl = null;

            java.util.List<String> analyses = request.getAnalyses();
            boolean doAll = (analyses == null || analyses.isEmpty());

            if (doAll || analyses.contains("time_domain")) {
                td = ClassicalAnalysis.timeDomain(rri);
            }
            if (doAll || analyses.contains("frequency_domain")) {
                fd = ClassicalAnalysis.frequencyDomain(
                    rriForFreq.getRri(), rriForFreq.getTime(),
                    request.getFs(), request.getMethod(),
                    rriForFreq.isInterpolated() ? null : request.getInterpMethod(),
                    rriForFreq.isDetrended() ? "false" : request.getDetrend(),
                    request.getVlfBand(), request.getLfBand(), request.getHfBand(),
                    request.getNperseg(), request.getNoverlap(), request.getWindow(),
                    request.getOrder(), null
                );
            }
            if (doAll || analyses.contains("non_linear")) {
                nl = ClassicalAnalysis.nonLinear(rri);
            }

            return HrvAnalysisResponse.success(td, fd, nl);
        } catch (Exception e) {
            return HrvAnalysisResponse.error(e.getMessage());
        }
    }

    /** 应用滤波处理 */
    private RRi applyFilter(RRi rri, HrvAnalysisRequest request) {
        switch (request.getFilterType().toLowerCase()) {
            case "quotient":
                return RRiFilters.quotient(rri);
            case "moving_average":
                return RRiFilters.movingAverage(rri, request.getFilterOrder());
            case "moving_median":
                return RRiFilters.movingMedian(rri, request.getFilterOrder());
            case "threshold":
            case "threshold_filter":
                try {
                    double threshold = Double.parseDouble(request.getFilterThreshold());
                    return RRiFilters.thresholdFilter(rri, threshold, 5);
                } catch (NumberFormatException e) {
                    return RRiFilters.thresholdFilter(rri, request.getFilterThreshold(), 5);
                }
            default:
                throw new IllegalArgumentException("不支持的滤波类型: " + request.getFilterType());
        }
    }

    /** 应用去趋势处理 */
    private RRiDetrended applyDetrend(RRi rri, HrvAnalysisRequest request) {
        switch (request.getDetrendMethod().toLowerCase()) {
            case "polynomial":
                return Detrend.polynomialDetrend(rri, request.getDetrendDegree());
            case "smoothness_priors":
            case "smoothness":
                return Detrend.smoothnessPriors(rri, request.getSmoothnesssPriorsL(), request.getFs());
            case "sg":
            case "savitzky_golay":
                return Detrend.sgDetrend(rri, request.getSgWindowLength(), request.getSgPolyorder());
            default:
                throw new IllegalArgumentException("不支持的去趋势方法: " + request.getDetrendMethod());
        }
    }

    /** 从文件路径读取RRi */
    private RRi readFile(String filePath) throws IOException {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".txt")) {
            return RRiReader.readFromText(filePath);
        } else if (lower.endsWith(".hrm")) {
            return RRiReader.readFromHrm(filePath);
        } else if (lower.endsWith(".csv")) {
            return RRiReader.readFromCsv(filePath);
        } else {
            throw new com.hrv.core.exception.FileNotSupportedException(
                "不支持的文件格式，仅支持 .txt .hrm .csv"
            );
        }
    }

    /** 从RRi对象构建请求 */
    private HrvAnalysisRequest buildRequestFromRRi(RRi rri, String analysisType) {
        HrvAnalysisRequest request = new HrvAnalysisRequest();
        double[] rriArr = rri.getRri();
        java.util.List<Double> rriList = new java.util.ArrayList<>();
        for (double v : rriArr) rriList.add(v);
        request.setRri(rriList);

        if (!"all".equals(analysisType) && analysisType != null) {
            request.setAnalyses(java.util.Collections.singletonList(analysisType));
        }
        return request;
    }
}
