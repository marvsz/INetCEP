//
// Created by johannes on 09.10.19.
//
#include "ccn-lite-sensor.h"

struct ccnl_sensor_setting_s*
ccnl_sensor_settings_new(unsigned int id, unsigned int type, unsigned int sasamplingRate, unsigned int name){

}

struct ccnl_sensor_tuple_s*
ccnl_sensor_tuple_new(struct ccnl_buf_s* data, int datalen){

}

struct ccnl_sensor_s*
ccnl_sensor_new(struct ccnl_sensor_setting_s* ssetings){

}

void ccnl_sensor_free(struct ccnl_sensor_s* sensor){

}

void populate_sensorData(struct ccnl_sensor_s* sensor, char* path){

}
