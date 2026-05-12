package com.clubchretien.abonnement.ws;

import com.owomi.commons.ws.SliderWs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlidersWs extends SliderWs {
    private MultipartFile file;
}
