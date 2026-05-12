package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.WithdrawalAccountService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.WithdrawalAccountCreateWs;
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
import java.text.ParseException;

@RestController
@RequestMapping("v1.0/account/withdrawal")
public class WithdrawalAccountController {
	private static Logger logger = LogManager.getLogger(WithdrawalAccountController.class);

	@Autowired
	WithdrawalAccountService withdrawalAccountService;
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	PartenaireRepository partenaireRepository;


	@PostMapping("/create")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> create(@RequestBody WithdrawalAccountCreateWs ws) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = withdrawalAccountService.create(partenaire, ws);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/update")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> update(@RequestBody WithdrawalAccountCreateWs ws) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = withdrawalAccountService.update(partenaire, ws);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/delete")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> delete(@RequestParam Long id) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = withdrawalAccountService.delete(id);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/list")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> list(
			@RequestParam Integer page, @RequestParam Integer size
	) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		CommonResponseWs response = withdrawalAccountService.list(partenaire, page, size);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}