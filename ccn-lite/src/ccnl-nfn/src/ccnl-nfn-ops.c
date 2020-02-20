/*
 * @f ccnl-nfnops.c
 * @b CCN-lite, builtin operations for Krivine's lazy machine
 *
 * Copyright (C) 2014, Christian Tschudin, University of Basel
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
 * 2014-11-01: created
 */
#define _POSIX_C_SOURCE 199309L
#ifdef USE_NFN

#ifdef CCNL_LINUXKERNEL
#include "../include/ccnl-nfn-ops.h"
#include "../include/ccnl-nfn-common.h"
#include "../include/ccnl-nfn-krivine.h"
#include "../../ccnl-core/include/ccnl-os-time.h"
#include "../../ccnl-core/include/ccnl-malloc.h"
#include "../../ccnl-core/include/ccnl-logging.h"
#include <linux/time.h>
#else
#include<time.h>
#include "ccnl-nfn-ops.h"
#include <stdio.h>
#include <bits/types/struct_timespec.h>
#include <time.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include "ccnl-nfn-common.h"
#include "ccnl-nfn-krivine.h"
#include "../../ccnl-pkt/include/ccnl-pkt-ccnb.h"
#include "../../ccnl-pkt/include/ccnl-pkt-builder.h"
#include "ccnl-os-time.h"
#include "ccnl-malloc.h"
#include "ccnl-logging.h"
#endif



char** str_split(char* a_str, const char a_delim)
{
    char** result    = 0;
    size_t count     = 0;
    char* tmp        = a_str;
    char* last_comma = 0;
    char delim[2];
    delim[0] = a_delim;
    delim[1] = 0;

    /* Count how many elements will be extracted. */
    while (*tmp)
    {
        if (a_delim == *tmp)
        {
            count++;
            last_comma = tmp;
        }
        tmp++;
    }

    /* Add space for trailing token. */
    count += last_comma < (a_str + strlen(a_str) - 1);

    /* Add space for terminating null string so caller
       knows where the list of returned strings ends. */
    count++;

    result = ccnl_malloc(sizeof(char*) * count);

    if (result)
    {
        size_t idx  = 0;
        char* token;
#ifndef CCNL_LINUXKERNEL
        token = strtok(a_str, delim);
#else
        token = strsep(&a_str, delim);
#endif


        while (token)
        {
            assert(idx < count);
            *(result + idx++) = ccnl_strdup(token);
#ifndef CCNL_LINUXKERNEL
            token = strtok(0, delim);
#else
            token = strsep(0,delim);
#endif

        }
        assert(idx == count - 1);
        *(result + idx) = 0;
    }

    return result;
}

int get_int_len (int value){
    int l=1;
    while(value>9){ l++; value/=10; }
    return l;
}

/**
 * Creates new packets with the given prefix and returns the last one.
 *
 * @param prefix the prefix of the packet
 * @param buf the content
 * @param len the length of the content
 * @return the packet or the last packet, depending on how many packets are being created.
 */
struct ccnl_pkt_s* createNewPacket(struct ccnl_prefix_s *prefix, char *buf, int len){
    int it, size = CCNL_MAX_PACKET_SIZE/2;
    int numPackets = len/(size/2) + 1;
    //(void) prefix;
    struct ccnl_pkt_s *pkt;

    //DEBUGMSG(DEBUG, "The content of the new Packet should be: %.*s\n",len,buf);

    for(it=0; it < numPackets; ++it){
        unsigned char *buf2;
        int packetsize = size/2, len4 = 0, len5;
        unsigned char *packet = (unsigned char*) ccnl_malloc(sizeof(char)*packetsize * 2);
        len4 += ccnl_ccnb_mkHeader(packet+len4, CCNL_DTAG_FRAG, CCN_TT_DTAG);
        if(it == numPackets -1){
            len4 += ccnl_ccnb_mkStrBlob(packet+len4, CCN_DTAG_ANY, CCN_TT_DTAG, buf);
        }
        len5 = len - it * packetsize;
        if (len5 > packetsize)
            len5 = packetsize;
        len4 += ccnl_ccnb_mkBlob(packet+len4, CCN_DTAG_CONTENTDIGEST,
                                 CCN_TT_DTAG, (char*) buf + it*packetsize,
                                 len5);
        packet[len4++] = 0;
        buf2 = ccnl_malloc(CCNL_MAX_PACKET_SIZE*sizeof(char));
        len5 = ccnl_ccnb_mkHeader(buf2, CCN_DTAG_CONTENTOBJ, CCN_TT_DTAG);   // content
        //len5 += ccnl_ccnb_mkStrBlob(packet+len5, CCN_DTAG_CONTENT, CCN_TT_DTAG, buf);
        memcpy(buf2+len5, packet, len4);
        len5 +=len4;
        buf2[len5++] = 0; // end-of-interest
        /*if(it == 0){
            struct ccnl_buf_s *retbuf;
            DEBUGMSG(TRACE, "  enqueue %d %d bytes\n", len4, len5);
            retbuf = ccnl_buf_new((char *)buf2, len5);
            ccnl_face_enqueue(ccnl, from, retbuf);
        }*/
        //char uri[50];
        int contentpos;
        pkt = ccnl_calloc(1, sizeof(*pkt));
        //pkt->pfx = ccnl_URItoPrefix(uri, CCNL_SUITE_CCNB, NULL, NULL);
        pkt->pfx = prefix;
        pkt->buf = ccnl_mkSimpleContent(pkt->pfx, buf2, len5, &contentpos, NULL);
        pkt->content = pkt->buf->data + contentpos;
        pkt->contlen = len5;
        ccnl_free(buf2);
        ccnl_free(packet);
    }
    return pkt;
}

