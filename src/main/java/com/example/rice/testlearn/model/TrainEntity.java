package com.example.rice.testlearn.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Map;

/**
 * @author wutianlong
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class TrainEntity implements Serializable {
    private Map<String, String> source;
    private String trainNo;
    private String trainCode;
    private String fromStation;
    private String toStation;
    private String scheduler = "";
    private String date;
}
