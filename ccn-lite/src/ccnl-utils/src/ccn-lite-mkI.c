/*
 * @f util/ccn-lite-mkI.c
 * @b CLI mkInterest, write to Stdout
 *
 * Copyright (C) 2013-15, Christian Tschudin, University of Basel
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
 * 2013-07-06  created
 */

#include "ccnl-common.h"

// ----------------------------------------------------------------------

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

int typeToInt(char* type){
    if(!strncmpci(type,"Interest",8))
        return 1;
    if(!strncmpci(type,"ConstantInterest",16))
        return 2;
    if(!strncmpci(type,"removeConstantInterest",22))
        return 3;
    return 0;
}

int
main(int argc, char *argv[])
{

    char *minSuffix = 0, *maxSuffix = 0, *scope = 0;
    char *digest = 0, *publisher = 0;
    char *fname = 0;
    char *type = 0;
    int f, opt;
    int dlen = 0, plen = 0;
    int packettype = CCNL_SUITE_NDNTLV;
    struct ccnl_prefix_s *prefix;
    time_t curtime;
    uint32_t nonce;
    int isLambda = 0;
    unsigned int chunknum = UINT_MAX;
    ccnl_interest_opts_u int_opts;
    struct ccnl_buf_s *buf = NULL;

    (void) minSuffix;
    (void) maxSuffix;
    (void) scope;

    time(&curtime);
    // Get current time in double to avoid dealing with time_t
    nonce = (uint32_t) difftime(curtime, 0);

    while ((opt = getopt(argc, argv, "ha:c:d:e:i:ln:o:t:p:s:v:x:")) != -1) {
        switch (opt) {
        case 'a':
            minSuffix = optarg;
            break;
        case 'c':
            scope = optarg;
            break;
        case 'd':
            digest = optarg;
            dlen = unescape_component(digest);
            if (dlen != 32) {
                DEBUGMSG(ERROR, "digest has wrong length (%d instead of 32)\n",
                        dlen);
                exit(-1);
            }
            break;
        case 'e':
            nonce = atoi(optarg);
            break;
        case 't':
            type = optarg;
            break;
        case 'l':
            isLambda = 1 - isLambda;
            break;
        case 'n':
            chunknum = atoi(optarg);
            break;
        case 'o':
            fname = optarg;
            break;
        case 'p':
            publisher = optarg;
            plen = unescape_component(publisher);
            if (plen != 32) {
                DEBUGMSG(ERROR,
                 "publisher key digest has wrong length (%d instead of 32)\n",
                 plen);
                exit(-1);
            }
            break;
        case 'v':
#ifdef USE_LOGGING
            if (isdigit(optarg[0]))
                debug_level = atoi(optarg);
            else
                debug_level = ccnl_debug_str2level(optarg);
#endif
            break;
        case 'x':
            maxSuffix = optarg;
            break;
        case 's':
            packettype = ccnl_str2suite(optarg);
            if (packettype >= 0 && packettype < CCNL_SUITE_LAST)
                break;
        /* falls through */
        case 'h':
        /* falls through */
        default:
Usage:
            fprintf(stderr, "usage: %s [options] URI [NFNexpr]\n"
            "  -a LEN     miN additional components\n"
            "  -c SCOPE\n"
            "  -t TYPE the type of interest. Either Interest, ConstantInterest, or RemoveConstantInterest\n"
            "  -d DIGEST  content digest (sets -x to 0)\n"
            "  -e NONCE   random 4 bytes\n"
            "  -l         URI is a Lambda expression\n"
            "  -n CHUNKNUM positive integer for chunk interest\n"
            "  -o FNAME   output file (instead of stdout)\n"
            "  -p DIGEST  publisher fingerprint\n"
            "  -s SUITE   (ccnb, ccnx2015, ndn2013)\n"
#ifdef USE_LOGGING
            "  -v DEBUG_LEVEL (fatal, error, warning, info, debug, verbose, trace)\n"
#endif
            "  -x LEN     maX additional components\n",
            argv[0]);
            exit(1);
        }
        /* falls through */
    }

    if (!argv[optind])
        goto Usage;

    /*
    if (isLambda)
        i = ccnl_lambdaStrToComponents(prefix, argv[optind]);
    else
    */
    prefix = ccnl_URItoPrefix(argv[optind],
                              packettype,
                              argv[optind+1],
                              chunknum == UINT_MAX ? NULL : &chunknum);
    if (!prefix) {
        DEBUGMSG(ERROR, "no URI found, aborting\n");
        return -1;
    }

    prefix->suite = packettype;
#ifdef USE_SUITE_NDNTLV
    int_opts.ndntlv.nonce = nonce;
    int_opts.ndntlv.interestlifetime = 2000; // 2000 milliseconds = 2 seconds
#endif
    switch(typeToInt(type)){
        case 1:
            buf = ccnl_mkSimpleInterest(prefix, &int_opts,NDN_TLV_Interest);
            break;
        case 2:
            buf = ccnl_mkSimpleInterest(prefix, &int_opts, NDN_TLV_PersistentInterest);
            break;
        case 3:
            buf = ccnl_mkSimpleInterest(prefix, &int_opts, NDN_TLV_RemovePersistentInterest);
            break;
        case 0:
        default:
            goto Usage;

    }


    if (buf->datalen <= 0) {
        DEBUGMSG(ERROR, "internal error: empty packet\n");
        return -1;
    }

    if (fname) {
        f = creat(fname, 0666);
        if (f < 0) {
            perror("file open:");
            return -1;
        }
    } else
        f = 1;

    write(f, buf->data, buf->datalen);
    close(f);

    return 0;
}

// eof
