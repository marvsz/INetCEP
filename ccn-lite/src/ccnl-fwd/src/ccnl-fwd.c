/*
 * @f ccnl-fwd.c
 * @b CCN lite (CCNL), fwd source file (internal data structures)
 *
 * Copyright (C) 2011-17, University of Basel
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
 * 2017-06-16 created
 */
#define _POSIX_C_SOURCE 199309L

#include "../include/ccnl-fwd.h"

#include "../../ccnl-core/include/ccnl-core.h"
#include "../../ccnl-core/include/ccnl-producer.h"
#include "../../ccnl-core/include/ccnl-callbacks.h"

#include "../../ccnl-core/include/ccnl-pkt-util.h"
#ifdef USE_NFN
#include "../../ccnl-nfn/include/ccnl-nfn-common.h"
#endif

#ifndef CCNL_LINUXKERNEL

#include<time.h>
#include "../../ccnl-pkt/include/ccnl-pkt-ccnb.h"
#include "../../ccnl-pkt/include/ccnl-pkt-ccntlv.h"
#include "../../ccnl-pkt/include/ccnl-pkt-ndntlv.h"
#include "../../ccnl-pkt/include/ccnl-pkt-switch.h"
#include <inttypes.h>
#include <ccnl-pkt.h>


#else
#include "../../ccnl-core/include/ccnl-mgmt.h"
#include "../../ccnl-core/include/ccnl-producer.h"
#include "../../ccnl-core/include/ccnl-callbacks.h"
#include "../../ccnl-pkt/include/ccnl-pkt-ccnb.h"
#include "../../ccnl-pkt/include/ccnl-pkt-ccntlv.h"
#include "../../ccnl-pkt/include/ccnl-pkt-ndntlv.h"
#include "../../ccnl-pkt/include/ccnl-pkt-switch.h"
#include <linux/time.h>
#endif

//#include "ccnl-logging.h"


#ifdef NEEDS_PREFIX_MATCHING
struct ccnl_prefix_s* ccnl_prefix_dup(struct ccnl_prefix_s *prefix);
int ccnl_fib_add_entry(struct ccnl_relay_s *relay, struct ccnl_prefix_s *pfx,
                       struct ccnl_face_s *face);
#endif

int
ccnl_content_serve_pendingQueries(struct ccnl_relay_s *relay, struct ccnl_content_s *c){
    struct ccnl_interest_s *i;
    struct ccnl_face_s *f;
    int cnt = 0;
#ifndef CCNL_LINUXKERNEL
    char s[CCNL_MAX_PREFIX_SIZE];
#else
    char *s;
#endif
    DEBUGMSG_CORE(TRACE, "ccnl_content_serve_pendingQueries\n");
    for (f = relay->faces; f; f = f->next){
        f->flags &= ~CCNL_FACE_FLAGS_SERVED; // reply on a face only once
    }
    for (i = relay->pit;i;){
        struct ccnl_pendint_s *pi;
        struct ccnl_pendQ_s *pq;
        struct ccnl_interest_s *qi;

        if(!i->pkt->pfx)
            continue;
        switch (i->pkt->pfx->suite) {
#ifdef USE_SUITE_CCNB
            case CCNL_SUITE_CCNB:
                if (!ccnl_i_prefixof_c(i->pkt->pfx, i->pkt->s.ccnb.minsuffix,
                                       i->pkt->s.ccnb.maxsuffix, c)) {
                    // XX must also check i->ppkd
                    i = i->next;
                    continue;
                }
                break;
#endif
#ifdef USE_SUITE_CCNTLV
            case CCNL_SUITE_CCNTLV:
                if (ccnl_prefix_cmp(c->pkt->pfx, NULL, i->pkt->pfx, CMP_EXACT)) {
                    // XX must also check keyid
                    i = i->next;
                    continue;
                }
                break;
#endif
#ifdef USE_SUITE_NDNTLV
            case CCNL_SUITE_NDNTLV:
                if (!ccnl_i_prefixof_c(i->pkt->pfx, i->pkt->s.ndntlv.minsuffix,
                                       i->pkt->s.ndntlv.maxsuffix, c)) {
                    // XX must also check i->ppkl,
                    i = i->next;
                    continue;
                }
                break;
#endif
            default:
                i = i->next;
                continue;
        }

        if(!i->pendingQueries)
            DEBUGMSG(DEBUG,"No pending Queries found.\n");
        for (pq = i->pendingQueries; pq; pq = pq->next) {
#ifndef CCNL_LINUXKERNEL
            DEBUGMSG(DEBUG,"found pending Query for interest %s\n",ccnl_prefix_to_str(i->pkt->pfx,s,CCNL_MAX_PREFIX_SIZE));
#else
            DEBUGMSG(DEBUG,"found pending Query for interest %s\n",(s = ccnl_prefix_to_path(i->pkt->pfx)));
#endif

            if(!pq->query)
                DEBUGMSG(DEBUG,"No query for PQ found.\n");
            for (qi = pq->query; qi; qi = qi->next) {
                for (pi = qi->pending; pi; pi = pi->next) {
                    if (pi->face->flags & CCNL_FACE_FLAGS_SERVED) // face already served? continue
                        continue;
                    pi->face->flags |= CCNL_FACE_FLAGS_SERVED; // else
                    cnt = cnt + 1;
                    struct ccnl_pkt_s* duplicateNFNInterest=ccnl_pkt_dup(qi->pkt);
                    ccnl_fwd_handleInterest(relay, pi->face, &duplicateNFNInterest, ccnl_ndntlv_cMatch);
                }
            }
        }
        i = i->next;

    }
    DEBUGMSG_CORE(TRACE, "ccnl_content_serve_pendingQueries done\n");

    return cnt;
}
// returning 0 if packet was
int
ccnl_fwd_handleContent(struct ccnl_relay_s *relay, struct ccnl_face_s *from,
                       struct ccnl_pkt_s **pkt)
{
    struct ccnl_content_s *c;
#ifndef CCNL_LINUXKERNEL
    char s[CCNL_MAX_PREFIX_SIZE];
    (void) s;
#endif


#ifdef USE_NFN
    int nonce = 0;
    if (pkt != NULL && (*pkt) != NULL && (*pkt)->s.ndntlv.nonce != NULL) {
        if ((*pkt)->s.ndntlv.nonce->datalen == 4) {
            nonce = *((int*)(*pkt)->s.ndntlv.nonce->data);
        }
    }

