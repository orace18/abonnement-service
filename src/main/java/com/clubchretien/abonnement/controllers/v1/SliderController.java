package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.authentication.SecurityUtils;
import com.clubchretien.abonnement.services.SliderService;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.utils.RequestContextUtils;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("v1.0/slider")
public class SliderController {

    @Autowired
    SliderService sliderService;
    @Autowired
    UserService userService;
    @Autowired
    HttpServletRequest httpServletRequest;
    @Autowired
    SecurityUtils securityUtils;

    public Logger logger = LogManager.getLogger(SliderController.class);

    @GetMapping("/read/slider")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> readSlider() throws DataNotFoundException, AuthentificationException {
        //Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        Partenaire partenaire = securityUtils.getPartenaireFromHeader(httpServletRequest);
        RequestContextWs requestContext = RequestContextUtils.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = sliderService.getPartenaireSliders(requestContext,partenaire);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/get/slider/image")
	//@ApiVersions({"1.0"})
    public ResponseEntity<Resource> getSliderImage(@RequestParam Integer idSlider) throws IOException, OwomiException, DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        String contentType = "application/octet-stream";
        Resource resource = null;
        try {
            resource = sliderService.getSliderImage(requestContext, idSlider);
            contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException | DataNotFoundException ex) {
            logger.error("requestContext : "+ requestContext, ex);
            throw new OwomiException(requestContext, ex.getMessage(), ErrorCodeEnums.NONE, ex.getMessage());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/read/slider/action")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> readSliderAction(@RequestParam String typeAction, @RequestParam Integer idAction) throws DataNotFoundException, AuthentificationException, OwomiException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = sliderService.readSliderAction(requestContext,partenaire, typeAction, idAction);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @GetMapping("/read/activityLogsForEveryone")
    //@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> readeActicityLogsForEveryOne() throws DataNotFoundException, AuthentificationException {
        Partenaire partenaire = userService.getPartenaire(httpServletRequest);
        RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
        CommonResponseWs commonResponseWs = sliderService.readActivityLogsForEveryone(requestContext);
        return new ResponseEntity<>(commonResponseWs, HttpStatus.OK);
    }

    @GetMapping("/read/sliderByCategories")
	//@ApiVersions({"1.0"})
    public ResponseEntity<CommonResponseWs> getTwoSliderByCategories(@RequestParam ("cityId") Integer cityId, @RequestParam (required = false)Optional<Integer> page, @RequestParam(required = false)Optional<Integer> pageSize)
    {
        CommonResponseWs sliderListWs = sliderService.getTwoSliderByCategorie(cityId,page.orElse(0),pageSize.orElse(2));
        return new ResponseEntity<>(sliderListWs, HttpStatus.OK);
    }
}