/**
 * Makes a query persistent in the sense that it will always be executed, when a desired packet with a specific prefix arrives.
 *
 * @param intr the nfn interest that should always be executed when a packet matching the prefix in pfx arrives.
 * @param ccnl the relay.
 * @param pfx the prefix for the packet to which the system shall react to.
 */
void ccnl_makeQueryPersistent(struct ccnl_interest_s* intr, struct ccnl_relay_s *ccnl, struct ccnl_prefix_s* pfx){
    //char* intName = "/nodeA/sensor/gps1";+
    int nonce;
#ifndef CCNL_LINUXKERNEL
    nonce = rand();
#else
    nonce = random();
#endif
    ccnl_interest_opts_u int_opts;
#ifdef USE_SUITE_NDNTLV
    int_opts.ndntlv.nonce = nonce;
#endif
    /*
     * TODO: Search the pit beforehand to maybe already find an entry --> do not create too many!
     */

    struct ccnl_interest_s *i = NULL;
    for(i = ccnl->pit; i; i=i->next){
        DEBUGMSG(DEBUG,"\n");
        if(!ccnl_prefix_cmp(i->pkt->pfx,NULL,pfx,CMP_EXACT)){
            if(!i->isConst){
                DEBUGMSG(DEBUG,"Found already pending Interest, but not a persistent one\n");
                continue;
            }
            DEBUGMSG(DEBUG,"Found already pending constant Interest\n");
            break;
        }
        else{
            DEBUGMSG(DEBUG,"Not the right Interest\n");
            continue;
        }
    }
    if(!i){
        i= ccnl_mkPersistentInterestObject(pfx,&int_opts);
        i->pkt->s = intr->pkt->s;
        DBL_LINKED_LIST_ADD(ccnl->pit, i);
        ccnl->pitcnt++;
    }
    struct ccnl_pkt_s *nfnPacket = ccnl_pkt_dup(intr->pkt);
    struct ccnl_interest_s *nfnInterestPacket = ccnl_interest_dup(intr->from,&nfnPacket);
    ccnl_query_append_pending(i,nfnInterestPacket);
}

// binds the name to the given fct in ZAM's list of known operations
void
ZAM_registerOp(char *name, BIF fct)
{
    struct builtin_s *b;
    struct ccnl_buf_s *buf;

    buf = ccnl_calloc(1, sizeof(*buf) + sizeof(*b));
    ccnl_core_addToCleanup(buf);

    b = (struct builtin_s*) buf->data;
    b->name = name;
    b->fct = fct;
    b->next = op_extensions;

    op_extensions = b;
}

//------------------------------------------------------------
/**
 * used to avoid code duplication for popping two integer values from result stack asynchronous
 * requires to define i1 and i2 before calling to store the results.
 */
#define pop2int() \
        do{     \
            struct stack_s *h1, *h2; \
            h1 = pop_or_resolve_from_result_stack(ccnl, config); \
            if(h1 == NULL){ \
                *halt = -1; \
                return prog; \
            } \
            if(h1->type == STACK_TYPE_INT) i1 = *(int*)h1->content;\
            h2 = pop_or_resolve_from_result_stack(ccnl, config); \
            if(h2 == NULL){ \
                *halt = -1; \
                push_to_stack(&config->result_stack, h1->content, h1->type); \
                ccnl_free(h1); \
                return prog; \
            } \
            if(h1->type == STACK_TYPE_INT) i2 = *(int*)h2->content;\
            ccnl_nfn_freeStack(h1); ccnl_nfn_freeStack(h2); \
        }while(0)