    if (from) {
        char *from_as_str = ccnl_addr2ascii(&(from->peer));

        if (from_as_str) {
#ifndef CCNL_LINUXKERNEL
            DEBUGMSG_CFWD(INFO, "  incoming data=<%s>%s (nfnflags=%d) nonce=%i from=%s\n",
                          ccnl_prefix_to_str((*pkt)->pfx,s,CCNL_MAX_PREFIX_SIZE), ccnl_suite2str((*pkt)->suite),
                          (*pkt)->pfx->nfnflags, nonce, from_as_str ? from_as_str : "");
#else
            char *s = NULL;
            DEBUGMSG_CFWD(INFO, "  incoming data=<%s>%s (nfnflags=%d) nonce=%i from=%s\n",
                          (s = ccnl_prefix_to_path((*pkt)->pfx)), ccnl_suite2str((*pkt)->suite),
                          (*pkt)->pfx->nfnflags, nonce, from_as_str ? from_as_str : "");

            //ccnl_free(s);
#endif

        } 
    } else {
#ifndef CCNL_LINUXKERNEL
        DEBUGMSG_CFWD(INFO, "  incoming data=<%s>%s (nfnflags=%d) nonce=%i from=%s\n",
                      ccnl_prefix_to_str((*pkt)->pfx,s,CCNL_MAX_PREFIX_SIZE), ccnl_suite2str((*pkt)->suite),
                      (*pkt)->pfx->nfnflags, nonce, "");
#else
        char *s = NULL;
        DEBUGMSG_CFWD(INFO, "  incoming data=<%s>%s (nfnflags=%d) nonce=%i from=%s\n",
                      (s = ccnl_prefix_to_path((*pkt)->pfx)), ccnl_suite2str((*pkt)->suite),
                      (*pkt)->pfx->nfnflags, nonce, "");

        //ccnl_free(s);
#endif


    }

    DEBUGMSG_CFWD(INFO, "  data %.*s\n", (*pkt)->contlen, (*pkt)->content);
#else
    if (from) {
        char *from_as_str = ccnl_addr2ascii(&(from->peer));

        if (from_as_str) {
             DEBUGMSG_CFWD(INFO, "  incoming data=<%s>%s from=%s\n",
                ccnl_prefix_to_str((*pkt)->pfx,s,CCNL_MAX_PREFIX_SIZE), ccnl_suite2str((*pkt)->suite),
                  from_as_str ? from_as_str : "");
        }
    } else {
        DEBUGMSG_CFWD(INFO, "  incoming data=<%s>%s from=%s\n",
            ccnl_prefix_to_str((*pkt)->pfx,s,CCNL_MAX_PREFIX_SIZE), ccnl_suite2str((*pkt)->suite), "");

    }
#endif

#if defined(USE_SUITE_CCNB) && defined(USE_SIGNATURES)
//  FIXME: mgmt messages for NDN and other suites?
        if (pkt->pfx->compcnt == 2 && !memcmp(pkt->pfx->comp[0], "ccnx", 4)
                && !memcmp(pkt->pfx->comp[1], "crypto", 6) &&
                from == relay->crypto_face) {
            return ccnl_crypto(relay, pkt->buf, pkt->pfx, from);
        }
#endif /* USE_SUITE_CCNB && USE_SIGNATURES*/

#ifndef CCNL_LINUXKERNEL
        if (ccnl_callback_rx_on_data(relay, from, *pkt)) {
        *pkt = NULL;
        return 0;
    }
#endif
    // CONFORM: Step 1:
    for (c = relay->contents; c; c = c->next) {
        if(!((*pkt)->type == NDN_TLV_Datastream)){
            if (ccnl_prefix_cmp(c->pkt->pfx, NULL, (*pkt)->pfx, CMP_EXACT) == 0) {
                DEBUGMSG_CFWD(TRACE, "  content is duplicate, ignoring\n");
                return 0; // content is dup, do nothing
            }
        }
        else{
            if (ccnl_prefix_cmp(c->pkt->pfx, NULL, (*pkt)->pfx, CMP_EXACT) == 0) {
                DEBUGMSG_CFWD(TRACE, "  content is duplicate, removing old one and storing new one\n");
                c = ccnl_content_remove(relay, c);
                DEBUGMSG_CFWD(TRACE,"  old content removed\n");
                if(!c)
                    break;
            }
        }
    }



#ifdef USE_NFN_REQUESTS
    // Find the original prefix for the intermediate result and use that prefix to cache the content.
    DEBUGMSG_CFWD(DEBUG," Check if it is a nfnprefix Request\n");
    if (ccnl_nfnprefix_isRequest((*pkt)->pfx)) {
        if (!nfn_request_handle_content(relay, from, pkt)) {
            // content was handled completely,
            // no need for further processing or forwarding
            return 0;
        }
    }

#endif

