package com.ccj.campus.chat.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class StudentCheckinReq {
    @NotNull
    private Long sessionId;
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;

    private Double accuracy;

    private String location;
}