//
// Created by johannes on 10.10.19.
//

#include "ccnl-common.h"
#include "ccn-lite-sensor.h"
#include <ctype.h> // for `tolower()`
#include <limits.h> // for `INT_MIN`
#include <sys/resource.h>
/*

Case-insensitive string compare (strncmp case-insensitive)
- Identical to strncmp except case-insensitive. See: http://www.cplusplus.com/reference/cstring/strncmp/
- Aided/inspired, in part, by: https://stackoverflow.com/a/5820991/4561887

str1    C string 1 to be compared
str2    C string 2 to be compared
num     max number of chars to compare

return:
(essentially identical to strncmp)
INT_MIN  invalid arguments (one or both of the input strings is a NULL pointer)
<0       the first character that does not match has a lower value in str1 than in str2
 0       the contents of both strings are equal
>0       the first character that does not match has a greater value in str1 than in str2

*/
static inline int strncmpci(const char * str1, const char * str2, size_t num)
{
    int ret_code = INT_MIN;

    size_t chars_compared = 0;

    // Check for NULL pointers
    if (!str1 || !str2)
    {
        goto done;
    }

    // Continue doing case-insensitive comparisons, one-character-at-a-time, of str1 to str2,
    // as long as at least one of the strings still has more characters in it, and we have
    // not yet compared num chars.
    while ((*str1 || *str2) && (chars_compared < num))
    {
        ret_code = tolower((int)(*str1)) - tolower((int)(*str2));
        if (ret_code != 0)
        {
            // The 2 chars just compared don't match
            break;
        }
        chars_compared++;
        str1++;
        str2++;
    }

    done:
    return ret_code;
}

int
main(int argc, char **argv){
    int opt, id, type, samplingRate = -1;
    int suite = CCNL_SUITE_DEFAULT, port;
    char *addr = NULL, *udp = NULL, *socket = NULL;
    char *name = NULL;
    int nameID = -1;
    char *datadir = NULL;
    struct ccnl_sensor_setting_s* setting = NULL;
    struct ccnl_sensor_s* sensor = NULL;
    while((opt = getopt(argc,argv, "hn:i:t:s:n:d:v:u:x:")) != -1){
        switch(opt){
            case 'i':
                id = atoi(optarg);
                break;
            case 't':
                type = atoi(optarg);
                break;
            case 's':
                samplingRate = atoi(optarg);
                break;
            case 'n':
                name = optarg;
                break;
            case 'd':
                datadir = optarg;
                break;
            case 'x':
                socket = optarg;
                break;
            case 'u':
                udp = optarg;
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
                               " -t Type is the type of the sensor. 1 for emulation and 2 for simulation\n"
                               " -s The Sampling rate in milliseconds\n"
                               " -n The name of the sensor. 1 for Victims, 2 for Survivors, 3 for GPS and 4 for Data\n"
                               " -d The directory of the file used with emulation\n"
                               " -v The level of debug Information (fatal, error, warning, info, debug, verbose, trace)\n"
                               " -u a.b.c.d/port  UDP destination (default is 127.0.0.1/6363)\n"
                               " -x The name of the socket to connect the sensor to\n"
                               " -h For Help\n"
                               "Examples:\n"
                               "ccn-lite-mkS -n Victims -i 1 -t 2 -s 500 -x mgmt-nfn-relay-a.sock -v trace\n"
                               "ccn-lite-mkS -n PLUG -i 1 -t 1 -s 1000 -x mgmt-nfn-relay-a.sock -v trace -d /home/johannes/INetCEP/sensors/plug1\n"
                               "ccn-lite-mkS -n GPS -i 1 -t 1 -s 1000 -x mgmt-nfn-relay-a.sock -v trace -d /home/johannes/INetCEP/sensors/gps1\n",
                               argv[0]);
                exit(EXIT_FAILURE);
        }
    }

    if (ccnl_parseUdp(udp, suite, &addr, &port) != 0) {
        exit(-1);
    }
    DEBUGMSG(TRACE, "using udp address %s/%d\n", addr, port);

    /*if (socket) { // use UNIX socket
        struct sockaddr_un *su = (struct sockaddr_un*) &sa;
        su->sun_family = AF_UNIX;
        strcpy(su->sun_path, socket);
        sock = ux_open();
    } else { // UDP
        struct sockaddr_in *si = (struct sockaddr_in*) &sa;
        si->sin_family = PF_INET;
        si->sin_addr.s_addr = inet_addr(addr);
        si->sin_port = htons(port);
        sock = udp_open();
    }*/

    if(id==-1||type==-1||samplingRate==-1||name==NULL||socket==NULL)
        goto usage;
    if(type==1 && datadir==NULL)
        goto usage;
    if(type==2 && datadir!=NULL)
        goto usage;
    else
        if(!strncmpci(name,"Victims",7))
            nameID = 1;
        else
            if(!strncmpci(name,"Survivors",9))
                nameID = 2;
            else
                if(!strncmpci(name,"GPS",3))
                    nameID = 3;
                else
                    if(!strncmpci(name,"PLUG",4))
                        nameID = 4;
                    else
                        goto usage;
    setting = ccnl_sensor_settings_new(id,type,samplingRate,nameID,socket);
    sensor = ccnl_sensor_new(setting);
    if(datadir){
        DEBUGMSG(DEBUG,"found data directory\n");
        populate_sensorData(sensor,datadir);
    }

    DEBUGMSG(TRACE,"Starting Sensor Loop\n");
    ccnl_sensor_loop(sensor);

    return 0;
}