    DEBUGMSG_CFWD(DEBUG," trying to create new content\n");
    c = ccnl_content_new(pkt);
    DEBUGMSG_CFWD(INFO, "data after creating packet %.*s\n", c->pkt->contlen, c->pkt->content);
    if (!c)
        return 0;

     // CONFORM: Step 2 (and 3)
#ifdef USE_NFN
    if (ccnl_nfnprefix_isNFN(c->pkt->pfx)) {
// #ifdef USE_NFN_REQUESTS
//         if (ccnl_nfnprefix_isKeepalive(c->pkt->pfx)) {
//             nfn_request_RX_keepalive(relay, from, c);
//                 // return 0;
//             // DEBUGMSG_CFWD(VERBOSE, "no interests to keep alive found \n");
//         } else {
// #endif // USE_NFN_REQUESTS
            if (ccnl_nfn_RX_result(relay, from, c))
                return 0;   // FIXME: memory leak
            DEBUGMSG_CFWD(VERBOSE, "no running computation found \n");
//#ifdef USE_NFN_REQUESTS
//        }
//#endif // USE_NFN_REQUESTS
    }
#endif
#ifdef USE_NFN_REQUESTS
    if (!ccnl_nfnprefix_isRequest(c->pkt->pfx)) {
#endif
        if (relay->max_cache_entries != 0) { // it's set to -1 or a limit
            DEBUGMSG_CFWD(DEBUG, "  adding content to cache\n");
            ccnl_content_add2cache(relay, c);
            DEBUGMSG_CFWD(INFO, "data after creating packet %.*s\n", c->pkt->contlen, c->pkt->content);
        } else {
            DEBUGMSG_CFWD(DEBUG, "Max Cache entries are %i",relay->max_cache_entries);
            DEBUGMSG_CFWD(DEBUG, "  content not added to cache\n");
            ccnl_content_free(c);
        }
#ifdef USE_NFN_REQUESTS
    } else {
        DEBUGMSG_CFWD(DEBUG, "  not caching nfn request\n");
    }

    int servedQueries = ccnl_content_serve_pendingQueries(relay,c);
    int servedInterests = ccnl_content_serve_pending(relay, c);

    if(!(servedQueries||servedInterests)){ // unsolicited content
        // CONFORM: "A node MUST NOT forward unsolicited data [...]"
        DEBUGMSG_CFWD(DEBUG, "  removed because no matching interest\n");
        ccnl_content_free(c);
        return 0;
    }
#endif

#ifdef USE_RONR
    /* if we receive a chunk, we assume more chunks of this content may be
     * retrieved along the same path */
    if ((c->pkt->pfx->chunknum) && (*(c->pkt->pfx->chunknum) >= 0)) {
        struct ccnl_prefix_s *pfx_wo_chunk = ccnl_prefix_dup(c->pkt->pfx);
        pfx_wo_chunk->compcnt--;
        ccnl_free(pfx_wo_chunk->chunknum);
        pfx_wo_chunk->chunknum = NULL;
        ccnl_fib_add_entry(relay, pfx_wo_chunk, from);
    }
#endif
    return 0;
}

#ifdef USE_FRAG
// returning 0 if packet was
int
ccnl_fwd_handleFragment(struct ccnl_relay_s *relay, struct ccnl_face_s *from,
                        struct ccnl_pkt_s **pkt, dispatchFct callback)
{
    unsigned char *data = (*pkt)->content;
    int datalen = (*pkt)->contlen;

    if (from) {
        char *from_as_str = ccnl_addr2ascii(&(from->peer));

        DEBUGMSG_CFWD(INFO, "  incoming fragment (%zd bytes) from=%s\n", 
            (*pkt)->buf->datalen, from_as_str ? from_as_str : "");
    }

    ccnl_frag_RX_BeginEnd2015(callback, relay, from,
                              relay->ifs[from->ifndx].mtu,
                              ((*pkt)->flags >> 2) & 0x03,
                              (*pkt)->val.seqno, &data, &datalen);

    ccnl_pkt_free(*pkt);
    *pkt = NULL;
    return 0;
}
#endif

// ----------------------------------------------------------------------
// returns 0 if packet should not be forwarded further
int
ccnl_pkt_fwdOK(struct ccnl_pkt_s *pkt)
{
    switch (pkt->suite) {
#ifdef USE_SUITE_NDNTLV
    case CCNL_SUITE_NDNTLV:
        return pkt->s.ndntlv.scope > 2;
#endif
    default:
        break;
    }

    return -1;
}

