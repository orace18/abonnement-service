package com.clubchretien.abonnement.exception.handlers;

import com.clubchretien.abonnement.firebase.exceptions.FirebaseException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.FileUploadException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWss;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalControllerExceptionHandler {

	@ExceptionHandler(ConversionFailedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public CommonResponseWss<Object> handleConflict(ConversionFailedException ex) {
		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), null);
	}

	@ExceptionHandler(FirebaseException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public CommonResponseWss<Object> handleFirebaseException(FirebaseException ex) {
		return CommonResponseWss.failed("Invalid Firebase Token", HttpStatus.UNAUTHORIZED.value(), null);
	}

	@ExceptionHandler(DataNotFoundException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public CommonResponseWss<Object> handleDataNotFoundException(DataNotFoundException ex) {
		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
	}

	@ExceptionHandler(FileUploadException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public CommonResponseWss<Object> handleFileUploadException(FileUploadException ex) {
		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
	}

	@ExceptionHandler(OwomiException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public CommonResponseWss<Object> handleOwomiException(OwomiException ex) {
		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public CommonResponseWss<Object> handleException(Exception ex) {
		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
	}
}

//@RestControllerAdvice
//public class GlobalControllerExceptionHandler {
//
//	@ExceptionHandler(ConversionFailedException.class)
//	@ResponseStatus(HttpStatus.BAD_REQUEST)
//	public CommonResponseWss<Object> handleConflict(RuntimeException ex) {
//		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.BAD_REQUEST.value(), null);
//	}
//
//	@ExceptionHandler(FirebaseException.class)
//	@ResponseStatus(HttpStatus.UNAUTHORIZED)
//	public CommonResponseWss<Object> handleFirebaseException(RuntimeException ex) {
//		return CommonResponseWss.failed("Invalid Firebase Token", HttpStatus.UNAUTHORIZED.value(), null);
//	}
//
//	@ExceptionHandler(DataNotFoundException.class)
//	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//	public CommonResponseWss<Object> handleDataNotFoundException(RuntimeException ex) {
//		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
//	}
//
//	@ExceptionHandler(FileUploadException.class)
//	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//	public CommonResponseWss<Object> handleFileUploadException(RuntimeException ex) {
//		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
//	}
//
//	@ExceptionHandler(OwomiException.class)
//	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//	public CommonResponseWss<Object> handleOwomiException(RuntimeException ex) {
//		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
//	}
//
//	@ExceptionHandler(Exception.class)
//	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//	public CommonResponseWss<Object> handleException(RuntimeException ex) {
//		return CommonResponseWss.failed(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
//	}
//
//
//
//}


