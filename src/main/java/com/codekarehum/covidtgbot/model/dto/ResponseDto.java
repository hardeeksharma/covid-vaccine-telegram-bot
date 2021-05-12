package com.codekarehum.covidtgbot.model.dto;

import lombok.Data;

@Data
public class ResponseDto {
    String name;
    String address;
    String date;
    Integer capacity;
    String vaccine;
    String feeType;
    Integer ageLimit;
}
