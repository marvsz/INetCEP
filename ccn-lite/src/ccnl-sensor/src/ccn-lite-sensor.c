//
// Created by johannes on 09.10.19.
//
#define _POSIX_C_SOURCE 199309L
#include <ccnl-core.h>
#include <stdio.h>
#include <time.h>
#include <bits/types/struct_timespec.h>
#include <stdlib.h>
#include <pthread.h>
#include <math.h>
#include <unistd.h>
#include <sys/stat.h>
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

    // Allocate a block of memory for *line if it is NULL or smaller than the chunk a
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

/**
 * Creates the directory that leads to a filename given in dir if it does not exist.
 * @param dir A filename
 * sources:
 * https://stackoverflow.com/questions/2336242/recursive-mkdir-system-call-on-unix/11425692
 * https://stackoverflow.com/questions/7430248/creating-a-new-directory-in-c
 */
static void _mkdir(const char *dir) {
    char tmp[256];
    char *p = NULL;
    size_t len;
    struct stat st = {0};

    snprintf(tmp, sizeof(tmp),"%s",dir);
    len = strlen(tmp);
    if(tmp[len - 1] == '/')
        tmp[len - 1] = 0;
    for(p = tmp + 1; *p; p++)
        if(*p == '/') {
            *p = 0;
            if(stat(tmp,&st)==-1)
                mkdir(tmp, S_IRWXU);
            *p = '/';
        }
}


struct ccnl_sensor_setting_s *
ccnl_sensor_settings_new(unsigned int id, unsigned int type, unsigned int sasamplingRate, unsigned int name, char* socketPath) {
    struct ccnl_sensor_setting_s *s = (struct ccnl_sensor_setting_s *) ccnl_calloc(1,
                                                                                   sizeof(struct ccnl_sensor_setting_s));
    s->id = id;
    s->name = name;
    s->type = type;
    s->samplingRate = sasamplingRate;
    s->socketPath = socketPath;
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
    //DEBUGMSG(DEBUG,"sensor_new: started\n");
    // hier muss unbedingt die laenge des char arrays das in dem Tupel gespeichert werden soll mit reingegeben werden.
    struct ccnl_sensor_tuple_s *st = (struct ccnl_sensor_tuple_s *)ccnl_calloc(1,sizeof(struct ccnl_sensor_tuple_s)+len+1);
    //DEBUGMSG(DEBUG,"sensor_new: allocate space\n");
    if(!st)
        return NULL;
    //DEBUGMSG(DEBUG,"sensor_new: space allocated\n");
    st->datalen = len;
    if(data)
        memcpy(st->data,data,len);
    //DEBUGMSG(DEBUG,"sensor_new: data copied into data\n");
    return st;
}

struct ccnl_sensor_s *
ccnl_sensor_new(struct ccnl_sensor_setting_s *ssetings) {
    struct ccnl_sensor_s *s = (struct ccnl_sensor_s *) ccnl_calloc(1, sizeof(struct ccnl_sensor_s));
    s->settings = ssetings;
    s->stopflag = 0;
    return s;
}

void ccnl_sensor_free(struct ccnl_sensor_s* sensor){
    ccnl_free(sensor->settings);
    while(sensor->sensorData){
        struct ccnl_sensor_tuple_s* toDelete = sensor->sensorData;
        DBL_LINKED_LIST_REMOVE_FIRST(sensor->sensorData);
        ccnl_free(toDelete);
    }
}

void populate_sensorData(struct ccnl_sensor_s* sensor, char* path){
    FILE * fp;
    char * line = NULL;
    size_t len = 0;
    int64_t res = 0;
    int i = 0;
    fp = fopen(path,"r");
    if(fp == NULL){
        DEBUGMSG(ERROR,"Unable to locate or open file.\n");
        exit(EXIT_FAILURE);
    }
    DEBUGMSG(DEBUG,"Vor dem while\n");
    while ((res =my_getline(&line, &len, fp))!= -1){
        i = i + 1;
        struct ccnl_sensor_tuple_s *st = ccnl_sensor_tuple_new(line,strlen(line)*sizeof(char));
        DBL_LINKED_LIST_EMPLACE_BACK(sensor->sensorData,st);
    }
    fclose(fp);
    free(line);
}

/*
 * checks, if the sensor is set to be stopped. this is indicated by a file in the temp folder with the name of the sensor.
 */
