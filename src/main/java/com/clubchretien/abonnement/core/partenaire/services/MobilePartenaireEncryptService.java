package com.clubchretien.abonnement.core.partenaire.services;

import com.google.gson.Gson;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.authentication.SessionInfoWs;
import com.owomi.commons.ws.encrypt.EncryptTerminalEncoderWs;
import com.owomi.rest.client.auth.TerminalEncryptServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class MobilePartenaireEncryptService {
	private static Logger logger = LogManager.getLogger(MobilePartenaireEncryptService.class);

	@Autowired
	private Mapper mapper;

	@Autowired
	TerminalEncryptServiceClient terminalEncryptServiceClient;

	@Autowired
	HttpServletRequest httpServletRequest;

	public CommonResponseWs decode(SessionInfoWs session, EncryptTerminalEncoderWs ws) {
		return this.terminalEncryptServiceClient.decode(session, ws);
	}

	public String decodedData(SessionInfoWs session, EncryptTerminalEncoderWs ws) {
		return this.terminalEncryptServiceClient.decodedData(session, ws);
	}

	public String decodedData(CommonResponseWs commonResponseWs) {
		Gson gson = new Gson();
		return gson.fromJson(gson.toJson(commonResponseWs.getData()), String.class);
	}

}
