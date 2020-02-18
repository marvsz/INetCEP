//
// Created by johannes on 10.10.19.
//
#include <ctype.h> // for `tolower()`
#include <limits.h> // for `INT_MIN`
#include "ccnl-common.h"

char* stringToLower(char* str){
    for(int i=0; str[i]; i++)
        str[i] = tolower(str[i]);
    return str;
}

int
main(int argc, char **argv){
    int opt;
    int id = -1;
    char* sensorName = NULL;
    char stopPath[100];
    while((opt = getopt(argc,argv, "hn:i:n:v:")) != -1){
        switch(opt){
            case 'i':
                id = atoi(optarg);
                break;
            case 'n':
                sensorName = optarg;
                break;
            case 'v':
                if (isdigit(optarg[0]))
                    debug_level = atoi(optarg);
                else
                    debug_level = ccnl_debug_str2level(optarg);
                break;
            case 'h':
            default:
usage:
                fprintf(stderr,
                        "usage: %s [options]\n"
                        " -i ID is the id of the sensor. It must be unique with together with the name\n"
                        " -n The name of the sensor. 1 for Victims, 2 for Survivors, 3 for GPS and 4 for Data\n"
                        " -v The level of debug Information (fatal, error, warning, info, debug, verbose, trace)\n"
                        " -h For Help\n"
                        "Examples:\n"
                        "ccn-lite-rmS -n GPS -i 1 -v trace\n",
                        argv[0]);
                            exit(EXIT_FAILURE);
        }
    }
    if(id==-1||sensorName==NULL)
        goto usage;
    DEBUGMSG(DEBUG,"Creating Path\n");
    snprintf(stopPath, sizeof(stopPath),"/tmp/%s%i/stop",stringToLower(sensorName),id);
    DEBUGMSG(DEBUG,"File path %s\n",stopPath);
    FILE * fPtr;
    fPtr = fopen(stopPath,"w");
    if(fPtr == NULL){
        DEBUGMSG(DEBUG,"Unable to create file.\n");
        exit(EXIT_FAILURE);
    }
    return 0;
}