// ----------------------------------------------------------------------
// builtin operations

char*
op_builtin_add(struct ccnl_relay_s *ccnl, struct configuration_s *config,
               int *restart, int *halt, char *prog, char *pending,
               struct stack_s **stack)
{
    int i1=0, i2=0, *h;
#ifndef CCNL_LINUXKERNEL
    struct timespec tstart={0,0}, tend={0,0};
    clock_gettime(CLOCK_MONOTONIC,&tstart);
#else
    struct timespec tstart;
    struct timespec tend;
    getrawmonotonic(&tstart);
#endif
    (void) restart;
    DEBUGMSG(DEBUG, "---to do: OP_ADD <%s> pending: %s\n", prog+7, pending);
    pop2int();
    h = ccnl_malloc(sizeof(int));
    *h = i1 + i2;
    push_to_stack(stack, h, STACK_TYPE_INT);
#ifndef CCNL_LINUXKERNEL
    clock_gettime(CLOCK_MONOTONIC,&tend);
#else
    getrawmonotonic(&tend);
#endif
    {
        uint64_t timeDifference = tend.tv_nsec - tstart.tv_nsec;//((uint64_t)tend.tv_sec + 1.0e-9*tend.tv_nsec) - ((uint64_t)tstart.tv_sec + 1.0e-9*tstart.tv_nsec);

#ifndef CCNL_LINUXKERNEL
        DEBUGMSG(DEBUG,"Builtin Add took about %lu seconds\n",timeDifference);

#else
        DEBUGMSG(DEBUG,"Builtin Kernel Add took about %llu seconds\n",timeDifference);
#endif
    }
    return pending ? ccnl_strdup(pending) : NULL;
}

void
window_purge_old_data(char* retVal, char* stateContent, char* tupleContent, int stateContLen, int quantity, int unit){
    char **intermediateArray = NULL;
    (void) unit;
    DEBUGMSG(DEBUG,"Statr content length was %i\n",stateContLen);
    if(stateContLen){
        intermediateArray = str_split(stateContent, '\n');
    }
    strcpy(retVal,tupleContent);
    strcat(retVal,"\n");



    //DEBUGMSG(DEBUG, "WINDOW: Copied new tuple as the first Element of the result\n");
    //DEBUGMSG(DEBUG, "WINDOW: TEEEEST1\n");
    if(intermediateArray){
        //DEBUGMSG(DEBUG, "WINDOW: IntermediateArray was not null\n");
        int i = 0;
        for(i=0; *(intermediateArray + i + 1); i++){
            //DEBUGMSG(DEBUG, "WINDOW: TEEEEST\n");
            //DEBUGMSG(DEBUG, "WINDOW: Start copying Datatuple %i with content %s\n",i,*(intermediateArray+i));
            strcat(retVal,*(intermediateArray+i));
            strcat(retVal,"\n");
            //DEBUGMSG(DEBUG, "WINDOW: Added the Datatuple %i regularly: %s\n",i,*(intermediateArray+i));
            ccnl_free(*(intermediateArray+i));
        }
        if(i< quantity-1 && intermediateArray){
            //DEBUGMSG(DEBUG, "WINDOW: Start copying Datatuple %i with content %s\n",i,*(intermediateArray+i));
            strcat(retVal,*(intermediateArray+i));
            strcat(retVal,"\n");
            //DEBUGMSG(DEBUG, "WINDOW: Added the Datatuple %i because there was still space left: %s\n",i,*(intermediateArray+i));
            ccnl_free(*(intermediateArray+i));
        }
        //DEBUGMSG(DEBUG, "WINDOW: Freeing Intermediate Result\n");
        ccnl_free(intermediateArray);
    }
        }

char*
op_builtin_window(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                  int *restart, int *halt, char *prog, char *pending,
                  struct stack_s **stack)
{
#ifndef CCNL_LINUXKERNEL
    struct timespec tstart={0,0}, tend={0,0};
    clock_gettime(CLOCK_MONOTONIC,&tstart);
#else
    struct timespec tstart;
    struct timespec tend;
    getrawmonotonic(&tstart);
#endif
    int local_search = 1;
    struct stack_s *streamStack;
    char *cp = NULL;
    struct ccnl_prefix_s *stateprefix;
    struct ccnl_prefix_s *tupleprefix;
    struct ccnl_content_s *state = NULL;
    struct ccnl_content_s *tuple = NULL;

