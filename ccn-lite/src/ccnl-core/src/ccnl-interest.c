/*
 * @f ccnl-interest.c
 * @b CCN lite (CCNL), core source file (internal data structures)
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

#ifndef CCNL_LINUXKERNEL
#include "../include/ccnl-interest.h"
#include "../include/ccnl-relay.h"
#include "../include/ccnl-malloc.h"
#include "../include/ccnl-os-time.h"
#include "../include/ccnl-prefix.h"
#include "../include/ccnl-logging.h"
#include "../include/ccnl-pkt-util.h"
#else
#include "../include/ccnl-interest.h"
#include "../include/ccnl-relay.h"
#include "../include/ccnl-malloc.h"
#include "../include/ccnl-os-time.h"
#include "../include/ccnl-prefix.h"
#include "../include/ccnl-logging.h"
#include "../include/ccnl-pkt-util.h"
#endif

#ifdef CCNL_RIOT
#include "../../ccnl-riot/include/ccn-lite-riot.h"
#endif

struct ccnl_interest_s*
ccnl_interest_new(struct ccnl_relay_s *ccnl, struct ccnl_face_s *from,
                  struct ccnl_pkt_s **pkt)
{
#ifndef CCNL_LINUXKERNEL
    char s[CCNL_MAX_PREFIX_SIZE];
    (void) s;
#endif
    DEBUGMSG_CORE(TRACE,"Trying to allocate buffer for interest with the size of %lu",sizeof(struct ccnl_interest_s));
    struct ccnl_interest_s *i = (struct ccnl_interest_s *) ccnl_calloc(1,sizeof(struct ccnl_interest_s));

    if(i == NULL){
        DEBUGMSG(TRACE, "Was not able to allocate memory for new interest");
    }
#ifndef CCNL_LINUXKERNEL

    DEBUGMSG_CORE(TRACE,
                  "ccnl_new_interest(prefix=%s, suite=%s)\n",
                  ccnl_prefix_to_str((*pkt)->pfx, s, CCNL_MAX_PREFIX_SIZE),
                  ccnl_suite2str((*pkt)->pfx->suite));
#else
    char *s = NULL;
    DEBUGMSG_CORE(TRACE,
                  "ccnl_new_interest(prefix=%s, suite=%s)\n",
                  (s = ccnl_prefix_to_path(i->pkt->pfx)),
                  ccnl_suite2str((*pkt)->pfx->suite));
    ccnl_free(s);
#endif
    if (!i)
        return NULL;
    i->pkt = *pkt;
    /* currently, the aging function relies on seconds rather than on milli seconds */
    i->lifetime = ccnl_pkt_interest_lifetime(*pkt);
    i->isConst = ccnl_pkt_interest_isConstant(*pkt);
    i->isRemoveI = ccnl_pkt_interest_isRemoveI(*pkt);
    *pkt = NULL;
    i->flags |= CCNL_PIT_COREPROPAGATES;
    i->from = from;
    i->last_used = CCNL_NOW();
#ifndef CCNL_LINUXKERNEL
#endif
    if(ccnl->max_pit_entries != -1){
        if (ccnl->pitcnt >= ccnl->max_pit_entries) {
            ccnl_pkt_free(i->pkt);
            ccnl_free(i);
            return NULL;
        }
    }
    DBL_LINKED_LIST_ADD(ccnl->pit, i);
    ccnl->pitcnt++;

#ifdef CCNL_RIOT
    ccnl_evtimer_reset_interest_retrans(i);
    ccnl_evtimer_reset_interest_timeout(i);
#endif

    return i;
}

int
ccnl_interest_isSame(struct ccnl_interest_s *i, struct ccnl_pkt_s *pkt)
{
    if (i) {
        if (pkt) {
            if (i->pkt->pfx->suite != pkt->suite || ccnl_prefix_cmp(i->pkt->pfx, NULL, pkt->pfx, CMP_EXACT)) {
                return 0;
            }

            switch (i->pkt->pfx->suite) {
#ifdef USE_SUITE_CCNB
                case CCNL_SUITE_CCNB:
                    return i->pkt->s.ccnb.minsuffix == pkt->s.ccnb.minsuffix && i->pkt->s.ccnb.maxsuffix == pkt->s.ccnb.maxsuffix &&
                           ((!i->pkt->s.ccnb.ppkd && !pkt->s.ccnb.ppkd) || buf_equal(i->pkt->s.ccnb.ppkd, pkt->s.ccnb.ppkd));
#endif

#ifdef USE_SUITE_NDNTLV
                case CCNL_SUITE_NDNTLV:
                    return i->pkt->s.ndntlv.minsuffix == pkt->s.ndntlv.minsuffix && i->pkt->s.ndntlv.maxsuffix == pkt->s.ndntlv.maxsuffix &&
                           ((!i->pkt->s.ndntlv.ppkl && !pkt->s.ndntlv.ppkl) || buf_equal(i->pkt->s.ndntlv.ppkl, pkt->s.ndntlv.ppkl));
#endif
#ifdef USE_SUITE_CCNTLV
                case CCNL_SUITE_CCNTLV:
                    break;
#endif
                default:
                    break;
            }

            return 1;
        }

        return -2;
    }
    return -1;
}


