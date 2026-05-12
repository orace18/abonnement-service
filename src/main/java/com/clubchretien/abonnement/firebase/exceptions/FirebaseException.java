package com.clubchretien.abonnement.firebase.exceptions;


import com.owomi.commons.ws.RequestContextWs;

public class FirebaseException extends RuntimeException {

	private RequestContextWs context;
	private Integer code;

	public FirebaseException(String message) {
		super(message);
	}

	public FirebaseException(String message, Throwable e) {
		super(message, e);
	}

}