    int i1=0, i2=0;
    int quantity=0;
    int unit=0;
    (void)stack;

    if (*restart) {
        DEBUGMSG(DEBUG, "---to do: OP_WINDOW restart\n");
        *restart = 0;
        local_search = 1;
    } else {

        DEBUGMSG(DEBUG, "---to do: OP_WINDOW <%s> <%s>\n", prog + 7, pending);
        pop2int();
        streamStack = pop_from_stack(&config->result_stack);
        if(streamStack==NULL){
            *halt = -1;
            return prog;
        }
        config->fox_state->num_of_params = 1;
        config->fox_state->params = ccnl_malloc(sizeof(struct ccnl_stack_s *));
        config->fox_state->params[0] = streamStack;
        config->fox_state->it_routable_param = 0;
    }
    tupleprefix = config->fox_state->params[0]->content;
    quantity = i2;
    unit = i1;
    //DEBUGMSG(DEBUG, "WINDOW: Checking if result was received\n");
    //DEBUGMSG(DEBUG, "WINDOW: Found parameters %s, %i, %i",ccnl_prefix_to_path(tupleprefix),quantity,unit);
    tuple = ccnl_nfn_local_content_search(ccnl, config, tupleprefix);
    int tupleContLen = 0;
    (void) local_search;
    /*if (!tuple) {
        if(local_search){
            DEBUGMSG(INFO, "WINDOW: no content\n");
            return NULL;
        }
    }*/
    if(tuple){
        tupleContLen = tuple->pkt->contlen;
    }
    char tupleContent[tupleContLen];
    if(tuple){
        memcpy(tupleContent,tuple->pkt->content,tuple->pkt->contlen);
    }
    else{
        DEBUGMSG(DEBUG, "WINDOW: Tuple was not present at the given time, wait for the next one\n");
    }

    //DEBUGMSG(DEBUG, "WINDOW: Found Tuple Content %s\n",tupleContent);
    int prefixLength = strlen(ccnl_prefix_to_path(tupleprefix))+strlen("state")+ get_int_len(quantity)+get_int_len(unit)+2;
    char statePrefix[prefixLength];
    sprintf(statePrefix,"state%s/%i/%i",ccnl_prefix_to_path(tupleprefix),quantity,unit);
    //DEBUGMSG(DEBUG, "Derived State Name is %.*s, calculated Length was %i, actual Length was %lu. This is because of lengths: tuplePrefix: %lu, state: %lu, quantitiy:%lu, unit:%lu\n",(int)strlen(statePrefix),statePrefix,prefixLength,strlen(statePrefix),strlen(ccnl_prefix_to_path(tupleprefix)),strlen("state"),sizeof(quantity),sizeof(unit));
    //DEBUGMSG(DEBUG, "Derived State Name is %.*s\n",(int)strlen(statePrefix),statePrefix);
    stateprefix =  ccnl_URItoPrefix(statePrefix, config->suite, NULL, NULL);
    state = ccnl_nfn_local_content_search(ccnl, config, stateprefix);
    int stateContLen = 0;
    if(state){
        stateContLen = state->pkt->contlen;
    }
    char stateContent[stateContLen];
    if(state){
        memcpy(stateContent,state->pkt->content,state->pkt->contlen);
        DEBUGMSG(DEBUG, "WINDOW: Found State Content %s\n",stateContent);
    }
    else{
        DEBUGMSG(DEBUG, "WINDOW: Did not find state content \n");
    }
    char endResult[tupleContLen+stateContLen];
    window_purge_old_data(endResult, stateContent, tupleContent, stateContLen, quantity, unit);
    /*char s[CCNL_MAX_PREFIX_SIZE];
    if(state){
        DEBUGMSG(DEBUG, "The name of the Packet is %s\n",ccnl_prefix_to_str(state->pkt->pfx, s, CCNL_MAX_PREFIX_SIZE));
        DEBUGMSG(DEBUG, "The content of the Packet Buffer is: %.*s\n",(int)state->pkt->buf->datalen+state->pkt->contlen,state->pkt->buf->data);
    }

    char d[CCNL_MAX_PREFIX_SIZE];
    DEBUGMSG(DEBUG, "The name of the config Prefix is %s\n",ccnl_prefix_to_str(config->prefix,d,CCNL_MAX_PREFIX_SIZE));
     */