int
ccnl_fwd_handleInterest(struct ccnl_relay_s *relay, struct ccnl_face_s *from,
                        struct ccnl_pkt_s **pkt, cMatchFct cMatch)
{
#ifndef CCNL_LINUXKERNEL
    struct timespec tstart={0,0}, tend={0,0};
    clock_gettime(CLOCK_MONOTONIC,&tstart);
#else
    struct timespec tstart;
    struct timespec tend;
    getrawmonotonic(&tstart);
#endif
    struct ccnl_interest_s *i = NULL;
    struct ccnl_content_s *c = NULL;
    int propagate= 0;
#ifndef CCNL_LINUXKERNEL
    char s[CCNL_MAX_PREFIX_SIZE];
    (void) s;
#endif

    int32_t nonce = 0;
    if (pkt != NULL && (*pkt) != NULL && (*pkt)->s.ndntlv.nonce != NULL) {
        if ((*pkt)->s.ndntlv.nonce->datalen == 4) {
            memcpy(&nonce, (*pkt)->s.ndntlv.nonce->data, 4);
        }
    }

    if (from) {
        char *from_as_str = ccnl_addr2ascii(&(from->peer));
#ifndef CCNL_LINUXKERNEL
        DEBUGMSG_CFWD(INFO, "  incoming interest=<%s>%s nonce=%"PRIi32" from=%s\n",
             ccnl_prefix_to_str((*pkt)->pfx,s,CCNL_MAX_PREFIX_SIZE),
             ccnl_suite2str((*pkt)->suite), nonce,
             from_as_str ? from_as_str : "");
#else
        DEBUGMSG_CFWD(DEBUG, "Before the allocation\n");
        char *s = NULL;
        DEBUGMSG_CFWD(INFO, "  incoming interest=<%s>%s nonce=%d from=%s\n",
                (s = ccnl_prefix_to_path((*pkt)->pfx)),
                ccnl_suite2str((*pkt)->suite), nonce,
                from_as_str ? from_as_str : "");
        DEBUGMSG_CFWD(DEBUG, "Before the free\n");
        //ccnl_free(s);
        DEBUGMSG_CFWD(DEBUG, "After the free\n");
#endif
    }

#ifdef USE_DUP_CHECK

    /*if (ccnl_nonce_isDup(relay, *pkt)) {
    #ifndef CCNL_LINUXKERNEL
        DEBUGMSG_CFWD(DEBUG, "  dropped because of duplicate nonce %"PRIi32"\n", nonce);
    #else
        DEBUGMSG_CFWD(DEBUG, "  dropped because of duplicate nonce %d\n", nonce);
    #endif
        return 0;
    }*/
#endif
#ifndef CCNL_LINUXKERNEL
    if (local_producer(relay, from, *pkt)) {
        return 0;
    }
#endif
#if defined(USE_SUITE_CCNB) //&& defined(USE_MGMT)
    //DEBUGMSG(DEBUG,"USE_SUITE_CCNB and MGMG are activated\n");
    if ((*pkt)->suite == CCNL_SUITE_CCNB && (*pkt)->pfx->compcnt == 4 &&
                                  !memcmp((*pkt)->pfx->comp[0], "ccnx", 4)) {
        DEBUGMSG_CFWD(INFO, "  found a mgmt message\n");
        ccnl_mgmt(relay, (*pkt)->buf, (*pkt)->pfx, from); // use return value? // TODO uncomment
        return 0;
    }
#endif

#ifdef USE_SUITE_NDNTLV
    //DEBUGMSG(DEBUG,"USE_SUITE_NDNTLV activated, looking fo mgmt message\n");
    //DEBUGMSG(DEBUG,"pkt suite is %i, pfx compcnt is %i\n",(*pkt)->suite,(*pkt)->pfx->compcnt);
    if ((*pkt)->suite == CCNL_SUITE_NDNTLV && (*pkt)->pfx->compcnt == 4 &&
        !memcmp((*pkt)->pfx->comp[0], "ccnx", 4)) {
        DEBUGMSG_CFWD(INFO, "  found a mgmt message\n");
#ifdef USE_MGMT
        ccnl_mgmt(relay, (*pkt)->buf, (*pkt)->pfx, from); // use return value?
#endif
        return 0;
    }
#endif

//#ifdef USE_NFN_REQUESTS
//    if ((*pkt)->pfx->nfnflags & CCNL_PREFIX_KEEPALIVE) {
//        DEBUGMSG_CFWD(DEBUG, "  is a keepalive interest\n");
//        if (ccnl_nfn_already_computing(relay, (*pkt)->pfx)) {
//            int internum = ccnl_nfn_intermediate_num(relay, (*pkt)->pfx);
//            DEBUGMSG_CFWD(DEBUG, "  running computation found, highest intermediate result: %i\n", internum);
//            int offset;
//            char reply[16];
//            snprintf(reply, 16, "%d", internum);
//            int size = internum >= 0 ? strlen(reply) : 0;
//            struct ccnl_buf_s *buf  = ccnl_mkSimpleContent((*pkt)->pfx, (unsigned char *)reply, size, &offset);
//            ccnl_face_enqueue(relay, from, buf);
//            return 0;
//        } else {
//            DEBUGMSG_CFWD(DEBUG, "  no running computation found.\n");
//        }
//    }
//#endif

#ifdef USE_NFN_REQUESTS
    if (ccnl_nfnprefix_isRequest((*pkt)->pfx)) {
        if (!nfn_request_handle_interest(relay, from, pkt, cMatch)) {
            // request was handled completely,
            // no need for further processing or forwarding
            return 0;
        }
    }
#endif

