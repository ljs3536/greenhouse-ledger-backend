package com.smartfarm.ledger.web;

import com.smartfarm.ledger.service.ApiException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Map<String, String>> handleApi(ApiException exception) {
		return ResponseEntity.status(exception.status()).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, String>> handleInvalid(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage() == null ? "요청 값을 확인해 주세요." : error.getDefaultMessage())
				.orElse("요청 값을 확인해 주세요.");
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", "요청 본문을 읽지 못했습니다."));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
		log.error("request failed", exception);
		return ResponseEntity.internalServerError().body(Map.of("message", "요청을 처리하지 못했습니다."));
	}
}
