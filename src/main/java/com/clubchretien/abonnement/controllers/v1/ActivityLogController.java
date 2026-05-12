package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.ActivityLogService;
import com.clubchretien.abonnement.services.ProduitService;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.NotificationException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.*;
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
import java.util.List;

@RestController
@RequestMapping("v1.0/activitylog")
public class ActivityLogController {
    private static Logger logger = LogManager.getLogger(ActivityLogController.class);
    @Autowired
    ActivityLogService activityLogService;
    @Autowired
    PartenaireRepository partenaireRepository;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    UserService userService;
    @Autowired
    ProduitService produitService;


	@PostMapping(value = "/makeLogActivity")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> makeLogActivity(
			@RequestParam String tel, @RequestParam String objetEnumString,
			@RequestParam String paramObjet, @RequestParam String s1,
			@RequestParam String s2, @RequestParam int i1
	) throws AuthentificationException, DataNotFoundException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs commonResponseWs = new CommonResponseWs();
		commonResponseWs.setStatus(WsOperationResponseStatus.success);
		return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
	}

	@GetMapping(value = "/list")
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<ActivityLogWs>> list(@RequestParam("page") int page, @RequestParam("size") int size) throws AuthentificationException, DataNotFoundException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		List<ActivityLogWs> activityLogWsList = activityLogService.pageActivityLogWs(requestContext, page, size, partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(activityLogWsList, httpStatus);
	}

	@GetMapping(value = "/list/priority")
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<ActivityLogWs>> list() throws AuthentificationException, DataNotFoundException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		List<ActivityLogWs> activityLogWsList = activityLogService.pageActivityLogPriorityWs(requestContext, partenaire);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(activityLogWsList, httpStatus);
	}

	@GetMapping(value = "create/priority")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> notificationPrioritaireAndAl(
			@RequestParam String destinataire, @RequestParam String title, @RequestParam String message
	) throws AuthentificationException, DataNotFoundException, OwomiException, NotificationException {
		Partenaire partenaire = partenaireRepository.findOneByTelephone(destinataire);
		activityLogService.notificationPrioritaireAndAl(partenaire, title, message);
		return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
	}

	@PostMapping(value = "create/all")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> notificationAlForAll(
			@RequestBody ActivityLogAllCreateWs ws
	) throws AuthentificationException, DataNotFoundException, OwomiException, NotificationException {
		RequestContextWs contextWs = RequestContextUtils.getRequestContext(httpServletRequest);
		activityLogService.notificationAlForAll(contextWs, ws);
		return new ResponseEntity<>(new CommonResponseWs(), HttpStatus.OK);
	}

	@GetMapping(value = "/get")
	//@ApiVersions({"1.0"})
	public ResponseEntity<List<ActivityLogWs>> get(@RequestParam int id) throws AuthentificationException, DataNotFoundException, OwomiException {
//        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
//        Partenaire partenaire = partenaireRepository.findOneByTelephone();
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		List<ActivityLogWs> activityLogWsList = activityLogService.get(requestContext, id);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(activityLogWsList, httpStatus);
    }

    @PutMapping("/updateActivityLog/{id}")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> updateActivityLog(@PathVariable("id") int id) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = activityLogService.updateActivityLog(requestContext,id);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @PostMapping("/updateActivityLog/list")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> updateActivityLogByList(@RequestBody List<IdActivityLogWs> activityLogWsList) throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = activityLogService.updateActivityLogByList(requestContext,activityLogWsList);
        commonResponseWs.setStatus(WsOperationResponseStatus.success);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @GetMapping(value = "/list/notRead")
	//@ApiVersions({"1.0"})
    public ResponseEntity<List<ActivityLogWs>> listNotRead(@RequestParam("page") int page, @RequestParam("size") int size) throws AuthentificationException, DataNotFoundException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        List<ActivityLogWs> activityLogWsList = activityLogService.pageActivityLogNotReadWs(requestContext,page,size, partenaire);
        HttpStatus httpStatus = HttpStatus.OK;
        return new ResponseEntity<>(activityLogWsList, httpStatus);
    }

}