    // Step 1: check if it is a remove (Query) Interest
    if((*pkt)->s.ndntlv.isRemovePersistent){
        for (i = relay->pit; i; i = i->next)
            if (ccnl_interest_isSame(i, *pkt)){
                ccnl_interest_remove_pending(i,from);
                if(i->pending == NULL)
                    ccnl_interest_remove(relay,i);
            }
        return 0;
    }

    // Step 2: search in content store
    DEBUGMSG_CFWD(DEBUG, "  searching in CS\n");
    /*if(relay!=NULL){
        DEBUGMSG_CFWD(DEBUG, "Relay is not Null");
    }
    if(relay->contents != NULL){
        DEBUGMSG_CFWD(DEBUG, "contents is not Null");
    }
    else{
        DEBUGMSG_CFWD(DEBUG, "contents is Null");
    }*/

    //DEBUGMSG_CFWD(DEBUG, "  the interest is a constant Interest = %i, 1 is yes, 0 is no.\n", (*pkt)->s.ndntlv.isPersistent);
#ifdef CCNL_LINUXKERNEL
    if(relay != NULL && relay->contents != NULL){
        DEBUGMSG_CFWD(DEBUG, "Appearently both now were not null, going into the loop");
#endif
    for (c = relay->contents; c; c = c->next) {
        if (c->pkt->pfx->suite != (*pkt)->pfx->suite)
            continue;
        if (cMatch(*pkt, c))
            continue;
#ifdef USE_NFN
        if(ccnl_nfnprefix_isNFN((*pkt)->pfx))
            continue;
#endif
        DEBUGMSG_CFWD(DEBUG, "  found matching content %p\n", (void *) c);
        if (from->ifndx >= 0) {
#ifdef USE_NFN_REQUESTS
            struct ccnl_pkt_s *cpkt = c->pkt;
            int matching_start_request = ccnl_nfnprefix_isRequest((*pkt)->pfx)
                                         && (*pkt)->pfx->request->type == NFN_REQUEST_TYPE_START;
            if (matching_start_request) {
                nfn_request_content_set_prefix(c, (*pkt)->pfx);
            }
#endif
#ifdef USE_NFN_MONITOR
            ccnl_nfn_monitor(relay, from, c->pkt->pfx, c->pkt->content,
                                 c->pkt->contlen);
#endif
#ifndef CCNL_LINUXKERNEL
            if (ccnl_callback_tx_on_data(relay, from, *pkt)) {
                continue;
            }
#endif
//HIER DAS HALT FLAG VON DER ZEITMESSUNG SETZEN
#ifndef CCNL_LINUXKERNEL
            clock_gettime(CLOCK_MONOTONIC,&tend);
#else
            getrawmonotonic(&tend);
#endif
            {
            uint64_t timeDifference = tend.tv_nsec - tstart.tv_nsec;//((uint64_t)tend.tv_sec + 1.0e-9*tend.tv_nsec) - ((uint64_t)tstart.tv_sec + 1.0e-9*tstart.tv_nsec);

#ifndef CCNL_LINUXKERNEL
            DEBUGMSG(EVAL,"Handling of Interest package took about %lu nano seconds\n",timeDifference);

#else
            DEBUGMSG(EVAL,"Handling of Interest package took about %llu nano seconds\n",timeDifference);
#endif
            }
            relay->served_content++;
            ccnl_send_pkt(relay, from, c->pkt);
#ifdef USE_NFN_REQUESTS
            c->pkt = cpkt;
#endif

        } else {
#ifdef CCNL_APP_RX
            ccnl_app_RX(relay, c);
#endif
        } //TODO: Johannes: Don't we need to free the interest here since we just send the answer? NO! the interest free thing happens somewhere else, all goot
        if(!(*pkt)->s.ndntlv.isPersistent) // if it is not a constant package we are done and the interest will be removed. otherwise we will constantly need to send it back.
            return 0; // we are done
    }
#ifdef CCNL_LINUXKERNEL
    }
    else{
        DEBUGMSG_CFWD(DEBUG, "Both now were null, did not go into the loop");
    }
#endif
    // CONFORM: Step 3: check whether interest is already known
#ifdef CCNL_LINUXKERNEL
if(relay->pit != NULL){
        DEBUGMSG_CFWD(DEBUG, "pit was not null, did go into the loop to search the PIT");
#endif
    for (i = relay->pit; i; i = i->next)
        if (ccnl_interest_isSame(i, *pkt))
            break;
#ifdef CCNL_LINUXKERNEL
    }
    else{
        DEBUGMSG_CFWD(DEBUG, "pit was null, did not go into the loop");
    }
#endif
    if (!i) {// this is a new/unknown I request: create and propagate
#ifdef CCNL_LINUXKERNEL
        DEBUGMSG_CFWD(DEBUG, "this is a new/unknown I request: create and propagate");
        DEBUGMSG_CFWD(DEBUG, "Check Again, if relay is not noll");
        if(relay == NULL)
            DEBUGMSG_CFWD(DEBUG, "Relay was null");
        else
            DEBUGMSG_CFWD(DEBUG, "Relay was not null");
#endif
#ifdef USE_NFN
        if (ccnl_nfn_RX_request(relay, from, pkt)){
#ifdef CCNL_LINUXKERNEL
            DEBUGMSG_CFWD(DEBUG, "this means: everything is ok and pkt was consumed");
#endif
            return -1; // this means: everything is ok and pkt was consumed
        }

#endif
        propagate = 1;
    }
    if (!ccnl_pkt_fwdOK(*pkt)){
#ifdef CCNL_LINUXKERNEL
        DEBUGMSG_CFWD(DEBUG, "pktfwd returned -1");
#endif
        return -1;
    }

