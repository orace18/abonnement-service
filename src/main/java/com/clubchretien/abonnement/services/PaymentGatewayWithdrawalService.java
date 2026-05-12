package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.BCryptUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DemandeRetraitWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.rest.client.auth.AuthentificationUtils;
import com.owomi.rest.client.owopay.OwoPayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class PaymentGatewayWithdrawalService {
	@Autowired
	PaymentGatewayService paymentGatewayService;
	@Autowired
	OwoPayClient owoPayClient;

	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	AuthentificationUtils authentificationUtils;


	public CommonResponseWs getWithdrawalMeans(RequestContextWs contextWs, Partenaire partenaire, Double version) throws OwomiException {
		return owoPayClient.getWithdrawalMeans(partenaire.getId(), version);
	}

	public CommonResponseWs resume(RequestContextWs contextWs, Long idPaiement) throws OwomiException {
		return owoPayClient.withdrawalResume(idPaiement);
	}

	public CommonResponseWs demandeRetrait(RequestContextWs contextWs, Partenaire partenaire, DemandeRetraitWs ws) throws OwomiException {
		ws.setPartenaire(partenaire.getId());
		return owoPayClient.demandeRetrait(ws);
	}

	public CommonResponseWs cancelRetrait(RequestContextWs contextWs, Long idPaiement) throws OwomiException {
		return owoPayClient.cancelRetrait(idPaiement);
	}

	public CommonResponseWs process(RequestContextWs contextWs, Partenaire partenaire, Long idPaiement, String pin) throws OwomiException {
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		if (!partenaire.getAutoriserResetPin()) {
			commonResponseWs.setReference(ErrorCodeEnums.PIN_RESET_ERROR.getErrorDescription());
			commonResponseWs.setMessage("CHANGE_PIN");
			commonResponseWs.setStatus(WsOperationResponseStatus.rejected);
			commonResponseWs.setInfo("PIN MUST BE CHANGE BEFORE ANY OPERATION");
			commonResponseWs.setData(false);
			return commonResponseWs;
		}
		boolean statusPin = BCryptUtils.checkpw(pin, partenaire.getPin());
		return owoPayClient.withdrawalProcess(idPaiement, statusPin);
	}

	public CommonResponseWs withdrawalList(
			RequestContextWs contextWs, Partenaire partenaire,
			Integer page, Integer size, Boolean pending
	) throws OwomiException {
		return owoPayClient.withdrawalList(
				partenaire.getId(), page, size, pending
		);
	}


}
