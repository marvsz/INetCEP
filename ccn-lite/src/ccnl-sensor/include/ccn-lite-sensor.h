//
// Created by johannes on 09.10.19.
//

#ifndef CCN_LITE_CCN_LITE_SENSOR_H
#define CCN_LITE_CCN_LITE_SENSOR_H

#include <ccnl-buf.h>
#include <stdint-gcc.h>

#define CCNL_SENSORTYPE_EMULATION 0x01
#define CCNL_SENSORTYPE_SIMULATION 0x2

#define CCNL_SENSORNAME_VICTIMS 0x01
#define CCNL_SENSORNAME_SURVIVORS 0x02
#define CCNL_SENSORNAME_GPS 0x03
#define CCNL_SENSORNAME_PLUG 0x04

struct ccnl_sensor_setting_s {
    unsigned int id; /** < the id of the sensor, must be unique*/
    unsigned int type; /** < the type of the sensor, possible at the moment are emulation or simulation. */
    unsigned int samplingRate;/** < The rate in seconds at which the sensor emits its data/reading */
    unsigned int name;/** < the name of the sensor,  [Victims, Survivors, GPS, Plug]*/
};

/**
 * @brief creates a sensor setting struct
 *
 * @param id the unique id of the sensor
 * @param type the type of the sensor
 * @param sasamplingRate the sampling rate of the sensor
 * @param name the name of the sensor
 * @return a sensor setting struct
 */
struct ccnl_sensor_setting_s*
ccnl_sensor_settings_new(unsigned int id, unsigned int type, unsigned int sasamplingRate, unsigned int name);

struct ccnl_sensor_tuple_s {
    struct ccnl_sensor_tuple_s* next; /** < a pointer to the next content tuple*/
    struct ccnl_sensor_tuple_s* prev; /** < a pointer to the previous content tuple*/
    struct ccnl_buf_s *buf; /** < the content*/
    unsigned char *content; /** < a pointer to the content */
    int contlen; /** < the length of the content */
};

/**
 *
 * @param data the data, which is one event tuple
 * @param datalen the length of the event tuple
 * @return a sensor tuple struct
 */
struct ccnl_sensor_tuple_s*
ccnl_sensor_tuple_new(struct ccnl_buf_s* data, int datalen);

struct ccnl_sensor_s {
    struct ccnl_sensor_s* next; /** < a pointer to the previous sensor*/
    struct ccnl_sensor_s* prev; /** < a pointer to the next sensor*/
    struct ccnl_sensor_setting_s* settings; /** < the settings of this sensor */
    struct ccnl_sensor_tuple_s* sensorData; /** < a pointer to the first element of the sensor data list*/
    int64_t last_sampled; /** < the last time the sensor was sampled */
};

/**
 *
 * @param ssetings sensor settings
 * @return a sensor struct
 */
struct ccnl_sensor_s*
ccnl_sensor_new(struct ccnl_sensor_setting_s* ssetings);

/**
 * Destroys a sensor
 * @param sensor the sensor to destroy
 */
void ccnl_sensor_free(struct ccnl_sensor_s* sensor);

/**
 *
 * @param sensor the sensor for which we want to load event tuples from a file
 * @param path the path to the file of event tuples / a trace
 */
void populate_sensorData(struct ccnl_sensor_s* sensor, char* path);

/**
 * @brief compares two sensors settings and returns if they are the same.
 *
 * @param sensor1 the first sensor setting
 * @param sensor2 the second sensor setting
 * @return 1 if the sensors are the same, 0 if not
 */
int ccnl_sensor_isSame(struct ccnl_sensor_setting_s* sensor1, struct ccnl_sensor_setting_s* sensor2);


#endif //CCN_LITE_CCN_LITE_SENSOR_H