    if (!i) {
#ifdef CCNL_LINUXKERNEL
        DEBUGMSG_CFWD(DEBUG, "create new interest");
#endif
        i = ccnl_interest_new(relay, from, pkt);

#ifdef USE_NFN
#ifndef CCNL_LINUXKERNEL
        DEBUGMSG_CFWD(DEBUG,
                      "  created new interest entry %p (prefix=%s, nfnflags=%d)\n",
                      (void *) i,
                      ccnl_prefix_to_str(i->pkt->pfx,s,CCNL_MAX_PREFIX_SIZE),
                      i->pkt->pfx->nfnflags);
#else
        char *s = NULL;
        DEBUGMSG_CFWD(DEBUG,
                      "  created new interest entry %p (prefix=%s, nfnflags=%d)\n",
                      (void *) i,
                      (s = ccnl_prefix_to_path(i->pkt->pfx)),
                      i->pkt->pfx->nfnflags);
        //ccnl_free(s);
#endif

#else
        DEBUGMSG_CFWD(DEBUG,
                      "  created new interest entry %p (prefix=%s)\n",
                      (void *) i, ccnl_prefix_to_str(i->pkt->pfx,s,CCNL_MAX_PACKET_SIZE));
#endif
    }
    if (i) { // store the I request, for the incoming face (Step 3)
        DEBUGMSG_CFWD(DEBUG, "  appending interest entry %p\n", (void *) i);
        ccnl_interest_append_pending(i, from);
        if(propagate) {
            ccnl_interest_propagate(relay, i);
        }
    }
    return 0;
}

// ----------------------------------------------------------------------

#ifdef USE_SUITE_CCNB

// helper proc: work on a message, top level type is already stripped
int
ccnl_ccnb_fwd(struct ccnl_relay_s *relay, struct ccnl_face_s *from,
              unsigned char **data, int *datalen, int typ)
{
    int rc= -1;
    struct ccnl_pkt_s *pkt;

    DEBUGMSG_CFWD(DEBUG, "ccnb fwd (%d bytes left)\n", *datalen);

    pkt = ccnl_ccnb_bytes2pkt(*data - 2, data, datalen);
    if (!pkt) {
        DEBUGMSG_CFWD(WARNING, "  parsing error or no prefix\n");
        goto Done;
    }
    pkt->type = typ;
    pkt->flags |= typ == CCN_DTAG_INTEREST ? CCNL_PKT_REQUEST : CCNL_PKT_REPLY;

    if (pkt->flags & CCNL_PKT_REQUEST) { // interest
        if (ccnl_fwd_handleInterest(relay, from, &pkt, ccnl_ccnb_cMatch)){
            relay->recieved_interest_pkts++;
        }
            goto Done;
    } else { // content
        if (ccnl_fwd_handleContent(relay, from, &pkt)){
            relay->recieved_data_pkts++;
        }
            goto Done;
    }
    rc = 0;
Done:
    ccnl_pkt_free(pkt);
    return rc;
}

// loops over a frame until empty or error
int
ccnl_ccnb_forwarder(struct ccnl_relay_s *relay, struct ccnl_face_s *from,
                    unsigned char **data, int *datalen)
{
    int rc = 0, num, typ;
    DEBUGMSG_CFWD(DEBUG, "ccnl_ccnb_forwarder: %dB from face=%p (id=%d.%d)\n",
             *datalen, (void*)from, relay->id, from ? from->faceid : -1);

    while (rc >= 0 && *datalen > 0) {
        if (ccnl_ccnb_dehead(data, datalen, &num, &typ) || typ != CCN_TT_DTAG)
            return -1;
        switch (num) {
        case CCN_DTAG_INTEREST:
        case CCN_DTAG_CONTENTOBJ:
            rc = ccnl_ccnb_fwd(relay, from, data, datalen, num);
            continue;
#ifdef OBSOLETE_BY_2015_06
#ifdef USE_FRAG
        case CCNL_DTAG_FRAGMENT2012:
            rc = ccnl_frag_RX_frag2012(ccnl_ccnb_forwarder, relay,
                                       from, data, datalen);
            continue;
        case CCNL_DTAG_FRAGMENT2013:
            rc = ccnl_frag_RX_CCNx2013(ccnl_ccnb_forwarder, relay,
                                       from, data, datalen);
            continue;
#endif
#endif // OBSOLETE
        default:
            DEBUGMSG_CFWD(DEBUG, "  unknown datagram type %d\n", num);
            return -1;
        }
    }
    return rc;
}

