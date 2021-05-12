package com.codekarehum.covidtgbot.service;

import com.codekarehum.covidtgbot.model.Center;
import com.codekarehum.covidtgbot.model.Data;
import com.codekarehum.covidtgbot.model.dto.ResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramBotService {

    @Autowired
    RestTemplate restTemplate;

    //ENTER YOUR PIN-CODE HERE
    private String pincCode = "110007";

    //Telegram API URL to send HTML message
    String sendMessageUrl = "https://api.telegram.org/bot{bot-token}/sendMessage?parse_mode=html&chat_id=-1001488083254&text={encodedMessage}";

    ObjectMapper objectMapper = new ObjectMapper();
    HttpHeaders headers = new HttpHeaders();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // CRON to call this method every 3 minutes
    @Scheduled(cron = "0 0/3 * * * *")
    public void getVaccineAvailabilityEvery5Minutes() {

        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        log.info("Scheduler started at : {}", LocalDateTime.now().toString());
        List<ResponseDto> dtos = new ArrayList<>();
        try {
            //Calling get data by PIN API
            Data data = getVaccineSlotsByPinApi(pincCode);

            //Get all centers from API response
            List<Center> centers = data.getCenters();

            centers.stream().forEach(center -> {
                //filter all center sessions where available capacity > 0
                center.getSessions()
                        .stream()
                        .filter(session -> session.getAvailableCapacity() > 0)
                        .collect(Collectors.toList())
                        .stream().forEach(o -> {
                    ResponseDto dto = new ResponseDto();
                    dto.setName(center.getName());
                    dto.setAddress(center.getAddress());
                    dto.setCapacity(o.getAvailableCapacity());
                    dto.setDate(o.getDate());
                    dto.setFeeType(center.getFeeType());
                    dto.setVaccine(o.getVaccine());
                    dto.setAgeLimit(o.getMinAgeLimit());
                    dtos.add(dto);
                });

            });

            // Converting dto telegram formatted html message and send notification only if slot available
            if (dtos.size() > 0) {
                for (ResponseDto responseDto : dtos) {
                    String messageUrl = sendMessageUrl.replace("{encodedMessage}", generateMessageTemplate(responseDto));
                    //Calling telegram API sendMessage to send message in group
                    final ResponseEntity<String> exchange = restTemplate.exchange(messageUrl, HttpMethod.GET, entity, String.class);
                    //Sending to sleep, as API has a rate limited on its end, if removed then
                    // API will throw 429 too many requests exception
                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            log.error("========ERROR FOUND========");
            log.error(e.getMessage());
        }
    }


    private Data getVaccineSlotsByPinApi(String pin) {
        LocalDate todayLocalDate = LocalDate.now();
        String currentDate = dateTimeFormatter.format(todayLocalDate);
        String url = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByPin?pincode=" + pin + "&date=" + currentDate;
        Data data = null;
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        try {
            //calling vaccine slot data API
            final ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            data = objectMapper.readValue(response.getBody(), Data.class);
        } catch (Exception e) {
            log.error("Error found while getting vaccine slots.");
            log.error(e.getMessage());
        }
        return data;
    }

    private String generateMessageTemplate(ResponseDto o) {
        StringBuffer sb = new StringBuffer();
        sb.append("<b>Center Name : </b>").append(o.getName()).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("<b>Address : </b>").append(o.getAddress()).append(System.lineSeparator());
        sb.append("<b>Date : </b>").append(o.getDate()).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("<b>Age Limit : </b>").append(o.getAgeLimit()).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("<b>Capacity : </b>").append(o.getCapacity()).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("<u>Vaccine : </u>").append(o.getVaccine()).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("<u>Fee type : </u>").append(o.getFeeType()).append(System.lineSeparator()).append(System.lineSeparator());
        return sb.toString();
    }

}
