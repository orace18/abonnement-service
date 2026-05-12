package com.clubchretien.abonnement.exception;//package com.owomi.mobile.service.exception;
//
//import com.owomi.commons.enums.ErrorCodeEnums;
//import com.owomi.commons.exceptions.AuthentificationException;
//import com.owomi.commons.exceptions.DataNotFoundException;
//import com.owomi.commons.exceptions.FloCashException;
//import com.owomi.commons.exceptions.OwomiException;
//import com.owomi.commons.models.ErrorResponse;
//import com.stripe.exception.StripeException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.context.request.WebRequest;
//
//import java.util.Date;
//
//@ControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(TokenGenerationException.class)
//    public ResponseEntity<ErrorResponse> tokenGenerationException(TokenGenerationException ex, WebRequest request){
//        ErrorResponse errorResponse = new ErrorResponse(
//                new Date().getTime(),
//                ErrorCodeEnums.TOKEN_GENERATION_ERROR.getErrorCode(),
//                ErrorCodeEnums.TOKEN_GENERATION_ERROR.name(),
//                "TOKEN ERROR"
//                );
//        return new ResponseEntity<>(errorResponse, HttpStatus.EXPECTATION_FAILED);
//    }
//
//    @ExceptionHandler(AuthentificationException.class)
//    public ResponseEntity<ErrorResponse> authentificationException(AuthentificationException ex,WebRequest request){
//        ErrorResponse errorResponse = new ErrorResponse(
//                new Date().getTime(),
//                ErrorCodeEnums.GET_AUTHENTIFICATION_CODE_ERROR.getErrorCode(),
//                "Authentication error",
//                "Impossible to authenticate the user, you could check user token"
//                );
//        return new ResponseEntity<>(errorResponse,HttpStatus.NETWORK_AUTHENTICATION_REQUIRED);
//    }
//
//    @ExceptionHandler(DataNotFoundException.class)
//    public ResponseEntity<ErrorResponse> dataNotFoundException(DataNotFoundException ex, WebRequest request){
//        ErrorResponse errorResponse = new ErrorResponse(
//                new Date().getTime(),
//                ErrorCodeEnums.DATA_NOT_FOUND.getErrorCode(),
//                ErrorCodeEnums.DATA_NOT_FOUND.getErrorDescription(),
//                "Partenaire with the given token not exist in database"
//        );
//        return new ResponseEntity<>(errorResponse,HttpStatus.NOT_FOUND);
//    }
//
//    @ExceptionHandler(FloCashException.class)
//    public ResponseEntity<ErrorResponse> flocashException(FloCashException ex, WebRequest request){
//        ErrorResponse errorResponse = new ErrorResponse(
//                new Date().getTime(),
//                ex.getErrorCode(),
//                ex.getErrorMessage(),
//                "Flocash excetion"
//        );
//        return new ResponseEntity<>(errorResponse,HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(StripeException.class)
//    public ResponseEntity<ErrorResponse> flocashException(StripeException ex, WebRequest request){
//        ErrorResponse errorResponse = new ErrorResponse(
//                new Date().getTime(),
//                ex.getStatusCode(),
//                ex.getMessage(),
//                ex.getLocalizedMessage()
//        );
//        return new ResponseEntity<>(errorResponse,HttpStatus.BAD_REQUEST);
//    }
//
//
//    @ExceptionHandler(OwomiException.class)
//    public ResponseEntity<ErrorResponse> owomiException(OwomiException ex, WebRequest request){
//        ErrorResponse errorResponse = new ErrorResponse(
//                new Date().getTime(),
//                ex.getErrorCodeEnums().getErrorCode(),
//                ErrorCodeEnums.DATA_NOT_FOUND.getErrorDescription(),
//                "This version of app is out of date please change the version of you app"
//        );
//        return new ResponseEntity<>(errorResponse,HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(SessionExpireException.class)
//    public ResponseEntity<ErrorResponse> sessionExpireException(SessionExpireException ex, WebRequest request){
//        ErrorResponse errorResponse = new ErrorResponse(
//                new Date().getTime(),
//                ErrorCodeEnums.SESSION_EXPIRED_ERROR.getErrorCode(),
//                "THE SESSION EXPIRED",
//                "THE COMMAND SESSION HAS EXPIRED PLEASE RETRY THE OPERATION"
//        );
//        return new ResponseEntity<>(errorResponse,HttpStatus.SERVICE_UNAVAILABLE);
//    }
//}
