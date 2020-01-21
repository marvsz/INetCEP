//
// Created by johannes on 21.01.20.
//

#ifndef CCNL_LINUXKERNEL
#include "../include/ccnl-content-store.h"
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

struct ccnl_tree_node_s* ccnl_tree_node_new(char* nodePrefix, int nodePrefixLength, int depth, ccnl_tree_node* prev, ccnl_tree_node* next){
    struct ccnl_tree_node_s *tn = (struct ccnl_tree_node_s *) ccnl_calloc(1,sizeof(struct ccnl_tree_node_s));
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

struct ccnl_tree_node_s* ccnl_tree_node_find(struct ccnl_tree_node_s* node, struct ccnl_prefix_s *pfx){
    struct ccnl_tree_node_s *currentNode = node;
    unsigned char *comp = currentNode->depth < pfx->compcnt ? pfx->comp[currentNode->depth] : 0;
    int clen = currentNode->depth < pfx->compcnt ? pfx->complen[currentNode->depth] : 32;
    while(currentNode){
        if(clen == currentNode->nodePrefixLength && memcmp(comp,node->nodePrefix,node->nodePrefixLength)){
            if(pfx->compcnt == currentNode->depth){
                return currentNode;
            }
            if(pfx->compcnt > currentNode->depth){
                ccnl_tree_node_find(currentNode->children,pfx);
            }
        }
        currentNode = currentNode->next;
    }
    return currentNode;
}

int
ccnl_tree_node_free(struct ccnl_tree_node_s *node){
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