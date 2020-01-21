//
// Created by johannes on 21.01.20.
//

#ifndef CCNL_LINUXKERNEL
#include "../include/ccnl-contentStore.h"
#include "../include/ccnl-prefixTree.h"
#include "../include/ccnl-malloc.h"
#include "../include/ccnl-prefix.h"
#include "../include/ccnl-pkt.h"
#include "../include/ccnl-os-time.h"
#include "../include/ccnl-logging.h"
#include "../include/ccnl-defs.h"
#else
#include "../include/ccnl-content-store.h"
#include "../include/ccnl-malloc.h"
#include "../include/ccnl-prefix.h"
#include "../include/ccnl-pkt.h"
#include "../include/ccnl-os-time.h"
#include "../include/ccnl-logging.h"
#include "../include/ccnl-defs.h"
#endif

struct ccnl_content_store_node_s* ccnl_pfxt_node_new(char* nodePrefix, int nodePrefixLength, int depth, struct ccnl_content_store_node_s* prev, struct ccnl_content_store_node_s* next){
    struct ccnl_content_store_node_s *tn = (struct ccnl_content_store_node_s *) ccnl_calloc(1,sizeof(struct ccnl_content_store_node_s));
    if(!tn){
        return NULL;
    }
    tn->nodePrefix = ccnl_calloc(1,nodePrefixLength+1);
    strcpy(tn->nodePrefix, nodePrefix);
    tn->nodePrefixLength = nodePrefixLength;
    tn->depth = depth;
    tn->prev = prev;
    tn->next = next;
    return tn;
}

int
ccnl_pfxt_node_free(struct ccnl_content_store_node_s *node){
    if(node){
        if(node->pkt){
            ccnl_pkt_free(node->pkt);
        }
        ccnl_free(node->nodePrefix);
        ccnl_free(node);

        return 0;
    }
    return -1;
}

struct ccnl_content_store_node_s* ccnl_pfxt_node_get(struct ccnl_content_store_node_s* node, struct ccnl_prefix_s *pfx, int mode){
    struct ccnl_content_store_node_s *currentNode = node;
    unsigned char *comp = currentNode->depth < pfx->compcnt ? pfx->comp[currentNode->depth] : 0;
    int clen = currentNode->depth < pfx->compcnt ? pfx->complen[currentNode->depth] : 32;
    if(mode == 0){ // longest matching prefix
        while(currentNode){
            if(clen == currentNode->nodePrefixLength && memcmp(comp,node->nodePrefix,node->nodePrefixLength)){
                if(pfx->compcnt == currentNode->depth){
                    return currentNode;
                }
                if(pfx->compcnt > currentNode->depth){
                    ccnl_pfxt_node_get(currentNode->children,pfx,mode);
                }
            }
            currentNode = currentNode->next;
        }
        return currentNode;
    }
    else {
        while(currentNode){ // exact prefix
            if(clen == currentNode->nodePrefixLength && memcmp(comp,node->nodePrefix,node->nodePrefixLength)){
                if(pfx->compcnt == currentNode->depth){
                    return currentNode;
                }
                if(pfx->compcnt > currentNode->depth){
                    ccnl_pfxt_node_get(currentNode->children,pfx,mode);
                }
            }
            currentNode = currentNode->next;
        }
        return NULL;
    }

}

int ccnl_pfxt_insert_content(struct ccnl_content_store_node_s *node, struct ccnl_pkt_s *pkt){

}

int ccnl_pfxt_contains_content(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx, int mode){
    struct ccnl_content_store_node_s *currentNode = node;
    unsigned char *comp = currentNode->depth < pfx->compcnt ? pfx->comp[currentNode->depth] : 0;
    int clen = currentNode->depth < pfx->compcnt ? pfx->complen[currentNode->depth] : 32;
    while(currentNode){
        if(clen == currentNode->nodePrefixLength && memcmp(comp,node->nodePrefix,node->nodePrefixLength)){
            if(pfx->compcnt == currentNode->depth){
                return 1;
            }
            if(pfx->compcnt > currentNode->depth){
                ccnl_pfxt_node_get(currentNode->children,pfx,mode);
            }
        }
        currentNode = currentNode->next;
    }
    return 0;
}

int ccnl_pfxt_remove_content(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx){

}