int
ccnl_interest_append_pending(struct ccnl_interest_s *i,  struct ccnl_face_s *from)
{
    if (i) {
        DEBUGMSG_CORE(TRACE, "ccnl_append_pending\n");
        if (from) {
            struct ccnl_pendint_s *pi, *last = NULL;
#ifndef CCNL_LINUXKERNEL
            char s[CCNL_MAX_PREFIX_SIZE];
#endif
            for (pi = i->pending; pi; pi = pi->next) { // check whether already listed
                if (pi->face == from) {
                    DEBUGMSG_CORE(DEBUG, "  we found a matching interest, updating time\n");
                    pi->last_used = CCNL_NOW();
                    return 0;
                }
                last = pi;
            }
            pi = (struct ccnl_pendint_s *) ccnl_calloc(1,sizeof(struct ccnl_pendint_s));
            if (!pi) {
                DEBUGMSG_CORE(DEBUG, "  no mem\n");
                return -1;
            }

#ifndef CCNL_LINUXKERNEL
            DEBUGMSG_CORE(DEBUG, "  appending a new pendint entry %p <%s>(%p)\n",
                          (void *) pi, ccnl_prefix_to_str(i->pkt->pfx,s,CCNL_MAX_PREFIX_SIZE),
                          (void *) i->pkt->pfx);
#else
            char *s = NULL;
            DEBUGMSG_CORE(DEBUG, "  appending a new pendint entry %p <%s>(%p)\n",
                          (void *) pi, (s = ccnl_prefix_to_path(i->pkt->pfx)),
                          (void *) i->pkt->pfx);
            ccnl_free(s);
#endif

            pi->face = from;
            pi->last_used = CCNL_NOW();
            if (last)
                last->next = pi;
            else
                i->pending = pi;
            return 0;
        }
        return -2;
    }

    return -1;
}

int
ccnl_interest_remove_pending(struct ccnl_interest_s *interest, struct ccnl_face_s *face)
{
    /** set result value to error-case */
    int result = -1;

    /** interest is valid? */
    if (interest) {
        /** face is valid? */
        if (face) {
#ifndef CCNL_LINUXKERNEL
            char s[CCNL_MAX_PREFIX_SIZE];
#endif

            result = 0;

            struct ccnl_pendint_s *prev = NULL;
            struct ccnl_pendint_s *pend = interest->pending;

            DEBUGMSG_CORE(TRACE, "ccnl_interest_remove_pending\n");

            while (pend) {  // TODO: is this really the most elegant solution?
                if (face->faceid == pend->face->faceid) {
#ifndef CCNL_LINUXKERNEL
                    DEBUGMSG_CFWD(INFO, "  removed face (%s) for interest %s\n",
                                  ccnl_addr2ascii(&pend->face->peer),
                                  ccnl_prefix_to_str(interest->pkt->pfx,s,CCNL_MAX_PREFIX_SIZE));
#else

                    char *s = NULL;
                    DEBUGMSG_CFWD(INFO, "  removed face (%s) for interest %s\n",
                                  ccnl_addr2ascii(&pend->face->peer),
                                  (s = ccnl_prefix_to_path(interest->pkt->pfx)));
                    ccnl_free(s);
#endif


                    result++;
                    if (prev) {
                        prev->next = pend->next;
                        ccnl_free(pend);
                        pend = prev->next;
                    } else {
                        interest->pending = pend->next;
                        ccnl_free(pend);
                        pend = interest->pending;
                    }
                } else {
                    prev = pend;
                    pend = pend->next;
                }
            }
        return result;
        }
        /** face was NULL */
        result = -2;
    }
    /** interest was NULL */
    return result;
}
