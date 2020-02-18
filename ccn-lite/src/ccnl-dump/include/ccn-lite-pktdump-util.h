//
// Created by johannes on 18.10.19.
//

#ifndef CCN_LITE_CCN_LITE_PKTDUMP_UTIL_H
#define CCN_LITE_CCN_LITE_PKTDUMP_UTIL_H
#define NDN_TLV_MAX_TYPE 256

#include <base64.h>
#include <ccnl-common.h>


enum {
    CTX_GLOBAL = 1,
    CTX_TOPLEVEL,
    CTX_MSG,
    CTX_NAME,
    CTX_METADATA,
    CTX_VALIDALGO,
    CTX_VALIDALGODEPEND
};

void
indent(char *s, int lev);

void
hexdump(int lev, unsigned char *base, unsigned char *cp, int len, int rawxml, FILE *out);

void
base64dump(int lev, unsigned char *base, unsigned char *cp, int len, int rawxml, FILE *out);

// ----------------------------------------------------------------------
// CCNB

int
ccnb_deheadAndPrint(int lev, unsigned char *base, unsigned char **buf,
                    int *len, int *num, int *typ, int rawxml, FILE *out);


void
ccnb_parse(int lev, unsigned char *data, int len, int rawxml, FILE *out);


void
ccntlv_2015(int lev, unsigned char *data, int len, int rawxml, FILE *out);


// ----------------------------------------------------------------------
// LOCALRPC

int
localrpc_parse(int lev, unsigned char *base, unsigned char **buf, int *len,
               int rawxml, FILE *out);

// ----------------------------------------------------------------------

int
emit_content_only(unsigned char *start, int len, int suite, int format);

// ----------------------------------------------------------------------

// returns 0 on success, -1 on error, 1 on "warning"
int
dump_content(int lev, unsigned char *base, unsigned char *data,
             int len, int format, int suite, FILE *out);

// ----------------------------------------------------------------------

#endif //CCN_LITE_CCN_LITE_PKTDUMP_UTIL_H
