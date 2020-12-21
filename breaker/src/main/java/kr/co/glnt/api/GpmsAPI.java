package kr.co.glnt.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.glnt.dto.CarInfo;
import kr.co.glnt.dto.ParkinglotInfo;
import kr.co.glnt.dto.ResponseDTO;
import kr.co.glnt.dto.payload.ParkInPayload;
import kr.co.glnt.dto.payload.ParkingInfoPayload;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

@Slf4j
@Component
public class GpmsAPI {
    private final RestTemplate template;
    private final ObjectMapper objectMapper;

    public GpmsAPI(@Qualifier(value = "gpmsRestTemplate")RestTemplate template, ObjectMapper objectMapper) {
        this.template = template;
        this.objectMapper = objectMapper;
    }

    /**
     * 서버 시작 시 호출하는 서버정보 조회
     * @param parkingInfoPayload 서버 정보 조회에 필요한 데이터.
     * @return ParkingFeature (서버정보)
     */
    @SneakyThrows
    public List<ParkinglotInfo> getParkinglotData(ParkingInfoPayload parkingInfoPayload) {
        ResponseEntity<ResponseDTO> response = template.postForEntity("/v1/parkinglot/facility/list", parkingInfoPayload, ResponseDTO.class);
        if (Objects.isNull(response)) return null;
        HttpStatus status = HttpStatus.resolve(response.getStatusCodeValue());
        switch (status) {
            case OK:
                ResponseDTO responseDTO = response.getBody();
                try {
                    return objectMapper.convertValue(responseDTO.getData(), new TypeReference<List<ParkinglotInfo>>() {});
                }
                catch (Exception e) {
                    log.error("서버 정보 가져오기 실패", e);
                }
            default:
                return null;
        }
//        return mapper.readValue(new ClassPathResource("parkinglot_info.json").getFile(), new TypeReference<List<ParkinglotInfo>>() {
//        });
    }


    @Async
    public void requestEntranceCar(String key, CarInfo carInfo) {
        try {
            ParkInPayload payload = new ParkInPayload(key, carInfo);
            template.postForObject("/v1/vehicle/parkin", payload, ResponseDTO.class);
        }
        catch (IOException e) {
            log.error("입차 이미지 전송 실패", e);
        }
    }
}

