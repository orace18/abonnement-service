package com.clubchretien.abonnement.services;

import com.owomi.file.manager.services.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceService {

    @Autowired
    FileService fileService;

    public Resource getResourceByUrl(String url) {
        if(url.equalsIgnoreCase("oJCRSjI.png")) url = "1b4280e1-ea5e-4f84-8511-7ac3d95a8ed6-1722096700891.jpeg";
        Resource resource = fileService.loadFileAsResource(url);
        return resource;
    }
}
