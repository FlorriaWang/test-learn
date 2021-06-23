package com.example.rice.testlearn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.rice.testlearn.dao.TrainDao;
import com.example.rice.testlearn.model.TrainDb;
import com.example.rice.testlearn.model.TrainEntity;
import com.example.rice.testlearn.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: wangfan
 * @Date: 2021/6/3
 */
@Slf4j
@Component
public class TrainService {

    @Autowired
    private TrainDao trainDao;

    @Autowired
    private RestTemplate restTemplate;

    private final static String TRAINNOURLTEMP = "https://search.12306.cn/search/v1/train/search?keyword=%s&date=%s";
    private final static String SCHEDULEURLTEMP = "https://kyfw.12306.cn/otn/queryTrainInfo/query?leftTicketDTO.train_no=%s&leftTicketDTO.train_date=%s&rand_code=";

    private final static List<String> trainNoPrefix1List = Arrays.asList("K", "G", "D", "Z", "T", "C", "S", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private final static List<String> trainNoPrefix2List = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");

//    private final static List<String> trainNoPrefix1List = Arrays.asList("K");
//    private final static List<String> trainNoPrefix2List = Arrays.asList( "11");

    private final static DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final static DateTimeFormatter format2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static String dingUrl = "https://oapi.dingtalk.com/robot/send?access_token=cf3d9cf852c601fd0751f38a91eedc45e109920c62764b17d8baf8b8a04cb94f";

    /**
     * 爬取12306数据
     * @return  返回爬取的数据条数
     */
    public int crawler() {
        try {
            trainDao.delete();
            //存储所有车次信息
            ConcurrentHashMap trainNoMap = new ConcurrentHashMap<String, TrainEntity>();
            trainNoPrefix1List.parallelStream().forEach(trainNoPrefix1 -> {
                trainNoPrefix2List.parallelStream().forEach(trainNoPrefix2 -> {
                        String trainNoprefix = trainNoPrefix1 + trainNoPrefix2 ;
                        LocalDate fromDate = LocalDate.now().plusDays(7);
                        LocalDate toDate = LocalDate.now().plusDays(14);
                        List<String> dateList = getBetweenDate(fromDate, toDate);
                        dateList.parallelStream().forEach(date -> {
                            String trainNoUrl = String.format(TRAINNOURLTEMP, trainNoprefix, date);
                            //爬取车次编码数据
                            JSONObject response = JSON.parseObject(HttpUtils.sendHttpGet(trainNoUrl));
                            if (response != null && response.get("status") != null && response.getBoolean("status") == true) {
                                for (int i = 0 ; i < response.getJSONArray("data").size(); i++) {
                                    JSONObject jo = (JSONObject)response.getJSONArray("data").get(i);
                                    TrainEntity trainEntity = new TrainEntity();
                                    if (trainNoMap.keySet().contains(jo.getString("station_train_code"))) {
                                        continue;
                                    }
                                    trainEntity.setTrainNo(jo.getString("station_train_code"));
                                    trainEntity.setTrainCode(jo.getString("train_no"));
                                    trainEntity.setFromStation(jo.getString("from_station"));
                                    trainEntity.setToStation(jo.getString("to_station"));
                                    String dateDb = LocalDate.parse(jo.getString("date"), format1).format(format2);
                                    trainEntity.setDate(dateDb);
                                    //获取时刻表数据
                                    String scheduleUrl = String.format(SCHEDULEURLTEMP, jo.getString("train_no"), dateDb);
                                    JSONObject schedule = JSON.parseObject(HttpUtils.sendHttpGet(scheduleUrl));
                                    if (schedule != null && schedule.get("status") != null && schedule.getBoolean("status") == true) {
                                        trainEntity.setScheduler(schedule.getJSONObject("data").getJSONArray("data").toJSONString());
                                        log.info("trainNo:{}", trainEntity.getTrainNo());
                                        //查询到车次信息才会加到数据库中
                                        trainNoMap.put(jo.getString("station_train_code"), trainEntity);
                                    }
                                }
                            }
                        });
                });
            });
            log.info("火车数据：{}",trainNoMap.values());
            int result = trainDao.insertTrainEntityList(new ArrayList<TrainEntity>(trainNoMap.values()));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 分析爬取的数据并入库
     */
    public JSONObject analysisSchedule() {
        List<TrainEntity> trainList = trainDao.selectTrainEntity();
        List<TrainDb> schedulerList =  new ArrayList<>();
        List<String> trainNoList = trainList.stream().map(train -> train.getTrainNo()).collect(Collectors.toList());

        CopyOnWriteArraySet<String> updateSet = new CopyOnWriteArraySet();
        CopyOnWriteArraySet<String> addSet = new CopyOnWriteArraySet<>();

        trainList.parallelStream().forEach(train -> {
            String trainNo = train.getTrainNo();
            Set<String> otherNo = new HashSet<>();
            TrainDb trainDb = new TrainDb();
            JSONArray stationList = JSONArray.parseArray(train.getScheduler()) ;
            String train_type = stationList.getJSONObject(0).getString("train_class_name");
            String start_time = stationList.getJSONObject(0).getString("start_time");
            String end_time = stationList.getJSONObject(stationList.size()-1).getString("arrive_time");
            String dataSource = "{\"text\":\"该数据由12306提供\", \"url\":\"12306.cn\"}";
            String midStations = "";
            //遍历时刻表站点
            for (int i = 0; i < stationList.size(); i++) {
                if (i == 0 || i == stationList.size()-1) {
                    continue;
                }
                JSONObject station = (JSONObject) stationList.get(i);
                String name = station.getString("station_name");
                String arrive = station.getString("arrive_time");
                String start = station.getString("start_time");

                long timeDiff = calTime(start,arrive);
                String during = timeDiff + "分钟";
                DecimalFormat df1 = new DecimalFormat("00");
                String midStation = name + "|" + arrive + "|"  + during + "|" + start + "|" + String.valueOf(df1.format(i+1)) +"#";
                midStations = midStations + midStation;

                String no = station.getString("station_train_code");
                if (!StringUtils.isEmpty(no) && !trainNo.equals(no) && !trainNoList.contains(no)) {
                    otherNo.add(no);
                }
            }
            trainDb.setTrainNo(train.getTrainNo());
            trainDb.setTrainType(train_type);
            trainDb.setStartStation(train.getFromStation());
            trainDb.setStartTime(start_time);
            trainDb.setEndStation(train.getToStation());
            trainDb.setEndTime(end_time);
            trainDb.setMidStations(midStations.length() > 0 ? midStations.substring(0,midStations.length()-1) : "");
            trainDb.setDataSource(dataSource);
            schedulerList.add(trainDb);
            //入库并记录数量
            updateCount(trainDao.insert(trainDb), trainNo, updateSet, addSet);
            //列车中途车次名称更换，也要入库
            for (String no : otherNo) {
                trainDb.setTrainNo(no);
                updateCount(trainDao.insert(trainDb), no, updateSet, addSet);
            }
        });
        JSONObject result = new JSONObject();
        result.put("updateSet", updateSet);
        result.put("addSet", addSet);
        return result;
    }

    private static long calTime(String time1, String time2) {
        DateFormat df = new SimpleDateFormat("HH:mm");
        long minutes = 0L;
        try {
            Date d1 = df.parse(time1);
            Date d2 = df.parse(time2);
            // 这样得到的差值是微秒级别
            long diff = d1.getTime() - d2.getTime();
            minutes = diff / (1000 * 60);
        } catch (ParseException e) {
            System.out.println("抱歉，时间日期解析出错。");
        }
        return minutes;
    }

    /**
     * 计算更新条数和新增条数
     * result:  0--表示未更新；1--表示新增；2--表示更新
     * @param result
     * @param updateSet
     * @param addSet
     */
    public void updateCount(int result, String trainNo, CopyOnWriteArraySet updateSet, CopyOnWriteArraySet addSet) {
        if (result == 1) {
            addSet.add(trainNo);
        } else if (result == 2) {
            updateSet.add(trainNo);
        }
    }

    public static List<String> getBetweenDate(LocalDate startDate, LocalDate endDate) {
        List<String> list =new ArrayList<>();
        long distance = ChronoUnit.DAYS.between(startDate, endDate);
        Stream.iterate(startDate, date -> date.plusDays(1)).limit(distance +1).
                forEach(day ->list.add(day.format(format1).toString()));

        return list;
    }

    public final <T> String doPostJsonBodyForDingTalk(String text){
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DateTimeFormatter fm = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String textMsgPrefix = "【时刻表更新】" ;
        JSONObject pushObj = new JSONObject();
        pushObj.put("msgtype","text");
        JSONObject textObj=new JSONObject();
        textObj.put("content",textMsgPrefix+text);
        pushObj.put("text",textObj);
        HttpEntity<T> httpEntity = new HttpEntity(pushObj, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(dingUrl, httpEntity, String.class);
        return responseEntity.getBody();
    }

}
