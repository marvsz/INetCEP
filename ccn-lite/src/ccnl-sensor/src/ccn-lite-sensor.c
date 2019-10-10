//
// Created by johannes on 09.10.19.
//
#define _POSIX_C_SOURCE 199309L
#include <ccnl-core.h>
#include <time.h>
#include <bits/types/struct_timespec.h>
#include "../include/ccn-lite-sensor.h"
#include "ccnl-os-includes.h"

struct ccnl_sensor_setting_s*
ccnl_sensor_settings_new(unsigned int id, unsigned int type, unsigned int sasamplingRate, unsigned int name){
struct ccnl_sensor_setting_s *s = (struct ccnl_sensor_setting_s *) ccnl_calloc(1,sizeof(struct ccnl_sensor_setting_s));
s->id = id;
s->name = name;
s->type = type;
s->samplingRate = sasamplingRate;

return s;
}
/*
struct ccnl_sensor_tuple_s*
ccnl_sensor_tuple_new(struct ccnl_buf_s* data, int datalen){

}*/

struct ccnl_sensor_s*
ccnl_sensor_new(struct ccnl_sensor_setting_s* ssetings){
struct ccnl_sensor_s *s = (struct ccnl_sensor_s *) ccnl_calloc(1,sizeof(struct ccnl_sensor_s));
s->settings = ssetings;
s->stopflag = 0;
return s;
}

/*void ccnl_sensor_free(struct ccnl_sensor_s* sensor){

}*/

/*void populate_sensorData(struct ccnl_sensor_s* sensor, char* path){

}*/

int ccnl_sensor_loop(struct ccnl_sensor_s* sensor){
    //ccnl_sensor_sample(sensor);
    struct timespec ts;
    DEBUGMSG(TRACE,"sensor loop started\n");
    ts.tv_sec = sensor->settings->samplingRate / 1000;
    ts.tv_nsec = (sensor->settings->samplingRate % 1000) * 1000000;
    while(!sensor->stopflag){
        DEBUGMSG(TRACE,"Sensor id = %i\n",sensor->settings->id);
        ccnl_sensor_sample(sensor);
        nanosleep(&ts,&ts);

    }
    return 0;
}

void ccnl_sensor_sample(struct ccnl_sensor_s* sensor){
    DEBUGMSG(INFO,"Sample sensor with sampling rate of %i milliseconds\n",sensor->settings->samplingRate);
}