#endif // USE_SUITE_CCNB

// ----------------------------------------------------------------------

#ifdef USE_SUITE_CCNTLV

// process one CCNTLV packet, return <0 if no bytes consumed or error
int
ccnl_ccntlv_forwarder(struct ccnl_relay_s *relay, struct ccnl_face_s *from,
                      unsigned char **data, int *datalen)
{
    int payloadlen, rc = -1;
    unsigned short hdrlen;
    struct ccnx_tlvhdr_ccnx2015_s *hp;
    unsigned char *start = *data;
    struct ccnl_pkt_s *pkt;

    DEBUGMSG_CFWD(DEBUG, "ccnl_ccntlv_forwarder: %dB from face=%p (id=%d.%d)\n",
                  *datalen, (void*)from, relay->id, from ? from->faceid : -1);

    if ( (unsigned int) *datalen < sizeof(struct ccnx_tlvhdr_ccnx2015_s) ||
                                                     **data != CCNX_TLV_V1) {
        DEBUGMSG_CFWD(DEBUG, "  short header or wrong version (%d)\n", **data);
        return -1;
    }

    hp = (struct ccnx_tlvhdr_ccnx2015_s*) *data;
    hdrlen = hp->hdrlen; // ntohs(hp->hdrlen);
    if (hdrlen > *datalen) { // not enough bytes for a full header
        DEBUGMSG_CFWD(DEBUG, "  hdrlen too large (%d > %d)\n",
                      hdrlen, *datalen);
        return -1;
    }

    payloadlen = ntohs(hp->pktlen);
    if (payloadlen < hdrlen ||
             payloadlen > *datalen) { // not enough data to reconstruct message
        DEBUGMSG_CFWD(DEBUG, "  pkt too small or too big (%d < %d < %d)\n",
                 hdrlen, payloadlen, *datalen);
        return -1;
    }
    payloadlen -= hdrlen;

    *data += hdrlen;
    *datalen -= hdrlen;

    if (hp->pkttype == CCNX_PT_Interest ||
#ifdef USE_FRAG
        hp->pkttype == CCNX_PT_Fragment ||
#endif
        hp->pkttype == CCNX_PT_NACK) {
        hp->hoplimit--;
        if (hp->hoplimit <= 0) { // drop it
            DEBUGMSG_CFWD(DEBUG, "  pkt dropped because of hop limit\n");
            *data += payloadlen;
            *datalen -= payloadlen;
            return 0;
        }
    }

    DEBUGMSG_CFWD(DEBUG, "ccnl_ccntlv_forwarder (%d bytes left, hdrlen=%d)\n",
                  *datalen, hdrlen);

#ifdef USE_FRAG
    if (hp->pkttype == CCNX_PT_Fragment) {
        uint16_t *sp = (uint16_t*) *data;
        int fraglen = ntohs(*(sp+1));

        if (ntohs(*sp) == CCNX_TLV_TL_Fragment && fraglen == (payloadlen-4)) {
            uint16_t fragfields; // = *(uint16_t *) &hp->fill;
            *data += 4;
            *datalen -= 4;
            payloadlen = fraglen;

            memcpy(&fragfields, hp->fill, 2);
            fragfields = ntohs(fragfields);

            ccnl_frag_RX_BeginEnd2015(ccnl_ccntlv_forwarder, relay, from,
                            relay->ifs[from->ifndx].mtu, fragfields >> 14,
                            fragfields & 0x3fff, data, datalen);

            DEBUGMSG_CFWD(TRACE, "  done (fraglen=%d, payloadlen=%d, *datalen=%d)\n",
                     fraglen, payloadlen, *datalen);
        } else {
            DEBUGMSG_CFWD(DEBUG, "  problem with frag type or length (%d, %d, %d)\n",
                     ntohs(*sp), fraglen, payloadlen);
            *data += payloadlen;
            *datalen -= payloadlen;
        }
        DEBUGMSG_CFWD(TRACE, "  returning after fragment: %d bytes\n", *datalen);
        return 0;
    } else {
        DEBUGMSG_CFWD(TRACE, "  not a fragment, continueing\n");
    }
#endif

    if (!from) {
        DEBUGMSG_CFWD(TRACE, "  local data, datalen=%d\n", *datalen);
    }

    pkt = ccnl_ccntlv_bytes2pkt(start, data, datalen);
    if (!pkt) {
        DEBUGMSG_CFWD(WARNING, "  parsing error or no prefix\n");
        goto Done;
    }
    if (!from) {
        DEBUGMSG_CFWD(TRACE, "  pkt ok\n");
//        goto Done;
    }


    if (hp->pkttype == CCNX_PT_Interest) {
        if (pkt->type == CCNX_TLV_TL_Interest) {
            pkt->flags |= CCNL_PKT_REQUEST;
            // DEBUGMSG_CFWD(DEBUG, "  interest=<%s>\n", ccnl_prefix_to_path(pkt->pfx));
            if (ccnl_fwd_handleInterest(relay, from, &pkt, ccnl_ccntlv_cMatch)){
                relay->recieved_interest_pkts++;
                goto Done;
            }

        } else {
            DEBUGMSG_CFWD(WARNING, "  ccntlv: interest pkt type mismatch %d %d\n",
                          hp->pkttype, pkt->type);
        }
    } else if (hp->pkttype == CCNX_PT_Data) {
        if (pkt->type == CCNX_TLV_TL_Object) {
            pkt->flags |= CCNL_PKT_REPLY;
            relay->recieved_data_pkts++;
            ccnl_fwd_handleContent(relay, from, &pkt);
        } else {
            DEBUGMSG_CFWD(WARNING, "  ccntlv: data pkt type mismatch %d %d\n",
                     hp->pkttype, pkt->type);
        }
    } // else ignore
    rc = 0;
Done:
    ccnl_pkt_free(pkt);

    DEBUGMSG_CFWD(TRACE, "  returning %d bytes\n", *datalen);
    return rc;
}

