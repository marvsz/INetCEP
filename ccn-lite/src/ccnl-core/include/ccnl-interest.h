/*
 * @f ccnl-interest.h
 * @b CCN lite (CCNL), core header file (internal data structures)
 *
 * Copyright (C) 2011-17  University of Basel
 * Copyright (C) 2018     HAW Hamburg
 * Copyright (C) 2018     Safety IO
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

#ifndef CCNL_INTEREST_H
#define CCNL_INTEREST_H

#include "ccnl-pkt.h"
#include "ccnl-face.h"

#ifdef CCNL_RIOT
#include "evtimer_msg.h"
#endif

/**
 * ?
 */
#ifndef CCNL_PIT_COREPROPAGATES
#define CCNL_PIT_COREPROPAGATES    0x01
#endif

/**
 * @brief A pending interest linked list element
 */
struct ccnl_pendint_s {
    struct ccnl_pendint_s *next; /**< pointer to the next list element */
    struct ccnl_face_s *face;    /** */
    uint32_t last_used;          /** */
};

/**
 * @brief A interest linked list element
 */
struct ccnl_interest_s {
    struct ccnl_interest_s *next;       /**< pointer to the next list element */
    struct ccnl_interest_s *prev;       /**< pointer to the previous list element */
    struct ccnl_pkt_s *pkt;             /**< the packet the interests originates from (?) */
    struct ccnl_face_s *from;           /**< the face the interest was received from */
    struct ccnl_pendint_s *pending;     /**< linked list of faces wanting that content */
    unsigned short flags;
    uint32_t lifetime;                  /**< interest lifetime */
#define CCNL_PIT_COREPROPAGATES    0x01
    uint32_t last_used;                 /**< last time the entry was used */
    int retries;
#ifdef USE_NFN_REQUESTS
    struct ccnl_interest_s *keepalive; // the keepalive interest dispatched for this interest
    struct ccnl_interest_s *keepalive_origin; // the interest that dispatched this keepalive interest 
#endif
#ifdef CCNL_RIOT
    evtimer_msg_event_t evtmsg_retrans; /**< retransmission timer */
    evtimer_msg_event_t evtmsg_timeout; /**< timeout timer for (?) */
#endif
};

/**
 * ?
 *
 * @param[in] ccnl
 * @param[in] from
 * @param[in] pkt
 *
 * @return
 */
struct ccnl_interest_s*
ccnl_interest_new(struct ccnl_relay_s *ccnl, struct ccnl_face_s *from,
                  struct ccnl_pkt_s **pkt);

/**
 * Checks if two interests are the same
 *
 * @param[in] i
 * @param[in] pkt
 *
 * @return 0
 * @return 1
 * @return -1 if \ref i was NULL
 * @return -2 if \ref pkt was NULL
 */
int
ccnl_interest_isSame(struct ccnl_interest_s *i, struct ccnl_pkt_s *pkt);

/**
 * Adds a pending interest
 *
 * @param[in] i
 * @param[in] face
 *
 * @return 0
 * @return 1
 * @return -1 if \ref i was NULL
 * @return -2 if \ref face was NULL
 */
int
ccnl_interest_append_pending(struct ccnl_interest_s *i, struct ccnl_face_s *from);

/**
 * Removes a pending interest
 *
 * @param[in] i
 * @param[in] face
 *
 * @return 0
 * @return 1
 * @return -1 if \ref i was NULL
 * @return -2 if \ref face was NULL
 */
int
ccnl_interest_remove_pending(struct ccnl_interest_s *i, struct ccnl_face_s *face);

#endif //CCNL_INTEREST_H