void sensorStopped(struct ccnl_sensor_s* sensor, char* stoppath){
    if(access(stoppath, F_OK) != -1){
        sensor->stopflag = 1;
        DEBUGMSG(DEBUG,"Sensor stopped.\n");
    }
}

int ccnl_sensor_loop(struct ccnl_sensor_s *sensor) {
    struct timespec ts;
    char stopPath[100];
    snprintf(stopPath, sizeof(stopPath),"/tmp/%s%i/stop",getSensorName(sensor->settings->name),sensor->settings->id);
    char sock[100];
    snprintf(sock,sizeof(sock),"/tmp/%s",sensor->settings->socketPath);
    char tuplePath[100];
    snprintf(tuplePath,sizeof(tuplePath),"/tmp/%s%i/tupleData",getSensorName(sensor->settings->name),sensor->settings->id);
    _mkdir(tuplePath);
    char binaryContentPath[100];
    snprintf(binaryContentPath,sizeof(binaryContentPath),"/tmp/%s%i/tupleDataBinary",getSensorName(sensor->settings->name),sensor->settings->id);
    DEBUGMSG(TRACE, "sensor loop started\n");
    ts.tv_sec = sensor->settings->samplingRate / 1000;
    ts.tv_nsec = (sensor->settings->samplingRate % 1000) * 1000000;
    while (!sensor->stopflag) {
        ccnl_sensor_sample(sensor,sock,tuplePath,binaryContentPath);
        nanosleep(&ts, &ts);
        sensorStopped(sensor,stopPath);
    }
    ccnl_sensor_free(sensor);
    remove(stopPath);
    return 0;
}

void ccnl_sensor_sample(struct ccnl_sensor_s *sensor,char* sock, char* tuplePath, char* binaryContentPath) {
    char *ccnl_home = getenv("CCNL_HOME");
    char *ctrl = "ccn-lite-ctrl";
    char *mkc = "ccn-lite-mkDSC";
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

    if(sensor->settings->type==CCNL_SENSORTYPE_EMULATION){
        DEBUGMSG(DEBUG,"Enter trace content into store.\n");
        struct ccnl_sensor_tuple_s* currentData = sensor->sensorData;
        snprintf(tupleData, sizeof(tupleData),"%.*s",(int)currentData->datalen,currentData->data);
        DBL_LINKED_LIST_REMOVE_FIRST(sensor->sensorData);
        ccnl_free(currentData);
        if(!sensor->sensorData)
            sensor->stopflag=1;

    }
    else{
        DEBUGMSG(DEBUG,"Enter random content into store.\n");
        snprintf(tupleData, sizeof(tupleData),"Testdata %02d:%02d:%02d.%03d", tm->tm_hour, tm->tm_min, tm->tm_sec, (int) (tv.tv_usec / 1000));

    }
    DEBUGMSG(DEBUG,"Enter Tuple Data into file.\n");
    fputs(tupleData, fPtr);
    fclose(fPtr);
    //snprintf(uri, sizeof(uri), "/node%s/sensor/%s%i/%02d:%02d:%02d.%03d", "A", getSensorName(sensor->settings->name),
             //sensor->settings->id, tm->tm_hour, tm->tm_min, tm->tm_sec, (int) (tv.tv_usec / 1000));
    snprintf(uri, sizeof(uri), "/node%s/sensor/%s%i", "B", getSensorName(sensor->settings->name),
             sensor->settings->id);
    snprintf(exec, sizeof(exec), "%s/bin/%s -s ndn2013 \"%s\" -i %s > %s", ccnl_home, mkc, uri, tuplePath, binaryContentPath);
    mkCStatus = system(exec);
    DEBUGMSG(DEBUG, "mkC returned %i sock is %s\n", mkCStatus, sock);
    snprintf(exec, sizeof(exec), "%s/bin/%s -x %s addContentToCache %s -v trace", ccnl_home, ctrl, sock, binaryContentPath);
    //snprintf(exec, sizeof(exec), "%s/bin/%s -u 127.0.0.1/6363 addContentToCache %s -v trace", ccnl_home, ctrl, binaryContentPath);
    DEBUGMSG(DEBUG, "command Messge is %s\n",exec);
    //snprintf(exec, sizeof(exec), "%s/bin/%s -x %s addContentToCache %s -v trace", ccnl_home, ctrl, sock, binaryContentPath);
    execStatus = system(exec);
    DEBUGMSG(DEBUG, "addContentToCache returned %i\n", execStatus);
}
