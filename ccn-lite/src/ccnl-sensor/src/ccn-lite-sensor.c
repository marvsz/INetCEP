//
// Created by johannes on 09.10.19.
//
#define _POSIX_C_SOURCE 199309L
#include <ccnl-core.h>
#include <stdio.h>
#include <time.h>
#include <bits/types/struct_timespec.h>
#include <stdlib.h>
#include <math.h>
#include <unistd.h>
#include "../include/ccn-lite-sensor.h"
#include "ccnl-os-includes.h"
#include "../../ccnl-utils/include/ccnl-crypto.h"
//#include "../include/randomGPS.h"
//#include "../include/randomVICTIMS.h"
//#include "../include/randomPLUG.h"
//#include "../include/randomSURVIVORS.h"

#ifndef CCN_LITE_MKC_BODY_SIZE
#define CCN_LITE_MKC_BODY_SIZE (64 * 1024)
#endif

/*
 * POSIX getline replacement for non-POSIX systems (like Windows)
 * Differences:
 *  - the function returns int64_t instead of sszie_t
 *  - does not accept NUL characters in the input file
 *  - does remove the newline character at the end of a line
 * Warnings:
 *  - the function sets EINVAL, ENOMEM, EOVERFLOW in case of errors. The above are no but are supporte by other C compilers like MSVC
 *
 *  This was created with the help of https://solarianprogrammer.com/2019/04/03/c-programming-read-file-lines-fgets-getline-implement-portable-getline/
 */
int64_t my_getline(char **restrict line, size_t *restrict len, FILE *restrict fp){
    if(line == NULL || len == NULL || fp == NULL){
        errno = EINVAL;
        return -1;
    }
    // Use a chunk array of 128 bytes as parameter for fgets
    char chunk[128];

    // Allocate a block of memory for *line if it is NULL or smaller than the cunk a
    if(*line == NULL || *len < sizeof(chunk)){
        *len = sizeof(chunk);
        if((*line = malloc(*len)) == NULL){
            errno = ENOMEM;
            return -1;
        }
    }
    // "EMPTY" the string
    (*line)[0] = '\0';

    while(fgets(chunk, sizeof(chunk), fp) != NULL){
        // Resize the line buffer if necessary
        size_t len_used = strlen(*line);
        size_t chunk_used = strlen(chunk);

        if(*len - len_used < chunk_used){
            // CHeck for overflow
            if(*len > SIZE_MAX / 2){
                errno = EOVERFLOW;
                return -1;
            } else {
                *len = 2;
            }

            if((*line = realloc(*line, *len)) == NULL){
                errno = ENOMEM;
                return -1;
            }
        }
        // Copy the chunk to the end of the line buffer
        memcpy(*line + len_used, chunk, chunk_used);
        len_used += chunk_used;
        (*line)[len_used] = '\0';

        // Chekc if *line contains '\n', if yes return the *line length
        if((*line)[len_used -1] == '\n'){
            (*line)[strcspn((*line),"\n")] = 0;
            return len_used-1;
        }
    }
    return -1;
}


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

struct ccnl_sensor_tuple_s*
ccnl_sensor_tuple_new(void *data, int len){
    struct ccnl_sensor_tuple_s *st = (struct ccnl_sensor_tuple_s *)ccnl_calloc(1,sizeof(struct ccnl_sensor_tuple_s));
    if(!st)
        return NULL;
    st->datalen = len;
    if(data)
        memcpy(st->data,data,len);
    return st;
}

struct ccnl_sensor_s *
ccnl_sensor_new(struct ccnl_sensor_setting_s *ssetings) {
    struct ccnl_sensor_s *s = (struct ccnl_sensor_s *) ccnl_calloc(1, sizeof(struct ccnl_sensor_s));
    s->settings = ssetings;
    s->stopflag = 0;
    return s;
}

/*void ccnl_sensor_free(struct ccnl_sensor_s* sensor){

}*/

