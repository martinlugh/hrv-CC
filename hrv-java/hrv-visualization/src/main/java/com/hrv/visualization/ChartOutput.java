package com.hrv.visualization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图表生成输出结果。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChartOutput {

    /** 输出状态："success"或"error" */
    @JsonProperty("status")
    private String status;

    /** Base64编码的PNG图像（输出格式为base64时） */
    @JsonProperty("base64_image")
    private String base64Image;

    /** 输出文件路径（输出格式为png时） */
    @JsonProperty("file_path")
    private String filePath;

    /** 错误信息 */
    @JsonProperty("error")
    private String error;

    private ChartOutput() {}

    /** Base64图像输出 */
    public static ChartOutput base64(String base64Data) {
        ChartOutput out = new ChartOutput();
        out.status = "success";
        out.base64Image = base64Data;
        return out;
    }

    /** 文件路径输出 */
    public static ChartOutput file(String path) {
        ChartOutput out = new ChartOutput();
        out.status = "success";
        out.filePath = path;
        return out;
    }

    /** 错误输出 */
    public static ChartOutput error(String message) {
        ChartOutput out = new ChartOutput();
        out.status = "error";
        out.error = message;
        return out;
    }

    public String getStatus() { return status; }
    public String getBase64Image() { return base64Image; }
    public String getFilePath() { return filePath; }
    public String getError() { return error; }
}
