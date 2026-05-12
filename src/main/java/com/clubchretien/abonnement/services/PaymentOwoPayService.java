package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.utils.BCryptUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.PrivateKeyWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.dataservice.DataServiceClient;
import com.owomi.rest.client.owopay.PaymentOwoPayServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static com.clubchretien.abonnement.utils.UtilsService.decrypt;
import static com.owomi.commons.constants.TransfertConstant.PRIVATE_KEY_CODE_NOT_FOUND;

@Component
public class PaymentOwoPayService {

	private static Logger logger = LogManager.getLogger(PartenaireAuthenticationService.class);

	@Autowired
	PaymentOwoPayServiceClient paymentOwoPayServiceClient;

	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	DataServiceClient dataServiceClient;

	@Autowired
	Mapper mapper;

	public CommonResponseWs processOwoPay(
			Long idPaiement, String corporate, Boolean pushedPaiement
	) throws Exception {
		return paymentOwoPayServiceClient.processOwoPay(idPaiement, corporate, pushedPaiement);
	}

	public CommonResponseWs validateTransactionPaiementOwoPay(
			Partenaire partenaire, Long idPaiement, String pin
	) throws Exception {
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		if (!partenaire.getAutoriserResetPin()) {
			commonResponseWs.setReference(ErrorCodeEnums.PIN_RESET_ERROR.getErrorDescription());
			commonResponseWs.setMessage("CHANGE_PIN");
			commonResponseWs.setStatus(WsOperationResponseStatus.rejected);
			commonResponseWs.setInfo("PIN MUST BE CHANGE BEFORE ANY OPERATION");
			commonResponseWs.setData(false);
			return commonResponseWs;
		}
		PrivateKeyWs privateKeyWs = dataServiceClient.getPrivate(Long.valueOf(partenaire.getId()), httpServletRequest.getHeader("access-token"));
		if (privateKeyWs.getPrivateKey().equals(PRIVATE_KEY_CODE_NOT_FOUND))
			return new CommonResponseWs(WsOperationResponseStatus.failed, "PRIVATE_KEY_CODE_NOT_FOUND", 430, null);
		logger.debug(" pin crypted {}", pin);
		String decryptedPin = decrypt(pin, privateKeyWs.getPrivateKey());
		logger.debug(" pin decryptedPin {}", decryptedPin);
		if (decryptedPin == null || decryptedPin.length() != 5)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "PRIVATE_KEY_CODE_NOT_FOUND", 413, null);
		boolean statusPin = BCryptUtils.checkpw(decryptedPin, partenaire.getPin());
		logger.debug(" pin statusPin {}", statusPin);
		return paymentOwoPayServiceClient.validateTransactionPaiementOwoPay(idPaiement, statusPin, partenaire.getId());
	}

	public CommonResponseWs rejectTransactionPaiementOwoPay(
			Partenaire partenaire, Long idPaiement
	) throws Exception {
		return paymentOwoPayServiceClient.rejectTransactionPaiementOwoPay(idPaiement, partenaire.getId());
	}


}
