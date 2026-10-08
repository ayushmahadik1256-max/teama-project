package com.apnileap.backup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private ApiMeta meta;
    private ApiError error;

    public ApiResponse() {}

    public ApiResponse(T data, ApiMeta meta) {
        this.data = data;
        this.meta = meta;
    }

    public ApiResponse(ApiError error, ApiMeta meta) {
        this.error = error;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(data, new ApiMeta(correlationId, "v1"));
    }

    public static <T> ApiResponse<T> failure(String code, String message, List<String> details, String correlationId) {
        return new ApiResponse<>(new ApiError(code, message, details), new ApiMeta(correlationId, "v1"));
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public ApiMeta getMeta() { return meta; }
    public void setMeta(ApiMeta meta) { this.meta = meta; }
    public ApiError getError() { return error; }
    public void setError(ApiError error) { this.error = error; }

    public static class ApiMeta {
        private String correlation_id;
        private String api_version;

        public ApiMeta() {}
        public ApiMeta(String correlation_id, String api_version) {
            this.correlation_id = correlation_id;
            this.api_version = api_version;
        }

        public String getCorrelation_id() { return correlation_id; }
        public void setCorrelation_id(String correlation_id) { this.correlation_id = correlation_id; }
        public String getApi_version() { return api_version; }
        public void setApi_version(String api_version) { this.api_version = api_version; }
    }

    public static class ApiError {
        private String code;
        private String message;
        private List<String> details;

        public ApiError() {}
        public ApiError(String code, String message, List<String> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<String> getDetails() { return details; }
        public void setDetails(List<String> details) { this.details = details; }
    }
}
