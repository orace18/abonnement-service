package com.clubchretien.abonnement.controllers.v1;

import com.owomi.commons.enums.FirebaseAppEnums;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.commons.ws.VersionOwomiWs;
import com.owomi.rest.client.dataservice.VersionOwomiServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;

@RestController("VersionOwomiController")
@RequestMapping("v1.0/version/owomi")
public class VersionOwomiController {
	private static Logger logger = LogManager.getLogger(VersionOwomiController.class);

	@Autowired
	VersionOwomiServiceClient versionOwomiServiceClient;
	@Autowired
	HttpServletRequest httpServletRequest;


	@PostMapping("/create")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> createVersion(@RequestBody VersionOwomiWs ws) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		ws.setApplication(FirebaseAppEnums.MOBILE.toString());
		CommonResponseWs response = versionOwomiServiceClient.createVersion(context, ws);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/check")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> checkVersion(@RequestBody VersionOwomiWs ws) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		ws.setApplication(FirebaseAppEnums.MOBILE.toString());
		CommonResponseWs response = versionOwomiServiceClient.checkVersion(context, ws);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/delete")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> deleteVersion(@RequestBody VersionOwomiWs ws) throws AuthentificationException, DataNotFoundException, ParseException, OwomiException {
		RequestContextWs context = RequestContextUtils.getRequestContext(httpServletRequest);
		ws.setApplication(FirebaseAppEnums.MOBILE.toString());
		CommonResponseWs response = versionOwomiServiceClient.deleteVersion(context, ws);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}


}