package com.example.rice.testlearn.dao;

import com.example.rice.testlearn.model.TrainDb;
import com.example.rice.testlearn.model.TrainEntity;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wutianlong
 */
@Repository
@Mapper
public interface TrainDao {

    @Insert({"insert INTO schedule (`train_no`,`train_type`,`start_station`,`start_time`,`end_station`,`end_time`,`mid_stations`,`data_source`) " +
            "VALUES (#{trainDb.trainNo},#{trainDb.trainType},#{trainDb.startStation},#{trainDb.startTime},#{trainDb.endStation},#{trainDb.endTime},#{trainDb.midStations},#{trainDb.dataSource}) " +
            "ON DUPLICATE KEY UPDATE train_no = #{trainDb.trainNo},train_type = #{trainDb.trainType},start_station = #{trainDb.startStation}, start_time = #{trainDb.startTime},end_station = #{trainDb.endStation},end_time = #{trainDb.endTime},mid_stations = #{trainDb.midStations} "})
    int insert(@Param("trainDb") TrainDb trainDb);


    @Insert("<script>" +
            "replace into train_no(`train_no`,`train_code`,`from_station`,`to_station`,`scheduler`,`date`) " +
            "values " +
            "<foreach collection='trainEntityList' item='trainEntity' index='index' separator=','>" +
            "(#{trainEntity.trainNo}, #{trainEntity.trainCode},#{trainEntity.fromStation},#{trainEntity.toStation},#{trainEntity.scheduler},#{trainEntity.date})" +
            "</foreach>" +
            "</script>")
    int insertTrainEntityList(@Param("trainEntityList") List<TrainEntity> trainEntityList);

    @Select({"select * from train_no"})
    List<TrainEntity> selectTrainEntity();

    @Delete({"delete from train_no"})
    boolean delete();
}
