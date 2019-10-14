//
// Created by johannes on 09.10.19.
//
#define _POSIX_C_SOURCE 199309L

#include <ccnl-core.h>
#include <time.h>
#include <bits/types/struct_timespec.h>
#include <stdlib.h>
#include <math.h>
#include "../include/ccn-lite-sensor.h"
#include "ccnl-os-includes.h"
#include "../../ccnl-utils/include/ccnl-common.h"
#include "../../ccnl-utils/include/ccnl-crypto.h"


#ifndef CCN_LITE_MKC_BODY_SIZE
#define CCN_LITE_MKC_BODY_SIZE (64 * 1024)
#endif

struct ccnl_sensor_setting_s *
ccnl_sensor_settings_new(unsigned int id, unsigned int type, unsigned int sasamplingRate, unsigned int name) {
    struct ccnl_sensor_setting_s *s = (struct ccnl_sensor_setting_s *) ccnl_calloc(1,
                                                                                   sizeof(struct ccnl_sensor_setting_s));
    s->id = id;
    s->name = name;
    s->type = type;
    s->samplingRate = sasamplingRate;

    return s;
}

char *getSensorName(unsigned int i) {
    char *retval = "";
    switch (i) {
        case 1 :
            retval = "victims";
            break;
        case 2:
            retval = "survivors";
            break;
        case 3:
            retval = "gps";
            break;
        case 4:
            retval = "plug";
            break;
        default:
            retval = NULL;
            break;
    }
    return retval;
}

/*struct ccnl_sensor_tuple_s*
ccnl_sensor_tuple_new(struct ccnl_buf_s* data, int datalen){

}*/

struct ccnl_sensor_s *
ccnl_sensor_new(struct ccnl_sensor_setting_s *ssetings) {
    struct ccnl_sensor_s *s = (struct ccnl_sensor_s *) ccnl_calloc(1, sizeof(struct ccnl_sensor_s));
    s->settings = ssetings;
    s->stopflag = 0;
    return s;
}

/*void ccnl_sensor_free(struct ccnl_sensor_s* sensor){

}*/

/*void populate_sensorData(struct ccnl_sensor_s* sensor, char* path){

}*/

int ccnl_sensor_loop(struct ccnl_sensor_s *sensor) {
    struct timespec ts;
    DEBUGMSG(TRACE, "sensor loop started\n");
    ts.tv_sec = sensor->settings->samplingRate / 1000;
    ts.tv_nsec = (sensor->settings->samplingRate % 1000) * 1000000;
    while (!sensor->stopflag) {
        DEBUGMSG(TRACE, "Sensor id = %i\n", sensor->settings->id);
        ccnl_sensor_sample(sensor);
        nanosleep(&ts, &ts);

    }
    return 0;
}

/*ccnl_sensor_mkPrefix(char* name, int suite){
    struct ccnl_prefix
}*/

void ccnl_sensor_sample(struct ccnl_sensor_s *sensor) {
    char *ccnl_home = getenv("CCNL_HOME");
    char *ctrl = "ccn-lite-ctrl";
    char *mkc = "ccn-lite-mkC";
    char *sock = "/tmp/mgmt-nfn-relay-a.sock";
    char *tuplePath = "/tmp/tupleData";
    char *conPath = "/tmp/sensorData";
    int mkCStatus, execStatus;
    char exec[200];
    DEBUGMSG(INFO, "Sample sensor with sampling rate of %i milliseconds\n", sensor->settings->samplingRate);
    struct timeval tv;
    struct tm *tm;
    gettimeofday(&tv, NULL);
    tm = localtime(&tv.tv_sec);
    char uri[100];
    snprintf(uri, sizeof(uri), "/node%s/sensor/%s%i/%02d:%02d:%02d.%03d", "A", getSensorName(sensor->settings->name),
             sensor->settings->id, tm->tm_hour, tm->tm_min, tm->tm_sec, (int) (tv.tv_usec / 1000));
    snprintf(exec, sizeof(exec), "%s/bin/%s -s ndn2013 \"%s\" -i %s > %s", ccnl_home, mkc, uri, tuplePath, conPath);
    mkCStatus = system(exec);
    DEBUGMSG(DEBUG, "mkC returned %i\n", mkCStatus);
    snprintf(exec, sizeof(exec), "%s/bin/%s -x %s addContentToCache %s", ccnl_home, ctrl, sock, conPath);
    execStatus = system(exec);
    DEBUGMSG(DEBUG, "addContentToCache returned %i\n", execStatus);

}