    /*if(nfnInterest)
        DEBUGMSG(DEBUG, "Managed to find the desired nfn Interest. Nice\n");
    else
        DEBUGMSG(DEBUG, "Grind on...\n");

    DEBUGMSG(DEBUG, "WINDOW: EndResult is %s\n",endResult);
     */
    if(!state){
        struct ccnl_interest_s* nfnInterest = ccnl_nfn_local_interest_search(ccnl,config,config->prefix);
        ccnl_makeQueryPersistent(nfnInterest, ccnl, ccnl_prefix_dup(tupleprefix));
    }
    /*else
        DEBUGMSG(DEBUG, "Datalen of the Buffer was %lu, contentlen of the pkt was %i\n",state->pkt->buf->datalen,state->pkt->contlen);*/
    //DEBUGMSG(DEBUG, "The new size of the char to safe is %lu\n",sizeof(char)*strlen(endResult)+1);
    int len = sizeof(char)*strlen(endResult);
    struct ccnl_pkt_s* pkt = NULL;
    struct ccnl_content_s* oldState = NULL;
    if(tuple){
    if(state){
        pkt = createNewPacket(ccnl_prefix_dup(state->pkt->pfx),endResult,len);
        oldState = state;
        state = ccnl_content_new(&(pkt));
        if(!oldState->prev){
            ccnl->contents = state;
        }
        else{
            oldState->prev->next = state;
        }
        state->next = oldState->next;
        ccnl_content_free(oldState);
    }
    else{
        pkt = createNewPacket(ccnl_prefix_dup(stateprefix),endResult,len);
        state = ccnl_content_new(&(pkt));
        if(ccnl->contents){
            ccnl->contents->prev = state;
            state->next = ccnl->contents;
            ccnl->contents = state;
        }
        else{
            ccnl->contents = state;
        }
    }
    }
    else{
        DEBUGMSG(INFO, "WINDOW: no content\n");
        return NULL;
    }
    /*DEBUGMSG(DEBUG, "The new Data of the packet is %.*s\n",state->pkt->contlen,state->pkt->content);
    char a[CCNL_MAX_PREFIX_SIZE];

    DEBUGMSG(DEBUG, "The name of the new Packet is still %s\n",ccnl_prefix_to_str(state->pkt->pfx, a, CCNL_MAX_PREFIX_SIZE));
    DEBUGMSG(DEBUG, "The content of the new Packet Buffer is: %.*s\n",(int)state->pkt->buf->datalen+state->pkt->contlen,state->pkt->buf->data);
    DEBUGMSG(DEBUG, "Datalen of the Buffer was %lu, contentlen of the new Packet was %i\n",state->pkt->buf->datalen,state->pkt->contlen);
     */
    DEBUGMSG(INFO, "WINDOW: result was found ---> handle it (%s), prog=%s, pending=%s\n", ccnl_prefix_to_path(stateprefix), prog, pending);
    push_to_stack(&config->result_stack, ccnl_prefix_dup(stateprefix), STACK_TYPE_PREFIX);
    ccnl_free(stateprefix);
    if (pending) {
        DEBUGMSG(DEBUG, "Pending: %s\n", pending);

        cp = ccnl_strdup(pending);
    }
#ifndef CCNL_LINUXKERNEL
    clock_gettime(CLOCK_MONOTONIC,&tend);
#else
    getrawmonotonic(&tend);
#endif

    uint64_t timeDifference = tend.tv_nsec - tstart.tv_nsec;//((uint64_t)tend.tv_sec + 1.0e-9*tend.tv_nsec) - ((uint64_t)tstart.tv_sec + 1.0e-9*tstart.tv_nsec);

#ifndef CCNL_LINUXKERNEL
    DEBUGMSG(DEBUG,"Builtin Add took about %lu seconds\n",timeDifference);

#else
    DEBUGMSG(DEBUG,"Builtin Kernel Add took about %llu seconds\n",timeDifference);
#endif
    return cp;
}

char*
op_builtin_find(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                int *restart, int *halt, char *prog, char *pending,
                struct stack_s **stack)
{
    int local_search = 0;
    struct stack_s *h;
    char *cp = NULL;
    struct ccnl_prefix_s *prefix;
    struct ccnl_content_s *c = NULL;
    (void)stack;

    if (*restart) {
        DEBUGMSG(DEBUG, "---to do: OP_FIND restart\n");
        *restart = 0;
        local_search = 1;
    } else {
        DEBUGMSG(DEBUG, "---to do: OP_FIND <%s> <%s>\n", prog+7, pending);
        h = pop_from_stack(&config->result_stack);
        //    if (h->type != STACK_TYPE_PREFIX)  ...
        config->fox_state->num_of_params = 1;
        config->fox_state->params = ccnl_malloc(sizeof(struct ccnl_stack_s *));
        config->fox_state->params[0] = h;
        config->fox_state->it_routable_param = 0;
    }
    prefix = config->fox_state->params[0]->content;

