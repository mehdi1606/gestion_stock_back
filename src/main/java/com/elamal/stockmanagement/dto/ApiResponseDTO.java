package com.elamal.stockmanagement.dto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// DTO pour les requêtes d'entrée de stock
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private LocalDateTime timestamp;

    // Constructeur pour succès
    public ApiResponseDTO(T data, String message) {
        this.success = true;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // Constructeur pour succès sans message
    public ApiResponseDTO(T data) {
        this.success = true;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // Constructeur pour erreur
    public ApiResponseDTO(List<String> errors, String message) {
        this.success = false;
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    // Méthodes statiques pour faciliter la création
    public static <T> ApiResponseDTO<T> success(T data) {
        return new ApiResponseDTO<>(data);
    }

    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return new ApiResponseDTO<>(data, message);
    }

    public static <T> ApiResponseDTO<T> error(String message) {
        return new ApiResponseDTO<>(List.of(message), message);
    }

    public static <T> ApiResponseDTO<T> error(List<String> errors, String message) {
        return new ApiResponseDTO<>(errors, message);
    }
}
