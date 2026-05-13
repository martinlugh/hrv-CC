package com.hrv.visualization;

import com.hrv.core.classical.ClassicalAnalysis;
import com.hrv.core.rri.RRi;
import com.hrv.core.utils.RRiUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

/**
 * HRV图表生成器。
 * 支持生成4种图表类型，输出为PNG文件或Base64编码字符串。
 * 对应Python原项目中 rri.py 中的各 plot() 方法。
 *
 * <p>图表类型：
 * <ul>
 *   <li>rri：RRi序列时间图（对应Python: RRi.plot()）</li>
 *   <li>histogram：RRi/心率直方图（对应Python: RRi.hist()）</li>
 *   <li>poincare：Poincaré散点图（对应Python: RRi.poincare_plot()）</li>
 *   <li>spectrum：频谱图（PSD曲线加频段着色）</li>
 * </ul>
 *
 * <p>图表生成失败不影响核心HRV计算结果（异常被捕获并返回错误信息）。
 */
public class ChartGenerator {

    /**
     * 根据请求参数生成图表。
     *
     * @param request 图表请求参数
     * @return 图表输出结果（Base64字符串或文件路径）
     */
    public static ChartOutput generate(ChartRequest request) {
        try {
            JFreeChart chart = createChart(request);
            return renderChart(chart, request);
        } catch (Exception e) {
            return ChartOutput.error("图表生成失败: " + e.getMessage());
        }
    }

    /**
     * 根据图表类型创建JFreeChart对象。
     */
    private static JFreeChart createChart(ChartRequest request) {
        switch (request.getChartType().toLowerCase()) {
            case "rri":
                return createRriPlot(request);
            case "histogram":
            case "hist":
                return createHistogram(request);
            case "poincare":
            case "lorenz":
                return createPoincarePlot(request);
            case "spectrum":
            case "psd":
                return createSpectrumPlot(request);
            default:
                throw new IllegalArgumentException("不支持的图表类型: " + request.getChartType()
                    + "，可选: rri, histogram, poincare, spectrum");
        }
    }

