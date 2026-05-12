package com.clubchretien.abonnement.controllers.v2;

import com.clubchretien.abonnement.services.NetworkService;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.database.repositories.PartenaireRepository;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("v2.0/network")
@Tag(name = "Network", description = "Network")
public class NetworkV2Controller {
	@Autowired
	NetworkService networkService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;
	@Autowired
	PartenaireRepository partenaireRepository;

	@PostMapping("membership")
	//@ApiVersions({"2.0"})
	public ResponseEntity<CommonResponseWs> payMemberShipFees(
			@RequestParam Integer idCorporate,
			@RequestParam Integer idPartenaire
	) throws DataNotFoundException, AuthentificationException, OwomiException {
		Partenaire partenaire = partenaireRepository.findOneById(idPartenaire);
		RequestContextWs requestContext = new RequestContextWs();
		CommonResponseWs response = networkService.payMemberShipFees(requestContext, partenaire, idCorporate);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}


}
