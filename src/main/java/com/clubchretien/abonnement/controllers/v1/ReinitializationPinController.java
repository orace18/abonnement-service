package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.ReinitializationPinService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("v1.0/reset/pin")
public class ReinitializationPinController {

	@Autowired
	UserService userService;
	@Autowired
	PartenaireRepository partenaireRepository;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	private ReinitializationPinService reinitializationPinService;

	@PostMapping("/request/email")
	//@ApiVersions({"1.0"})
	public CommonResponseWs requestResetPinEmail() throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = reinitializationPinService.requestResetPinEmail(partenaire.getEmail());
		return response;
	}

	@PostMapping("/request/email/master")
	//@ApiVersions({"1.0"})
	public CommonResponseWs requestResetPinEmail(@RequestParam String email) throws OwomiException, DataNotFoundException, AuthentificationException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = reinitializationPinService.requestResetPinEmail(email);
		return response;
	}

	@PostMapping("/request/telephone")
	//@ApiVersions({"1.0"})
	public CommonResponseWs requestResetPinTelephone() throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = reinitializationPinService.requestResetPinTelephone(partenaire.getTelephone());
		return response;
	}

	@PostMapping("/request/telephone/master")
	//@ApiVersions({"1.0"})
	public CommonResponseWs requestResetPinTelephone(@RequestParam String telephone) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = reinitializationPinService.requestResetPinTelephone(telephone);
		return response;
	}

	@PostMapping(path = "/request/certify")
	//@ApiVersions({"1.0"})
	public CommonResponseWs certificationDemande(@RequestParam String code) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = reinitializationPinService.requestResetPinCertification(partenaire, code);
		return response;
	}

	@PostMapping(path = "/request/certify/master")
	//@ApiVersions({"1.0"})
	public CommonResponseWs certificationDemande(@RequestParam String code, @RequestParam String telephone) throws Exception {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
		CommonResponseWs response = reinitializationPinService.requestResetPinCertification(partenaire, code);
		return response;
	}

	@PostMapping
	//@ApiVersions({"1.0"})
	public CommonResponseWs resetPin(@RequestParam String pin) throws Exception {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = reinitializationPinService.resetPin(partenaire, pin);
		return response;
	}

	@PostMapping(path = "/master")
	//@ApiVersions({"1.0"})
	public CommonResponseWs resetPin(@RequestParam String pin, @RequestParam String telephone) throws Exception {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(telephone);
		CommonResponseWs response = reinitializationPinService.resetPin(partenaire, pin);
		return response;
	}

}
