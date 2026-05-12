package com.hrv.visualization;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图表生成请求参数。
 * 对应Python原项目中各 plot() 方法的参数。
 */
public class ChartRequest {

    /** 图表类型："rri"/"histogram"/"poincare"/"spectrum" */
    @JsonProperty("chart_type")
    private String chartType = "rri";

    /** RRi序列值（毫秒） */
    @JsonProperty("rri")
    private double[] rri;

    /** 时间数组（秒） */
    @JsonProperty("time")
    private double[] time;

    /** 频率数组（用于频谱图） */
    @JsonProperty("fxx")
    private double[] fxx;

    /** 功率谱密度数组（用于频谱图） */
    @JsonProperty("pxx")
    private double[] pxx;

    /** 图表宽度（像素），默认800 */
    @JsonProperty("width")
    private int width = 800;

    /** 图表高度（像素），默认500 */
    @JsonProperty("height")
    private int height = 500;

    /** 输出格式："png"或"base64"，默认"base64" */
    @JsonProperty("output_format")
    private String outputFormat = "base64";

    /** 输出文件路径（outputFormat="png"时使用） */
    @JsonProperty("output_path")
    private String outputPath;

    /** 图表标题 */
    @JsonProperty("title")
    private String title;

    /** 是否显示心率（直方图使用） */
    @JsonProperty("hr")
    private boolean hr = false;

    /** VLF频段边界（频谱图使用） */
    @JsonProperty("vlf_band")
    private double[] vlfBand = {0, 0.04};

    /** LF频段边界（频谱图使用） */
    @JsonProperty("lf_band")
    private double[] lfBand = {0.04, 0.15};

    /** HF频段边界（频谱图使用） */
    @JsonProperty("hf_band")
    private double[] hfBand = {0.15, 0.4};

    // Getters and Setters
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }

    public double[] getRri() { return rri; }
    public void setRri(double[] rri) { this.rri = rri; }

    public double[] getTime() { return time; }
    public void setTime(double[] time) { this.time = time; }

    public double[] getFxx() { return fxx; }
    public void setFxx(double[] fxx) { this.fxx = fxx; }

    public double[] getPxx() { return pxx; }
    public void setPxx(double[] pxx) { this.pxx = pxx; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isHr() { return hr; }
    public void setHr(boolean hr) { this.hr = hr; }

    public double[] getVlfBand() { return vlfBand; }
    public void setVlfBand(double[] vlfBand) { this.vlfBand = vlfBand; }

    public double[] getLfBand() { return lfBand; }
    public void setLfBand(double[] lfBand) { this.lfBand = lfBand; }

    public double[] getHfBand() { return hfBand; }
    public void setHfBand(double[] hfBand) { this.hfBand = hfBand; }
}