    //check if result is now available
    //loop by reentering (with local_search) after timeout of the interest...
    DEBUGMSG(DEBUG, "FIND: Checking if result was received\n");
    c = ccnl_nfn_local_content_search(ccnl, config, prefix);
    if (!c) {
        struct ccnl_prefix_s *copy;
        struct ccnl_interest_s *interest;
        if (local_search) {
            DEBUGMSG(INFO, "FIND: no content\n");
            return NULL;
        }
        //Result not in cache, search over the network
        //        struct ccnl_interest_s *interest = mkInterestObject(ccnl, config, prefix);
        copy = ccnl_prefix_dup(prefix);
        interest = ccnl_nfn_query2interest(ccnl, &copy, config);
        DEBUGMSG(DEBUG, "FIND: sending new interest from Face ID: %d\n",
                 interest->from->faceid);
        if (interest)
            ccnl_interest_propagate(ccnl, interest);
        //wait for content, return current program to continue later
        *halt = -1; //set halt to -1 for async computations
        return ccnl_strdup(prog);
    }

    DEBUGMSG(INFO, "FIND: result was found ---> handle it (%s), prog=%s, pending=%s\n", ccnl_prefix_to_path(prefix), prog, pending);
#ifdef USE_NACK
/*
    if (!strncmp((char*)c->content, ":NACK", 5)) {
        DEBUGMSG(DEBUG, "NACK RECEIVED, going to next parameter\n");
        ++config->fox_state->it_routable_param;

        return prog ? ccnl_strdup(prog) : NULL;
    }
*/
#endif
    prefix = ccnl_prefix_dup(prefix);
    push_to_stack(&config->result_stack, prefix, STACK_TYPE_PREFIX);

    if (pending) {
        DEBUGMSG(DEBUG, "Pending: %s\n", pending);

        cp = ccnl_strdup(pending);
    }
    return cp;
}

char*
op_builtin_mult(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                int *restart, int *halt, char *prog, char *pending,
                struct stack_s **stack)
{
    int i1=0, i2=0, *h;
    (void)restart;

    DEBUGMSG(DEBUG, "---to do: OP_MULT <%s>\n", prog+8);
    pop2int();
    h = ccnl_malloc(sizeof(int));
    *h = i2 * i1;
    push_to_stack(stack, h, STACK_TYPE_INT);

    return pending ? ccnl_strdup(pending) : NULL;
}

char*
op_builtin_raw(struct ccnl_relay_s *ccnl, struct configuration_s *config,
               int *restart, int *halt, char *prog, char *pending,
               struct stack_s **stack)
{
    (void)stack;
    int local_search = 0;
    struct stack_s *h;
    char *cp = NULL;
    struct ccnl_prefix_s *prefix;
    struct ccnl_content_s *c = NULL;

    //    print_argument_stack(config->argument_stack);
    //    print_result_stack(config->result_stack);
    if (*restart) {
        DEBUGMSG(DEBUG, "---to do: OP_RAW restart\n");
        *restart = 0;
        local_search = 1;
    } else {
        DEBUGMSG(DEBUG, "---to do: OP_RAW <%s>\n", prog+7);
        h = pop_from_stack(&config->result_stack);
        if (!h || h->type != STACK_TYPE_PREFIX) {
            DEBUGMSG(DEBUG, "  stack empty or has no prefix %p\n", (void*) h);
        } else {
            DEBUGMSG(DEBUG, "  found a prefix!\n");
        }
        config->fox_state->num_of_params = 1;
        config->fox_state->params = ccnl_malloc(sizeof(struct ccnl_stack_s *));
        config->fox_state->params[0] = h;
        config->fox_state->it_routable_param = 0;
    }
    prefix = config->fox_state->params[0]->content;

    //check if result is now available
    //loop by reentering (with local_search) after timeout of the interest...
    DEBUGMSG(DEBUG, "RAW: Checking if result was received\n");
    c = ccnl_nfn_local_content_search(ccnl, config, prefix);
    if (!c) {
        if (local_search) {
            DEBUGMSG(DEBUG, "RAW: no content\n");
            return NULL;
        }
        //Result not in cache, search over the network
        //        struct ccnl_interest_s *interest = mkInterestObject(ccnl, config, prefix);
        struct ccnl_prefix_s *copy = ccnl_prefix_dup(prefix);
        struct ccnl_interest_s *interest = ccnl_nfn_query2interest(ccnl, &copy, config);
        DEBUGMSG(DEBUG, "RAW: sending new interest from Face ID: %d\n", interest->from->faceid);
        if (interest)
            ccnl_interest_propagate(ccnl, interest);
        //wait for content, return current program to continue later
        *halt = -1; //set halt to -1 for async computations
        return prog ? ccnl_strdup(prog) : NULL;
    }

