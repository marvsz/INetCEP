//
// Created by johannes on 09.10.19.
//
//#define _POSIX_C_SOURCE 199309L
#define _POSIX_C_SOURCE 200809L

#include <ccnl-core.h>
#include <stdio.h>
#include <time.h>
#include <bits/types/struct_timespec.h>
#include <stdlib.h>
#include <pthread.h>
#include <math.h>
#include <inttypes.h>
#include <unistd.h>
#include <sys/stat.h>
#include <ccnl-ext-hmac.h>
#include <limits.h>
#include <ccnl-common.h>
#include <stdlib.h>
#include "../include/ccn-lite-sensor.h"
#include "ccnl-os-includes.h"
#include "../../ccnl-utils/include/ccnl-crypto.h"
#include "../../ccnl-nfn/include/ccnl-nfn-ops.h"

//#include "../include/randomGPS.h"
//#include "../include/randomVICTIMS.h"
//#include "../include/randomPLUG.h"
//#include "../include/randomSURVIVORS.h"

#ifndef CCN_LITE_MKC_OUT_SIZE
#define CCN_LITE_MKC_OUT_SIZE (65 * 1024)
#endif

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
/*static void _mkdir(const char *dir) {
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
}*/


struct ccnl_sensor_setting_s *
ccnl_sensor_settings_new(unsigned int id, unsigned int type, unsigned int sasamplingRate, unsigned int name, char* socketPath) {
    struct ccnl_sensor_setting_s *s = (struct ccnl_sensor_setting_s *) ccnl_calloc(1,
                                                                                   sizeof(struct ccnl_sensor_setting_s));
    s->id = id;
    s->name = name;
    s->type = type;
    s->samplingRate = sasamplingRate;
    if(strchr(socketPath, '/'))
        s->use_udp = 1;
    else
        s->use_udp = 0;
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

/*int get_int_len (int value){
    int l=1;
    while(value>9){ l++; value/=10; }
    return l;
}*/

struct ccnl_sensor_tuple_s*
        ccnl_sensor_generate_random_victims_tuple(struct ccnl_sensor_s* sensor){
 // <Date, ID, SequenceNumber, Gender, Age>
    sensor->sampleCounter++;
 int sequenceNumber = sensor->sampleCounter;
 time_t t ;
    struct tm *tmp;
    char date[50];
    char gender;
    time(&t);
    tmp = localtime(&t);
    strftime(date, sizeof(date), "%H:%M:%S.000", tmp);
    srand(time(NULL));
    if((rand() & 1))
        gender = 'M';
    else
        gender = 'F';
    int age = rand() % 100;

    int sizeOfTuple = strlen(date) + get_int_len(age) + get_int_len(sequenceNumber) + get_int_len(sensor->settings->id) + 6;
    char newTuple[sizeOfTuple];
    snprintf(newTuple, sizeof(newTuple), "%s/%i/%i/%c/%i", date, sensor->settings->id, sequenceNumber, gender, age);
    return ccnl_sensor_tuple_new(newTuple,sizeOfTuple);
}

struct ccnl_sensor_tuple_s*
ccnl_sensor_generate_random_survivors_tuple(struct ccnl_sensor_s* sensor){
    // <Date, SequenceNumber, Gender, Age>
    sensor->sampleCounter++;
    int sequenceNumber = sensor->sampleCounter;
    time_t t ;
    struct tm *tmp;
    char date[50];
    char gender;
    time(&t);
    tmp = localtime(&t);
    strftime(date, sizeof(date), "%H:%M:%S.000", tmp);
    srand(time(NULL));
    if((rand() & 1))
        gender = 'M';
    else
        gender = 'F';
    int age = rand() % 100;

    int sizeOfTuple = strlen(date) + get_int_len(age) + get_int_len(sequenceNumber) + get_int_len(sensor->settings->id) + 6;
    char newTuple[sizeOfTuple];
    snprintf(newTuple, sizeof(newTuple), "%s/%i/%i/%c/%i", date, sensor->settings->id, sequenceNumber, gender, age);
    return ccnl_sensor_tuple_new(newTuple, sizeOfTuple);

}

struct ccnl_sensor_tuple_s*
ccnl_sensor_generate_random_gps_tuple(struct ccnl_sensor_s* sensor){
    // <Date, Identifier, Latitude, Longitude, Altitude, Accuracy, Distance, Speed>

    time_t t ;
    struct tm *tmp;
    char date[50];
    time(&t);
    tmp = localtime(&t);
    strftime(date, sizeof(date), "%Y-%m-%d %H:%M:%S", tmp);
    double latitude;
    double longitude;
    int identifier = sensor->settings->id;
    int altitude = 180;
    int accuracy = 1;
    int distance = 1;
    int speed = 1;


    srand ( time ( NULL));

    latitude = (double)rand()/RAND_MAX*2.0-90.0;//float in range -1 to 1
    longitude = (double)rand()/RAND_MAX*2.0-180.0;//float in range -1 to 1
    int sizeOfTuple = strlen(date) + get_int_len(identifier) + 11 + 10 + + get_int_len(altitude) + get_int_len(accuracy) + get_int_len(distance) + get_int_len(speed) + 1 + 7;
    char newTuple[sizeOfTuple];
    snprintf(newTuple, sizeof(newTuple),"%s;%i;%f;%f;%i;%i;%i;%i",date,identifier,latitude,longitude,altitude,accuracy,distance,speed);
    return ccnl_sensor_tuple_new(newTuple, sizeOfTuple);
}

struct ccnl_sensor_tuple_s*
ccnl_sensor_generate_random_plug_tuple(struct ccnl_sensor_s* sensor){
    // <SequenceNumber, Date, Value, Property, Plug_ID, Household_ID, House_ID>
    int sequenceNumber = sensor->sampleCounter +1;
    time_t t ;
    struct tm *tmp;
    char date[50];
    char gender;
    time(&t);
    tmp = localtime(&t);
    strftime(date, sizeof(date), "%Y-%m-%d %H:%M:%S", tmp);
    srand(time(NULL));
    if((rand() & 1))
        gender = 'M';
    else
        gender = 'F';
    int age = rand() % 100;

    int SizeOfTuple = strlen(date) + get_int_len(age) + get_int_len(sequenceNumber) + 5;
    char newTuple[SizeOfTuple];
    snprintf(newTuple, sizeof(newTuple), "%s;%i;%c,%i", date, sequenceNumber, gender, age);
    return ccnl_sensor_tuple_new(newTuple,SizeOfTuple);
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

char getNodeName(char* socket){
    return toupper(socket[15]);
}

int ccnl_sensor_loop(struct ccnl_sensor_s *sensor) {

    /*sendTuples(sensor);*/
    struct timespec ts/*, tstart, tend*/, resultsleep;
    char *addr = NULL, *ux = NULL;
    struct sockaddr sa;
    int port, cnt=0, sock = 0, suite = CCNL_SUITE_DEFAULT;
    int  socksize;
    char stopPath[100];
    char uri[100];

    snprintf(stopPath, sizeof(stopPath),"/tmp/%s%i/stop",getSensorName(sensor->settings->name),sensor->settings->id);
    // create the name for the data
    if(!sensor->settings->use_udp)
        snprintf(uri, sizeof(uri), "node/node%c/sensor/%s/%i", getNodeName(sensor->settings->socketPath), getSensorName(sensor->settings->name),
                 sensor->settings->id);
    else
        snprintf(uri, sizeof(uri), "node/nodeA/sensor/%s/%i", getSensorName(sensor->settings->name),
                 sensor->settings->id);

    struct ccnl_prefix_s *name;

    // make the prefix from the data name
    name = ccnl_URItoPrefix(uri, suite, NULL, NULL);

    // parse the udp address or else safe the address for the socket path
    if(sensor->settings->use_udp){
        if (ccnl_parseUdp(sensor->settings->socketPath, suite, &addr, &port) != 0) {
            exit(-1);
        }
    }
    else{
        char uxSocketPath[100];
        snprintf(uxSocketPath, sizeof(uxSocketPath),"tmp/%s",sensor->settings->socketPath);
        ux=uxSocketPath;//sensor->settings->socketPath;
    }

    // open the socket
    if (ux) { // use UNIX socket
        struct sockaddr_un *su = (struct sockaddr_un*) &sa;
        su->sun_family = AF_UNIX;
        strcpy(su->sun_path, ux);
        sock = ux_open();
    } else { // UDP
        struct sockaddr_in *si = (struct sockaddr_in*) &sa;
        si->sin_family = PF_INET;
        si->sin_addr.s_addr = inet_addr(addr);
        si->sin_port = htons(port);
        sock = udp_open();
    }


    // get the socksize
    if (ux) {
        socksize = sizeof(struct sockaddr_un);
    } else {
        socksize = sizeof(struct sockaddr_in);
    }

    DEBUGMSG(TRACE, "sensor loop started\n");
    ts.tv_sec = sensor->settings->samplingRate / 1000000;
    ts.tv_nsec = (sensor->settings->samplingRate % 1000000) * 1000;
    while (!sensor->stopflag) {
        clock_gettime(CLOCK_MONOTONIC,&resultsleep);
        resultsleep.tv_sec = resultsleep.tv_sec + ts.tv_sec;
        resultsleep.tv_nsec = resultsleep.tv_nsec + ts.tv_nsec;
        //clock_gettime(CLOCK_MONOTONIC,&tstart);
        cnt++;
        ccnl_sensor_sample(sensor, name, sa, sock, socksize);
        DEBUGMSG(EVAL,"sent %i Messages\n",cnt);

        sensorStopped(sensor,stopPath);
        //clock_gettime(CLOCK_MONOTONIC,&tend);
        //resultsleep.tv_sec = ts.tv_sec - (tend.tv_sec - tstart.tv_sec);
        //resultsleep.tv_nsec = ts.tv_nsec - (tend.tv_nsec - tstart.tv_nsec);
        //DEBUGMSG(EVAL,"t nano delta was %lu\n",tdelta.tv_nsec);
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME,&resultsleep, NULL);

    }
    close(sock);
    remove(stopPath);
    ccnl_sensor_free(sensor);

    return 0;
}



int sendTuple(struct ccnl_sensor_tuple_s* currentData, struct ccnl_prefix_s* name, struct sockaddr sa, int sock, int socksize){
    unsigned char out[CCN_LITE_MKC_OUT_SIZE];
    unsigned char body[CCN_LITE_MKC_BODY_SIZE];
    int rc, len, offs = CCNL_MAX_PACKET_SIZE;
    //int cnt = 0;

    // copy the tuple content into the body
    snprintf((char*)body, sizeof(body),"%.*s",(int)currentData->datalen,currentData->data);
    // set the length of the body
    len = (int) currentData->datalen +1;
    // free the allocated memory for the tuple
    ccnl_free(currentData);
    // prepend the data stream content
    len = ccnl_ndntlv_prependDataStreamContent(name, body, len, NULL, NULL, &offs, out);


    //Now we send it

    rc = sendto(sock, out+offs, len, 0, (struct sockaddr*)&sa, socksize);
    /*
    while(cnt < 100){
        //since the content is at the end of the buffer we send only the end.
        rc = sendto(sock, out+offs, len, 0, (struct sockaddr*)&sa, socksize);
        cnt++;
    }*/
    return rc;
}



void ccnl_sensor_sample(struct ccnl_sensor_s *sensor, struct ccnl_prefix_s* name, struct sockaddr sa, int sock, int socksize) {
    struct ccnl_sensor_tuple_s* currentData;
    DEBUGMSG(INFO, "Sample sensor with sampling rate of %i microseconds\n", sensor->settings->samplingRate);
    struct timeval tv;
    gettimeofday(&tv, NULL);
    int returnVal = 0;

    if(sensor->settings->type==CCNL_SENSORTYPE_EMULATION){
        DEBUGMSG(DEBUG,"Getting current trace data.\n");
        currentData = sensor->sensorData;
        DBL_LINKED_LIST_REMOVE_FIRST(sensor->sensorData);
        ccnl_free(currentData);
        if(!sensor->sensorData)
            sensor->stopflag=1;
    }
    else{
        DEBUGMSG(DEBUG,"Generating random data.\n");
        switch(sensor->settings->name){
            case CCNL_SENSORNAME_VICTIMS:
                currentData = ccnl_sensor_generate_random_victims_tuple(sensor);
                break;
            case CCNL_SENSORNAME_SURVIVORS:
                currentData = ccnl_sensor_generate_random_survivors_tuple(sensor);
                break;
            case CCNL_SENSORNAME_GPS:
                currentData = ccnl_sensor_generate_random_gps_tuple(sensor);
                break;
            case CCNL_SENSORNAME_PLUG:
                currentData = ccnl_sensor_generate_random_plug_tuple(sensor);
                break;
            default:
                currentData = ccnl_sensor_generate_random_victims_tuple(sensor);
                //snprintf(tupleData, sizeof(tupleData),"Testdata %02d:%02d:%02d.%03d", tm->tm_hour, tm->tm_min, tm->tm_sec, (int) (tv.tv_usec / 1000));
                break;
        }
    }
    returnVal = sendTuple(currentData,name, sa, sock, socksize);
    DEBUGMSG(DEBUG,"Sendto returned %i",returnVal);
    long            ns; // Nanoseconds
    time_t          s;  // Seconds
    struct timespec spec;
    clock_gettime(CLOCK_REALTIME, &spec);
    s  = spec.tv_sec;
    ns =  spec.tv_nsec;
    DEBUGMSG(EVAL,"Current time: %"PRIdMAX".%09ld seconds since the Epoch\n",
             (intmax_t)s, ns);
}
