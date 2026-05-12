package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.RechargeCoreOwoPayClientCallbackService;
import com.clubchretien.abonnement.services.RechargeService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.OwoPayPaymentWs;
import com.owomi.commons.ws.owopay.core.CoreOwoPayCallbackWs;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v1.0/recharge/callback")
public class CallbackController {
	private static Logger logger = LogManager.getLogger(CallbackController.class);

	@Autowired
	HttpServletRequest request;
	@Autowired
	RechargeService rechargeService;
	@Autowired
	RechargeCoreOwoPayClientCallbackService rechargeCoreOwoPayClientCallbackService;
	@Autowired
	UserService userService;


	@PostMapping("/owopay")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> owopayCallback(@RequestBody OwoPayPaymentWs paymentWs) throws OwomiException, NotificationException {
		try {
			rechargeService.listenPaiement(paymentWs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		commonResponseWs.setCode(200);
		commonResponseWs.setStatus(WsOperationResponseStatus.success);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@PostMapping("/core")
	//@ApiVersions({"1.0"})
	public CommonResponseWs callbackClient(
			@RequestParam(required = false) Long transaction,
			@RequestParam(required = false) String reference,
			@RequestParam(required = false) String status,
			@RequestBody CoreOwoPayCallbackWs ws
	) throws Exception {
		return rechargeCoreOwoPayClientCallbackService.callbackClient(ws);
	}

}
