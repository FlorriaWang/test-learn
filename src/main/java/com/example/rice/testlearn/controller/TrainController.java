package com.example.rice.testlearn.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.rice.testlearn.common.MessageResponse;
import com.example.rice.testlearn.service.TrainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * @author wutianlong
 */
@Slf4j
@RestController
@RequestMapping("train")
public class TrainController extends BaseController {

    @Autowired
    private TrainService trainService;
    /**
     * 爬取12306车次数据
     * @return
     */
    @RequestMapping("crawler")
    public MessageResponse crawler() {
        try {
            log.info("列车时刻表人工触发开启......");
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int crawlerResult = trainService.crawler();
            if (crawlerResult > 0) {
                JSONObject result = trainService.analysisSchedule();
                log.info("人工触发时刻表任务：时间:{}，车次总数量：{}，更新车次数量：{},增加车次数量:{},更新车次号：{},增加车次号：{}",
                        LocalDate.now(), crawlerResult, result.getJSONArray("updateSet").size(), result.getJSONArray("addSet").size(),result.getString("updateSet"), result.getString("addSet"));
            }
            stopWatch.stop();
            log.info("列车时刻表人工触发结束，总耗时:{}s", stopWatch.getTotalTimeSeconds());
            return generateSuccessResult("success");
        } catch (Exception e) {
           log.error("列车时刻表人工触发异常:{}", e);
           return generateFailureResult(-4,"error");
        }


    }
}