    /**
     * 生成RRi序列时间图。
     * 对应Python: RRi.plot()
     * X轴：时间（秒），Y轴：RRi（毫秒）
     */
    private static JFreeChart createRriPlot(ChartRequest request) {
        double[] rri = request.getRri();
        double[] time = request.getTime();

        // 若无时间数组，自动生成
        if (time == null) {
            time = RRiUtils.createTimeArray(rri);
        }

        XYSeries series = new XYSeries("RRi");
        for (int i = 0; i < rri.length; i++) {
            series.add(time[i], rri[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
            request.getTitle() != null ? request.getTitle() : "RRi Series",
            "Time (s)",
            "RRi (ms)",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false
        );

        customizeChart(chart);
        return chart;
    }

    /**
     * 生成RRi/心率直方图。
     * 对应Python: RRi.hist(hr=False/True)
     */
    private static JFreeChart createHistogram(ChartRequest request) {
        double[] rri = request.getRri();
        double[] values;
        String xLabel;

        if (request.isHr()) {
            // 心率模式：HR = 60 / (rri / 1000)
            values = RRiUtils.toHeartRate(rri);
            xLabel = "HR (bpm)";
        } else {
            values = rri;
            xLabel = "RRi (ms)";
        }

        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("Frequency", values, 30); // 30个bin

        JFreeChart chart = ChartFactory.createHistogram(
            request.getTitle() != null ? request.getTitle() : "RRi Histogram",
            xLabel,
            "Frequency",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false
        );

        customizeChart(chart);
        return chart;
    }

    /**
     * 生成Poincaré散点图。
     * 对应Python: RRi.poincare_plot()
     * X轴：RRi[n]，Y轴：RRi[n+1]
     * 附加：SD1/SD2椭圆和对角线
     */
    private static JFreeChart createPoincarePlot(ChartRequest request) {
        double[] rri = request.getRri();
        int n = rri.length;

        // 散点数据：(rri[n], rri[n+1])
        XYSeries scatter = new XYSeries("RRi Pairs");
        for (int i = 0; i < n - 1; i++) {
            scatter.add(rri[i], rri[i + 1]);
        }

        // 计算SD1/SD2
        double[] nlResult = ClassicalAnalysis.computePoincareValues(rri);
        double sd1 = nlResult[0];
        double sd2 = nlResult[1];

        // 对角线 y=x（SD2方向）
        double minVal = RRiUtils.min(rri) * 0.95;
        double maxVal = RRiUtils.max(rri) * 1.05;
        XYSeries sd2Line = new XYSeries("SD2 (y=x)");
        sd2Line.add(minVal, minVal);
        sd2Line.add(maxVal, maxVal);

        // 反对角线（SD1方向）
        double mrri = RRiUtils.mean(rri);
        XYSeries sd1Line = new XYSeries("SD1 (y=-x+2c)");
        sd1Line.add(minVal, -minVal + 2 * mrri);
        sd1Line.add(maxVal, -maxVal + 2 * mrri);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(scatter);
        dataset.addSeries(sd2Line);
        dataset.addSeries(sd1Line);

        JFreeChart chart = ChartFactory.createXYLineChart(
            request.getTitle() != null ? request.getTitle() :
                String.format("Poincaré Plot (SD1=%.1fms, SD2=%.1fms)", sd1, sd2),
            "RRi_n (ms)",
            "RRi_{n+1} (ms)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );

        // 自定义渲染：散点为点，线为虚线
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setSeriesLinesVisible(1, true);
        renderer.setSeriesShapesVisible(1, false);
        renderer.setSeriesPaint(1, Color.GRAY);
        renderer.setSeriesStroke(1, new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        renderer.setSeriesLinesVisible(2, true);
        renderer.setSeriesShapesVisible(2, false);
        renderer.setSeriesPaint(2, Color.DARK_GRAY);
        renderer.setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        plot.setRenderer(renderer);

        customizeChart(chart);
        return chart;
    }

    /**
     * 生成PSD频谱图。
     * X轴：频率（Hz），Y轴：功率谱密度（ms²/Hz）
     * 各频段（VLF/LF/HF）用不同颜色区域标注。
     */
    private static JFreeChart createSpectrumPlot(ChartRequest request) {
        double[] fxx = request.getFxx();
        double[] pxx = request.getPxx();

        if (fxx == null || pxx == null) {
            throw new IllegalArgumentException("频谱图需要提供fxx和pxx数据");
        }

        XYSeries series = new XYSeries("PSD");
        for (int i = 0; i < fxx.length; i++) {
            series.add(fxx[i], pxx[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
            request.getTitle() != null ? request.getTitle() : "Power Spectral Density",
            "Frequency (Hz)",
            "PSD (ms²/Hz)",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false
        );

        customizeChart(chart);

        // 设置X轴范围到HF上界
        XYPlot plot = chart.getXYPlot();
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(0, request.getHfBand()[1]);

        return chart;
    }

    /** 统一图表样式设置 */
    private static void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        }
    }

    /**
     * 将图表渲染为PNG或Base64编码字符串。
     */
    private static ChartOutput renderChart(JFreeChart chart, ChartRequest request) throws IOException {
        int width = request.getWidth();
        int height = request.getHeight();

        // 渲染为BufferedImage
        BufferedImage image = chart.createBufferedImage(width, height);

        if ("png".equalsIgnoreCase(request.getOutputFormat())) {
            // 输出到文件
            String path = request.getOutputPath();
            if (path == null || path.isEmpty()) {
                path = "hrv_chart_" + request.getChartType() + ".png";
            }
            ImageIO.write(image, "PNG", new File(path));
            return ChartOutput.file(path);
        } else {
            // 输出为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return ChartOutput.base64("data:image/png;base64," + base64);
        }
    }
}
