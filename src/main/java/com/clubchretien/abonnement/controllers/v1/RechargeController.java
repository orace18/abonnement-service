package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.RechargeService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.CreateRechargeInitWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("v1.0/recharge")
public class RechargeController {
	private static Logger logger = LogManager.getLogger(RechargeController.class);

	@Autowired
	HttpServletRequest request;
	@Autowired
	RechargeService rechargeService;
	@Autowired
	UserService userService;

	@Autowired
	PartenaireRepository partenaireRepository;

	@Autowired
	SecurityUtils securityUtils;


	@PostMapping("/init")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> initRecharge(@RequestBody CreateRechargeInitWs rechargeWs) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(request);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(request);
		CommonResponseWs response = rechargeService.createRecharge(requestContext, rechargeWs, partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/init/external")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> initRechargeExternal(
			@RequestParam Integer idPartenaire,
			@RequestBody CreateRechargeInitWs rechargeWs
	) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(request);
		Partenaire partenaire = partenaireRepository.findOneById(idPartenaire);
		CommonResponseWs response = rechargeService.createRecharge(requestContext, rechargeWs, partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/list")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> listRecharge(
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(request);
		Partenaire partenaire = securityUtils.getPartenaireFromHeader(request);
		CommonResponseWs response = rechargeService.listRecharge(
				requestContext, partenaire, page, size
		);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/list/external")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> listRecharge(
			@RequestParam Integer idPartenaire, @RequestParam String corporate,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "25") Integer size
	) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(request);
		Partenaire partenaire = partenaireRepository.findOneById(idPartenaire);
		CommonResponseWs response = rechargeService.listRecharge(
				requestContext, partenaire, corporate, page, size
		);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/moyenne")
	//@ApiVersions({"1.0"})
	public ResponseEntity<Double> moyenneRecharge(
			@RequestParam Integer idPartenaire, @RequestParam Integer mois,
			@RequestParam(required = false) String corporate
	) throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		RequestContextWs requestContext = RequestContextUtils.getRequestContext(request);
		Partenaire partenaire = partenaireRepository.findOneById(idPartenaire);
		Double response = rechargeService.moyenneRecharge(
				partenaire, corporate, mois
		);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/auto-correction")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> autoUpdate(@RequestParam Integer idPartenaire) throws OwomiException, NotificationException {
		CommonResponseWs commonResponseWs = rechargeService.updateRecharges(idPartenaire);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@PostMapping("/certify")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> certify(@RequestParam Long idPaiement) throws OwomiException, NotificationException {
		CommonResponseWs commonResponseWs = rechargeService.certifiedRechargePaiement(idPaiement);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

}
