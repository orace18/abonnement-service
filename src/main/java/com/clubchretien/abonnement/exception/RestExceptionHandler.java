package com.clubchretien.abonnement.exception;//package com.owomi.mobile.service.exception;
//
//import com.owomi.commons.exceptions.*;
//import com.owomi.commons.models.ErrorResponse;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import javax.servlet.http.HttpServletRequest;
//
//@ControllerAdvice
//public class RestExceptionHandler {
//    Logger logger = LogManager.getLogger(RestExceptionHandler.class);
//
//    @ExceptionHandler(value = {
//            DataNotFoundException.class,
//            AuthentificationException.class,
//            FileUploadException.class,
//            OwomiException.class
//    })
//
//    public ResponseEntity<ErrorResponse> handleException(Exception error, HttpServletRequest request) {
//        ErrorResponse errorResponse = new ErrorResponse();
//        HttpStatus httpStatus = null;
//
//        if (error instanceof AuthentificationException) {
//            httpStatus= HttpStatus.UNAUTHORIZED;
//            errorResponse =  getErrorResponse((AuthentificationException) error,  request);
//        }
//        else if (error instanceof DataNotFoundException) {
//            httpStatus= HttpStatus.INTERNAL_SERVER_ERROR;
//            errorResponse =  getErrorResponse((DataNotFoundException) error,  request);
//        }
//        else if (error instanceof FileUploadException) {
//            httpStatus= HttpStatus.INTERNAL_SERVER_ERROR;
//            errorResponse =  getErrorResponse((FileUploadException) error,  request);
//        }
//        else if (error instanceof OwomiException) {
//            httpStatus= HttpStatus.INTERNAL_SERVER_ERROR;
//            errorResponse =  getErrorResponse((OwomiException) error,  request);
//        }
//        else {
//            httpStatus= HttpStatus.INTERNAL_SERVER_ERROR;
//            errorResponse.setPath(request.getServletPath());
//            errorResponse.setErrorMessage(ExceptionUtils.getExceptionStackTrace(error));
//        }
//        return new ResponseEntity<>(errorResponse, httpStatus);
//    }
//
//
//    private ErrorResponse getErrorResponse(AuthentificationException authentication, HttpServletRequest request) {
//        ErrorResponse errorResponse = new ErrorResponse();
//        errorResponse.setErrorCode(authentication.getErrorCodeEnums().getErrorCode());
//        errorResponse.setErrorType(authentication.getErrorCodeEnums().name());
//        errorResponse.setErrorMessage(authentication.getErrorCodeEnums().getErrorDescription());
//        errorResponse.setPath(request.getServletPath());
//        return errorResponse;
//    }
//
//    private ErrorResponse getErrorResponse(DataNotFoundException exception, HttpServletRequest request) {
//        ErrorResponse errorResponse = new ErrorResponse();
//        errorResponse.setErrorCode(exception.getErrorCodeEnums().getErrorCode());
//        errorResponse.setErrorMessage(exception.getDetailMessage());
//        errorResponse.setDataId(exception.getDataId()+"");
//        errorResponse.setErrorType(exception.getErrorCodeEnums().name());
//        errorResponse.setPath(request.getServletPath());
//        return errorResponse;
//    }
//
//    private ErrorResponse getErrorResponse(FileUploadException exception, HttpServletRequest request ) {
//        ErrorResponse errorResponse = new ErrorResponse();
//        errorResponse.setErrorCode(exception.getErrorCodeEnums().getErrorCode());
//        errorResponse.setErrorMessage(exception.getErrorCodeEnums().getErrorDescription());
//        errorResponse.setErrorType(exception.getErrorCodeEnums().name());
//        errorResponse.setPath(request.getServletPath());
//        return errorResponse;
//    }
//
//    private ErrorResponse getErrorResponse(OwomiException exception,
//                                           HttpServletRequest request
//    ) {
//        ErrorResponse errorResponse = new ErrorResponse();
//        errorResponse.setErrorCode(exception.getErrorCodeEnums().getErrorCode());
//        errorResponse.setErrorMessage(!exception.getErrorCodeEnums().getErrorDescription().trim().isEmpty() ? exception.getErrorCodeEnums().getErrorDescription() : !exception.getDetailMessage().trim().isEmpty() ? exception.getDetailMessage() : !exception.getMessage().trim().isEmpty() ? exception.getMessage() : "");
//        errorResponse.setErrorType(exception.getErrorCodeEnums().name());
//        errorResponse.setPath(request.getServletPath());
//        errorResponse.setData(exception.getData());
//        return errorResponse;
//    }
//}
