//
// Created by johannes on 17.10.19.
//

#ifndef CCN_LITE_CCN_LITE_PKTDUMP_H
#define CCN_LITE_CCN_LITE_PKTDUMP_H

int
dump_content(int lev, unsigned char *base, unsigned char *data,
             int len, int format, int suite, FILE *out);

#endif //CCN_LITE_CCN_LITE_PKTDUMP_H
