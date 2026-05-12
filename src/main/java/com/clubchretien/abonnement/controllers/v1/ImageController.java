package com.clubchretien.abonnement.controllers.v1;

import com.clubchretien.abonnement.services.ImageService;
import com.clubchretien.abonnement.ws.PhotoWs;
import com.owomi.commons.enums.ErrorCodeEnums;
import com.owomi.commons.enums.WsOperationResponseStatus;
import com.owomi.commons.exceptions.AuthentificationException;
import com.owomi.commons.exceptions.DataNotFoundException;
import com.owomi.commons.exceptions.FileUploadException;
import com.owomi.commons.exceptions.OwomiException;
import com.owomi.commons.ws.CommonResponseWs;
import com.owomi.commons.ws.RequestContextWs;
import com.owomi.database.entities.Partenaire;
import com.owomi.security.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("v1.0/image")
@Tag(name = "Image", description = "Image")
public class ImageController {

	private static Logger logger = LogManager.getLogger(ImageController.class);
	@Autowired
	ImageService imageService;
	@Autowired
	UserService userService;
	@Autowired
	HttpServletRequest httpServletRequest;

	@PostMapping("/upload")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadImage(@ModelAttribute MultipartFile file) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.uploadImage(requestContext, partenaire, file);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/update")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> updateImage(@ModelAttribute MultipartFile file) throws AuthentificationException, DataNotFoundException, FileUploadException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.updateImage(requestContext, partenaire, file);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping("/")
	//@ApiVersions({"1.0"})
	public ResponseEntity<Resource> getImage() throws AuthentificationException, OwomiException, DataNotFoundException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		String contentType = "application/octet-stream";
		Resource resource = null;
		try {
			resource = imageService.getImage(requestContext, partenaire);
			contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			logger.error("requestContext : " + requestContext, ex);
			throw new OwomiException(requestContext, ex.getMessage(), ErrorCodeEnums.NONE, ex.getMessage());
		}
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	@PostMapping("/corporate/upload/{idCorporate}")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadImageCorporate(@ModelAttribute MultipartFile file, @PathVariable Integer idCorporate) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.uploadImageCorporate(requestContext, idCorporate, file);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PostMapping("/corporate/update/{idCorporate}")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> updateImageCorporate(@ModelAttribute MultipartFile file, @PathVariable Integer idCorporate) throws AuthentificationException, OwomiException, DataNotFoundException, FileUploadException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.updateImageCorporate(requestContext, idCorporate, file);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping("/corporate/{idCorporate}")
	//@ApiVersions({"1.0"})
	public ResponseEntity<Resource> getImageCorporate(@PathVariable Integer idCorporate) throws AuthentificationException, OwomiException, DataNotFoundException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		Resource resource = null;
		String contentType = "application/octet-stream";
		try {
			resource = imageService.getImageCorporate(requestContext, idCorporate);
			contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			logger.error("requestContext : " + requestContext, ex);
			throw new OwomiException(requestContext, ex.getMessage(), ErrorCodeEnums.NONE, ex.getMessage());
		}
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	@PostMapping("/corporate/piece/upload")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadImagePiece(@ModelAttribute MultipartFile file) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.uploadImagePiece(requestContext, partenaire, file);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping("/corporate/piece")
	//@ApiVersions({"1.0"})
	public ResponseEntity<Resource> getImagePiece() throws AuthentificationException, OwomiException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		String contentType = "application/octet-stream";
		Resource resource = null;
		try {
			resource = imageService.getImagePiece(requestContext, partenaire);
			contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			logger.error("requestContext : " + requestContext, ex);
			throw new OwomiException(requestContext, ex.getMessage(), ErrorCodeEnums.NONE, ex.getMessage());
		}

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	@PostMapping("/corporate/photo/upload")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadImagePiecePhoto(@ModelAttribute MultipartFile file) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.uploadImagePiecePhoto(requestContext, partenaire, file);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PutMapping("/corporate/photo/upload")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadImagePiecePhoto(@RequestParam String fileUrl) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.uploadImagePiecePhoto(requestContext, partenaire, fileUrl);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping("/corporate/photo")
	//@ApiVersions({"1.0"})
	public ResponseEntity<Resource> getImagePiecePhoto() throws AuthentificationException, DataNotFoundException, IOException, OwomiException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		String contentType = "application/octet-stream";
		Resource resource = null;
		try {
			resource = imageService.getImagePiecePhoto(requestContext, partenaire);
			contentType = httpServletRequest.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			logger.error("requestContext : " + requestContext, ex);
			throw new OwomiException(requestContext, ex.getMessage(), ErrorCodeEnums.NONE, ex.getMessage());
		}
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}


	@PostMapping("/corporate/photos/upload")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadImagePiecePhoto(
			@ModelAttribute MultipartFile piece,
			@ModelAttribute MultipartFile piecePhoto
	) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.uploadImagePieceAndPiecePhoto(requestContext, partenaire, piece, piecePhoto);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@PutMapping("/corporate/photos/upload")
	//@ApiVersions({"1.0"})
	public ResponseEntity<CommonResponseWs> uploadImagePiecePhoto(
			@RequestParam String pieceUrl, @RequestParam String piecePhotoUrl
	) throws AuthentificationException, DataNotFoundException, IOException {
		Partenaire partenaire = userService.getPartenaire(httpServletRequest);
		RequestContextWs requestContext = userService.getRequestContext(httpServletRequest);
		CommonResponseWs response = imageService.uploadImagePieceAndPiecePhoto(requestContext, partenaire, pieceUrl, piecePhotoUrl);
		HttpStatus httpStatus = HttpStatus.OK;
		return new ResponseEntity<>(response, httpStatus);
	}

	@GetMapping("/getByUrl")
	//@ApiVersions({"1.0"})
	public ResponseEntity<Resource> getByUrl(@RequestParam String url) throws AuthentificationException, DataNotFoundException, OwomiException {
		RequestContextWs requestContext = userService.getRequestContextBackOffice(httpServletRequest);
		Resource resource = imageService.getResourceByUrl(url);
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


	@PostMapping("/upload/file")
	//@ApiVersions({"1.0"})
	public CommonResponseWs uploadImage(@ModelAttribute PhotoWs image) throws IOException {
		String url = imageService.storeImage(image.getFile());
		return new CommonResponseWs(WsOperationResponseStatus.success, url, null, null, null, null, null);
	}

}
