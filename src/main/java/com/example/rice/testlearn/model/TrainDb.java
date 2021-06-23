package com.example.rice.testlearn.model;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * @author wutianlong
 */
@Data
@ToString
public class TrainDb {
    String trainNo;
    String trainType;
    String startStation;
    String startTime;
    String endStation;
    String endTime;
    Integer totalTime;
    String midStations;
    String dataSource;
    Date updateTime;
}