    DEBUGMSG(DEBUG, "RAW: result was found ---> handle it (%s), prog=%s, pending=%s\n", ccnl_prefix_to_path(prefix), prog, pending);
#ifdef USE_NACK
/*
    if (!strncmp((char*)c->content, ":NACK", 5)) {
        DEBUGMSG(DEBUG, "NACK RECEIVED, going to next parameter\n");
        ++config->fox_state->it_routable_param;

        return prog ? ccnl_strdup(prog) : NULL;
    }
*/
#endif
    prefix = ccnl_prefix_dup(prefix);
    push_to_stack(&config->result_stack, prefix, STACK_TYPE_PREFIXRAW);

    if (pending) {
        DEBUGMSG(DEBUG, "Pending: %s\n", pending+1);

        cp = ccnl_strdup(pending+1);
    }
    return cp;
}

char*
op_builtin_sub(struct ccnl_relay_s *ccnl, struct configuration_s *config,
               int *restart, int *halt, char *prog, char *pending,
               struct stack_s **stack)
{
    (void)restart;
    int i1=0, i2=0, *h;

    DEBUGMSG(DEBUG, "---to do: OP_SUB <%s>\n", prog+7);
    pop2int();
    h = ccnl_malloc(sizeof(int));
    *h = i2 - i1;
    push_to_stack(stack, h, STACK_TYPE_INT);

    return pending ? ccnl_strdup(pending) : NULL;
}

// ----------------------------------------------------------------------

char*
op_builtin_cmpeqc(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                  int *restart, int *halt, char *prog, char *pending,
                  struct stack_s **stack)
{
    int i1=0, i2=0;
    char res[1000], *cp;
    (void)restart;
    (void)stack;
    pop2int();
    cp = (i1 == i2) ? "@x@y x" : "@x@y y";
    DEBUGMSG(DEBUG, "---to do: OP_CMPEQ <%s>/<%s>\n", cp, pending);
    if (pending)
        sprintf(res, "RESOLVENAME(%s);%s", cp, pending);
    else
        sprintf(res, "RESOLVENAME(%s)", cp);
    return ccnl_strdup(res);
}

char*
op_builtin_cmpleqc(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                  int *restart, int *halt, char *prog, char *pending,
                  struct stack_s **stack)
{
    int i1=0, i2=0;
    char res[1000], *cp;
    (void)restart;
    (void)stack;
    pop2int();
    cp = (i2 <= i1) ? "@x@y x" : "@x@y y";
    DEBUGMSG(DEBUG, "---to do: OP_CMPLEQ <%s>/%s\n", cp, pending);
    if (pending)
        sprintf(res, "RESOLVENAME(%s);%s", cp, pending);
    else
        sprintf(res, "RESOLVENAME(%s)", cp);
    return ccnl_strdup(res);
}

char*
op_builtin_cmpeq(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                 int *restart, int *halt, char *prog, char *pending,
                 struct stack_s **stack)
{
    int i1=0, i2=0;
    char res[1000], *cp;
    (void)restart;
    (void)stack;

    DEBUGMSG(DEBUG, "---to do: OP_CMPEQ<%s>\n", pending);
    pop2int();
    cp = (i1 == i2) ? "1" : "0";
    if (pending)
        sprintf(res, "RESOLVENAME(%s);%s", cp, pending);
    else
        sprintf(res, "RESOLVENAME(%s)", cp);
    return ccnl_strdup(res);
}

char*
op_builtin_cmpleq(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                  int *restart, int *halt, char *prog, char *pending,
                  struct stack_s **stack)
{
    int i1=0, i2=0;
    char res[1000], *cp;
    (void)restart;
    (void)stack;

    DEBUGMSG(DEBUG, "---to do: OP_CMPLEQ <%s>\n", pending);
    pop2int();
    cp = (i2 <= i1) ? "1" : "0";
    if (pending)
        sprintf(res, "RESOLVENAME(%s);%s", cp, pending);
    else
        sprintf(res, "RESOLVENAME(%s)", cp);
    return ccnl_strdup(res);
}

char*
op_builtin_ifelse(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                  int *restart, int *halt, char *prog, char *pending,
                  struct stack_s **stack)
{
    struct stack_s *h;
    int i1=0;
    (void)restart;
    (void)stack;

