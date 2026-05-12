package com.clubchretien.abonnement.controllers.v1;


import com.clubchretien.abonnement.services.ResourceService;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.security.services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("v1.0/resource")
public class ResourceController {

	private static Logger logger = LogManager.getLogger(CorporateV1Controller.class);

	@Autowired
	ResourceService resourceService;
	@Autowired
	HttpServletRequest httpServletRequest;
	@Autowired
	UserService userService;

	@GetMapping("/getByUrl")
	//@ApiVersions({"1.0"})
	public ResponseEntity<Resource> getByUrl(@RequestParam String url) throws AuthentificationException, DataNotFoundException, OwomiException {
		RequestContextWs requestContext = userService.getRequestContextBackOffice(httpServletRequest);
		Resource resource = resourceService.getResourceByUrl(url);
		String contentType = null;
		try {
			contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			throw new OwomiException(requestContext, ex.getMessage(), ErrorCodeEnums.NONE, ex.getMessage());
		}
		if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }


}
