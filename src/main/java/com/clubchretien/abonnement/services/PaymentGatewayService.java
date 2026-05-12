package com.clubchretien.abonnement.services;

import com.owomi.commons.enums.PaymentTypeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.StringUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.DemandePaiementWs;
import com.owomi.commons.ws.OwoPayPaymentWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.owopay.PaymentRequestWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.entities.PaymentMeans;
import com.owomi.database.repositories.PaymentMeansRepository;
import com.owomi.database.repositories.PaymentRepository;
import com.owomi.rest.client.auth.AuthentificationUtils;
import com.owomi.rest.client.owopay.OwoPayClient;
import com.owomi.rest.client.owopay.PaymentGatewayServiceClient;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.owomi.commons.constants.TokenConstant.ACCESS_TOKEN;

@Component
public class PaymentGatewayService {
	@Autowired
	OwoPayClient owoPayClient;

	@Autowired
	PaymentGatewayServiceClient paymentGatewayServiceClient;

	@Autowired
	HttpServletRequest httpServletRequest;

	@Autowired
	AuthentificationUtils authentificationUtils;

	@Autowired
	PaymentMeansRepository paymentMeansRepository;
	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	Mapper mapper;


	public CommonResponseWs attentePaiement(String matricule) {
		System.out.println("INPUT " + matricule);
		PaymentMeans paymentMeans = paymentMeansRepository.findOneById(2);
		if (paymentMeans == null)
			return new CommonResponseWs(WsOperationResponseStatus.failed, "PAYMENT MEAN NOT FOUND", 404, null);
		List<String> statuses = Collections.singletonList("PENDING");
		List<OwoPayPaymentWs> list = this.paymentRepository.findByTypeAndPaymentMeansAndStatutInAndPaymentInfoContainsIgnoreCaseAndDateExpirationGreaterThan(
				PaymentTypeEnums.PAYMENT.getValue(), paymentMeans, statuses, matricule, Timestamp.from(Instant.now())
		).stream().map(v -> mapper.map(v, OwoPayPaymentWs.class)).collect(Collectors.toList());
		return new CommonResponseWs(WsOperationResponseStatus.success, "", 200, list);
	}

	public CommonResponseWs getPaymentsMeans(RequestContextWs contextWs, Partenaire partenaire) throws OwomiException {
		return owoPayClient.getPaymentsMeans(partenaire.getId());
	}

	public CommonResponseWs getPaymentsMeans(RequestContextWs contextWs, Partenaire partenaire, Double version) throws OwomiException {
		return owoPayClient.getPaymentsMeans(partenaire.getId(), version);
	}

	public CommonResponseWs getWithdrawalMeans(RequestContextWs contextWs, Partenaire partenaire, Double version) throws OwomiException {
		return owoPayClient.getWithdrawalMeans(partenaire.getId(), version);
	}


	public CommonResponseWs getPaymentsMeansV2(RequestContextWs contextWs, Partenaire partenaire) throws OwomiException {
		return owoPayClient.getPaymentsMeansV2(partenaire.getId());
	}

	public CommonResponseWs getResume(
			RequestContextWs contextWs, Long idPaiement, Integer idPaymentMean
	) throws OwomiException {
		return owoPayClient.getResume(idPaiement, idPaymentMean);
	}

	public CommonResponseWs resumePaiement(
			RequestContextWs contextWs, Long idPaiement, Integer idPaymentMean
	) throws OwomiException {
		return paymentGatewayServiceClient.resumePaiement(idPaiement, idPaymentMean);
	}

	public CommonResponseWs initializePaiement(DemandePaiementWs ws) throws OwomiException {
		return paymentGatewayServiceClient.initializePaiement(ws);
	}

	public CommonResponseWs attentePaiement(
			RequestContextWs contextWs, String matricule
	) throws OwomiException {
		return owoPayClient.attentePaiement(matricule);
	}

	public CommonResponseWs statutPaiement(
			RequestContextWs contextWs, Long idPaiement
	) throws OwomiException {
		return owoPayClient.statutPaiement(idPaiement);
	}

	public CommonResponseWs statusPaiement(
			RequestContextWs contextWs, Long idPaiement
	) throws OwomiException {

		return paymentGatewayServiceClient.statusPaiement(idPaiement);
	}

	public CommonResponseWs paiementV1(
			RequestContextWs contextWs, PaymentRequestWs paymentRequestWs
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		String token = this.getToken();
		paymentRequestWs.setToken(token);
		return owoPayClient.paiement(paymentRequestWs);
	}

	public CommonResponseWs paiement(
			RequestContextWs contextWs, PaymentRequestWs paymentRequestWs
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		String token = this.getToken();
		paymentRequestWs.setToken(token);
		return paymentGatewayServiceClient.paiement(paymentRequestWs);
	}

	public CommonResponseWs processOwopay(
			RequestContextWs contextWs, Long idPaiement, Integer idPaymentMean,
			String corporate
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		String token = this.getToken(httpServletRequest);
		return owoPayClient.processOwopay(idPaiement, idPaymentMean, token, corporate);
	}

	public CommonResponseWs processOwoPay(
			RequestContextWs contextWs, Long idPaiement, Integer idPaymentMean, String corporate
	) throws OwomiException, DataNotFoundException, AuthentificationException {
		String token = this.getToken(httpServletRequest);
		return paymentGatewayServiceClient.processOwoPay(idPaiement, idPaymentMean, token, corporate);
	}

	public String getToken() throws OwomiException, DataNotFoundException, AuthentificationException {
		String code = authentificationUtils.getCode();
		return authentificationUtils.getToken(code);
	}

	private String getToken(HttpServletRequest httpServletRequest) {
		String token = httpServletRequest.getHeader(ACCESS_TOKEN);
		if (StringUtils.isNull(token)) {
			Cookie[] cookies = httpServletRequest.getCookies();
            if (cookies == null || cookies.length == 0) return null;
            List<Cookie> cookiesList = Arrays.asList(cookies);
            Cookie cookie = cookiesList.stream().filter(item -> item.getName().equals(ACCESS_TOKEN)).findFirst().orElse(null);
            if (cookie == null) return null;
            token = cookie.getValue();
        }
        return token;
    }

}
