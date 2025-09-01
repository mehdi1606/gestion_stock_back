package com.elamal.stockmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {
    private boolean success;
    private T data;
    private String message;
    private String error;
    private LocalDateTime timestamp;

    public static <T> ApiResponseDTO<T> success(T data) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public static <T> ApiResponseDTO<T> success(T data, String message) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public static <T> ApiResponseDTO<T> error(String error) {
        ApiResponseDTO<T> response = new ApiResponseDTO<>();
        response.setSuccess(false);
        response.setError(error);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