    DEBUGMSG(DEBUG, "---to do: OP_IFELSE <%s>\n", prog+10);
    h = pop_or_resolve_from_result_stack(ccnl, config);
    if (!h) {
        *halt = -1;
        return ccnl_strdup(prog);
    }
    if (h->type != STACK_TYPE_INT) {
        DEBUGMSG(WARNING, "ifelse requires int as condition");
        ccnl_nfn_freeStack(h);
        return NULL;
    }
    i1 = *(int *)h->content;
    if (i1) {
        struct stack_s *stack = pop_from_stack(&config->argument_stack);
        DEBUGMSG(DEBUG, "Execute if\n");
        pop_from_stack(&config->argument_stack);
        push_to_stack(&config->argument_stack, stack->content,
                      STACK_TYPE_CLOSURE);
    } else {
        DEBUGMSG(DEBUG, "Execute else\n");
        pop_from_stack(&config->argument_stack);
    }
    return ccnl_strdup(pending);
}

// ----------------------------------------------------------------------

#ifdef USE_NFN_NSTRANS

// NFN namespace translation example: "translate NS /some/uri/to/fetch"
// where NS is a constant: 'ccnb, 'ccnx2014, 'ndn2013

char*
op_builtin_nstrans(struct ccnl_relay_s *ccnl, struct configuration_s *config,
                   int *restart, int *halt, char *prog, char *pending,
                   struct stack_s **stack)
{
    char *cp = NULL;
    struct stack_s *s1, *s2;

    DEBUGMSG(DEBUG, "---to do: OP_NSTRANS\n");

    s1 = pop_or_resolve_from_result_stack(ccnl, config);
    if (!s1) {
        *halt = -1;
        return prog;
    }
    s2 = pop_or_resolve_from_result_stack(ccnl, config);
    if (!s2) {
        ccnl_nfn_freeStack(s1);
        *halt = -1;
        return prog;
    }

    if (s2->type == STACK_TYPE_CONST && s1->type == STACK_TYPE_PREFIX) {
        struct ccnl_prefix_s *p = (struct ccnl_prefix_s*) s1->content;
        struct const_s *con = (struct const_s *) s2->content;
        int suite = ccnl_str2suite(con->str);
        DEBUGMSG(DEBUG, "  original packet format: %s\n", con->str);

        /*
        if (!strcmp(con->str, "ccnb"))
            suite = CCNL_SUITE_CCNB;
        else if (!strcmp(con->str, "ccnx2014"))
            suite = CCNL_SUITE_CCNTLV;
        else if (!strcmp(con->str, "ndn2013"))
            suite = CCNL_SUITE_NDNTLV;
        */
        if (suite < 0)
            goto out;
        DEBUGMSG(DEBUG, " >> changing PREFIX suite from %d to %d\n",
                 p->suite, suite);

        p->nfnflags = 0;
        p->suite = suite;
        push_to_stack(stack, s1->content, STACK_TYPE_PREFIX);

        ccnl_free(s1);
        s1 = NULL;

        if (pending) {
            cp = ccnl_malloc(strlen(pending)+1);
            strcpy(cp, pending);
        }
    } else {
out:
        *halt = -1;
        cp = prog;
    }
    if (s1)
        ccnl_nfn_freeStack(s1);
    ccnl_nfn_freeStack(s2);

    return cp;
}

#endif // USE_NFN_NSTRANS

// ----------------------------------------------------------------------

struct builtin_s bifs[] = {
    {"OP_ADD",           op_builtin_add,  NULL},
    {"OP_FIND",          op_builtin_find, NULL},
    {"OP_MULT",          op_builtin_mult, NULL},
    {"OP_RAW",           op_builtin_raw,  NULL},
    {"OP_SUB",           op_builtin_sub,  NULL},

    {"OP_CMPEQ_CHURCH",  op_builtin_cmpeqc, NULL},
    {"OP_CMPLEQ_CHURCH", op_builtin_cmpleqc, NULL},
    {"OP_CMPEQ",         op_builtin_cmpeq, NULL},
    {"OP_CMPLEQ",        op_builtin_cmpleq, NULL},
    {"OP_IFELSE",        op_builtin_ifelse, NULL},

    /**
     * Added By Johannes
     */
    {"OP_WINDOW", op_builtin_window,NULL},

#ifdef USE_NFN_NSTRANS
    {"OP_NSTRANS",       op_builtin_nstrans, NULL},
#endif

    {NULL, NULL, NULL}
};

#endif // USE_NFN
// eof
