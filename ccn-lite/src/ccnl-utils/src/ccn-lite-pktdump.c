/*
 * @f util/ccn-lite-pktdump.c
 * @b CCN lite - dumps CCNB, CCN-TLV and NDN-TLV encoded packets
 *               as well as RPC data structures
 *
 * Copyright (C) 2014-15, Christian Tschudin, University of Basel
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * File history:
 * 2014-03-29 created (merging three old dump programs)
 *
 */

#include "base64.h"
#include "ccnl-common.h"
#include "../../ccnl-dump/include/ccn-lite-pktdump-util.h"

// ----------------------------------------------------------------------


#ifdef OBSOLETE
void
ccntlv_201411(unsigned char *data, int len, int rawxml, FILE* out)
{
    unsigned char *buf;
    char *mp;
    unsigned short hdrlen, payloadlen;
    struct ccnx_tlvhdr_ccnx201411_s *hp;

    hp = (struct ccnx_tlvhdr_ccnx201411_s*) data;
    hdrlen = hp->hdrlen; // ntohs(hp->hdrlen);
    payloadlen = ntohs(hp->payloadlen);

    if (!rawxml)
        fprintf(out, "%04zx  hdr.vers=%d\n",
            (unsigned char*) &(hp->version) - data, hp->version);
    if (hp->packettype == CCNX_PT_Interest)
        if (!rawxml)
            mp = "Interest\\toplevelCtx";
        else
            mp = "Interest";
    else if (hp->packettype == CCNX_PT_ContentObject)
        if (!rawxml)
            mp = "Object\\toplevelCtx";
        else
            mp = "Object";
    else
        mp = "unknown";
    if (!rawxml) {
        fprintf(out, "%04zx  hdr.mtyp=0x%02x (%s)\n",
                (unsigned char*) &(hp->packettype) - data, hp->packettype, mp);
        fprintf(out, "%04zx  hdr.paylen=%d\n",
                (unsigned char*) &(hp->payloadlen) - data, payloadlen);
        fprintf(out, "%04zx  hdr.hdrlen=%d\n",
                (unsigned char*) &(hp->hdrlen) - data, hdrlen);
    }

    if (hdrlen + payloadlen != len) {
        fprintf(stderr, "length mismatch\n");
    }

    buf = data + 8;
    // dump the sequence of TLV fields of the optional header
    len = hp->hdrlen; // ntohs(hp->hdrlen);
    // if (len > 0) {
    //     ccntlv_parse_sequence(0, CTX_HOP, data, &buf, &len,
    //                                                     "header", rawxml, out);
    //     if (len != 0) {
    //         fprintf(stderr, "%d left over bytes in header\n", len);
    //     }
    // }
    if (!rawxml)
        fprintf(out, "%04zx  hdr.end\n", buf - data);

    // dump the sequence of TLV fields of the message (formerly called payload)
    len = payloadlen;
    buf = data + hdrlen;
    ccntlv_parse_sequence(0, CTX_TOPLEVEL, data, &buf, &len,
                                                        "message", rawxml, out);
    if (!rawxml)
        fprintf(out, "%04zx  pkt.end\n", buf - data);
}
#endif


// ----------------------------------------------------------------------

#ifndef USE_JNI_LIB

int
main(int argc, char *argv[]) {
    int opt, rc;
    unsigned char data[64 * 1024];
    int len, maxlen, suite = -1, format = 0;
    FILE *out = stdout;

    while ((opt = getopt(argc, argv, "hs:f:v:")) != -1) {
        switch (opt) {
            case 's':
                suite = ccnl_str2suite(optarg);
                if (!ccnl_isSuite(suite))
                    goto help;
                break;
            case 'f':
                format = atoi(optarg);
                break;
            case 'v':
#ifdef USE_LOGGING
                if (isdigit(optarg[0]))
                    debug_level = atoi(optarg);
                else
                    debug_level = ccnl_debug_str2level(optarg);
#endif
                break;
            default:
            help:
                fprintf(stderr,
                        "usage: %s [options] <encoded_data\n"
                        "  -f FORMAT    (0=readable, 1=rawxml, 2=content, 3=content+newline)\n"
                        "  -h           this help\n"
                        "  -s SUITE     (ccnb, ccnx2015, ndn2013)\n"
                        #ifdef USE_LOGGING
                        "  -v DEBUG_LEVEL (fatal, error, warning, info, debug, verbose, trace)\n"
#endif
                        ,
                        argv[0]);
                exit(1);
        }
    }

    if (argv[optind])
        goto help;

    len = 0;
    maxlen = sizeof(data);
    while (maxlen > 0) {
        rc = read(0, data + len, maxlen);
        if (rc == 0)
            break;
        if (rc < 0) {
            perror("read");
            return 1;
        }
        len += rc;
        maxlen -= rc;
    }

    if (format == 0)
        printf("# ccn-lite-pktdump, parsing %d byte%s\n", len, len != 1 ? "s" : "");

    return dump_content(0, data, data, len, format, suite, out);
}

#endif

// eof