void populate_sensorData(struct ccnl_sensor_s* sensor, char* path){
    DEBUGMSG(DEBUG,"Test\n");
    FILE * fp;
    char * line = NULL;
    //unsigned char testline;
    size_t len = 0;
    int64_t res = 0;
    fp = fopen(path,"r");
    if(fp == NULL){
        DEBUGMSG(ERROR,"Unable to locate or open file.\n");
        exit(EXIT_FAILURE);
    }
    DEBUGMSG(DEBUG,"Vor dem while\n");
    while ((res =my_getline(&line, &len, fp))!= -1){
        DEBUGMSG(DEBUG,"im while, line ist %s, size of line is %lu, size of the res is %ld\n",line,sizeof(line), res);
        struct ccnl_sensor_tuple_s *st = ccnl_sensor_tuple_new(line,res);
        DEBUGMSG(DEBUG,"Neuen Tupel erstellt\n");
        DBL_LINKED_LIST_EMPLACE_BACK(sensor->sensorData,st);
        DEBUGMSG(DEBUG,"Tupel der Liste hinzugefügt\n");
        //memcpy(&testline,st->data, sizeof(st->data));
        DEBUGMSG(DEBUG,"Try to get the sensor data, Content is %.*s\n",(int)st->datalen,st->data);
    }
    DEBUGMSG(DEBUG,"nach dem while\n");
    fclose(fp);
    DEBUGMSG(DEBUG,"file closed\n");
    free(line);
    struct ccnl_sensor_tuple_s* head = sensor->sensorData;
    while(head){
        DEBUGMSG(DEBUG,"Printing all the sensor data, Content is %.*s\n",(int)head->datalen,head->data);
        head = head->next;
    }
    struct ccnl_sensor_tuple_s* test = sensor->sensorData;
    if(test->prev == NULL)
        DEBUGMSG(DEBUG,"The previous of head is null");
    else
        DEBUGMSG(DEBUG,"The previous of head is %.*s\n",(int)test->prev->datalen,test->prev->data);
}

/*
 * checks, if the sensor is set to be stopped. this is indicated by a file in the temp folder with the name of the sensor.
 */
void sensorStopped(struct ccnl_sensor_s* sensor, char* stoppath){
    if(access(stoppath, F_OK) != -1){
        sensor->stopflag = true;
        DEBUGMSG(DEBUG,"Sensor stopped.\n");
    }
}

int ccnl_sensor_loop(struct ccnl_sensor_s *sensor) {
    struct timespec ts;
    char stopPath[100];
    snprintf(stopPath, sizeof(stopPath),"/tmp/%s%istop",getSensorName(sensor->settings->name),sensor->settings->id);
    DEBUGMSG(TRACE, "sensor loop started\n");
    ts.tv_sec = sensor->settings->samplingRate / 1000;
    ts.tv_nsec = (sensor->settings->samplingRate % 1000) * 1000000;
    while (!sensor->stopflag) {
        DEBUGMSG(TRACE, "Sensor id = %i\n", sensor->settings->id);
        ccnl_sensor_sample(sensor);
        nanosleep(&ts, &ts);
        sensorStopped(sensor,stopPath);
    }
    remove(stopPath);
    return 0;
}

void ccnl_sensor_sample(struct ccnl_sensor_s *sensor) {
    char *ccnl_home = getenv("CCNL_HOME");
    char *ctrl = "ccn-lite-ctrl";
    char *mkc = "ccn-lite-mkDSC";
    char *sock = "/tmp/mgmt-nfn-relay-b.sock";
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
    char tupleData[1000];
    FILE * fPtr;
    fPtr = fopen(tuplePath,"w");
    if(fPtr == NULL){
        DEBUGMSG(DEBUG,"Unable to create file.\n");
        exit(EXIT_FAILURE);
    }
    DEBUGMSG(DEBUG,"Enter contents into store.\n");
    snprintf(tupleData, sizeof(tupleData),"Testdata %02d:%02d:%02d.%03d", tm->tm_hour, tm->tm_min, tm->tm_sec, (int) (tv.tv_usec / 1000));
    DEBUGMSG(DEBUG,"Enter Tuple Data into file.\n");
    fputs(tupleData, fPtr);
    fclose(fPtr);
    //snprintf(uri, sizeof(uri), "/node%s/sensor/%s%i/%02d:%02d:%02d.%03d", "A", getSensorName(sensor->settings->name),
             //sensor->settings->id, tm->tm_hour, tm->tm_min, tm->tm_sec, (int) (tv.tv_usec / 1000));
    snprintf(uri, sizeof(uri), "/node%s/sensor/%s%i", "B", getSensorName(sensor->settings->name),
             sensor->settings->id);
    snprintf(exec, sizeof(exec), "%s/bin/%s -s ndn2013 \"%s\" -i %s > %s", ccnl_home, mkc, uri, tuplePath, conPath);
    mkCStatus = system(exec);
    DEBUGMSG(DEBUG, "mkC returned %i\n", mkCStatus);
    snprintf(exec, sizeof(exec), "%s/bin/%s -x %s addContentToCache %s -v trace", ccnl_home, ctrl, sock, conPath);
    execStatus = system(exec);
    DEBUGMSG(DEBUG, "addContentToCache returned %i\n", execStatus);

}