#endif // USE_SUITE_CCNTLV

// ----------------------------------------------------------------------

#ifdef USE_SUITE_NDNTLV


int
ccnl_ndntlv_forwarder(struct ccnl_relay_s *relay, struct ccnl_face_s *from,
                      unsigned char **data, int *datalen)
{
    int rc = -1, len;
    unsigned int typ;
    unsigned char *start = *data;
    struct ccnl_pkt_s *pkt;

    DEBUGMSG_CFWD(DEBUG, "ccnl_ndntlv_forwarder (%d bytes left)\n", *datalen);

    if (ccnl_ndntlv_dehead(data, datalen, (int*) &typ, &len) || (int) len > *datalen) {
        DEBUGMSG_CFWD(TRACE, "  invalid packet format\n");
        return -1;
    }
    pkt = ccnl_ndntlv_bytes2pkt(typ, start, data, datalen);
    if (!pkt) {
        DEBUGMSG_CFWD(INFO, "  ndntlv packet coding problem\n");
        goto Done;
    }
    pkt->type = typ;
    switch (typ) {
    case NDN_TLV_Interest:
        relay->recieved_interest_pkts++;
        if (ccnl_fwd_handleInterest(relay, from, &pkt, ccnl_ndntlv_cMatch))
            goto Done;

        break;
    case NDN_TLV_PersistentInterest:
        relay->recieved_persistent_interest_pkts++;
        if (ccnl_fwd_handleInterest(relay, from, &pkt, ccnl_ndntlv_cMatch))
            goto Done;
        break;
    case NDN_TLV_RemovePersistentInterest:
        if (ccnl_fwd_handleInterest(relay, from, &pkt, ccnl_ndntlv_cMatch))
            goto Done;

        break;
    case NDN_TLV_Data:
        relay->recieved_data_pkts++;
        if (ccnl_fwd_handleContent(relay, from, &pkt))
            goto Done;
        break;
    case NDN_TLV_Datastream:
        relay->recieved_data_stream_pkts++;
        if (ccnl_fwd_handleContent(relay, from, &pkt))
            goto Done;
        break;
#ifdef USE_FRAG
    case NDN_TLV_Fragment:
        if (ccnl_fwd_handleFragment(relay, from, &pkt, ccnl_ndntlv_forwarder))
            goto Done;
        break;
#endif
    default:
        DEBUGMSG_CFWD(INFO, "  unknown packet type %d, dropped\n", typ);
        break;
    }
    rc = 0;
Done:
    ccnl_pkt_free(pkt);
    return rc;
}

#endif // USE_SUITE_NDNTLV

// ----------------------------------------------------------------------

// insert forwarding entry with a tap - the prefix arg is consumed
int
ccnl_set_tap(struct ccnl_relay_s *relay, struct ccnl_prefix_s *pfx,
             tapCallback callback)
{
    struct ccnl_forward_s *fwd, **fwd2;
#ifndef CCNL_LINUXKERNEL
    char s[CCNL_MAX_PREFIX_SIZE];
    (void) s;

    DEBUGMSG_CFWD(INFO, "setting tap for <%s>, suite %s\n",
                  ccnl_prefix_to_str(pfx,s,CCNL_MAX_PREFIX_SIZE),
                  ccnl_suite2str(pfx->suite));
#else
    char *s = NULL;
    DEBUGMSG_CFWD(INFO, "setting tap for <%s>, suite %s\n",
                  (s = ccnl_prefix_to_path(pfx)),
                  ccnl_suite2str(pfx->suite));
    //ccnl_free(s);
#endif


    for (fwd = relay->fib; fwd; fwd = fwd->next) {
        if (fwd->suite == pfx->suite &&
                        !ccnl_prefix_cmp(fwd->prefix, NULL, pfx, CMP_EXACT)) {
            ccnl_prefix_free(fwd->prefix);
            fwd->prefix = NULL;
            break;
        }
    }
    if (!fwd) {
        fwd = (struct ccnl_forward_s *) ccnl_calloc(1, sizeof(*fwd));
        if (!fwd)
            return -1;
        fwd2 = &relay->fib;
        while (*fwd2)
            fwd2 = &((*fwd2)->next);
        *fwd2 = fwd;
        fwd->suite = pfx->suite;
    }
    fwd->prefix = pfx;
    fwd->tap = callback;
    return 0;
}
