//
// Created by johannes on 21.01.20.
//

#include <stddef.h>
#include <stdio.h>
#include <ccnl-contentStore.h>

struct ccnl_content_store_node_s* ccnl_dbl_node_new(char* nodePrefix, int nodePrefixLength, int depth, struct ccnl_content_store_node_s* prev, struct ccnl_content_store_node_s* next){

}

int ccnl_dbl_node_free(struct ccnl_content_store_node_s *node){

}

struct ccnl_content_store_node_s* ccnl_dbl_node_get(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx, int mode){

}

int ccnl_dbl_insert_content(struct ccnl_content_store_node_s **node, struct ccnl_pkt_s *pkt){
    struct ccnl_content_store_node_s *e = ccnl_dbl_node_new("",0,0,NULL,NULL);
    if(e == NULL){
        return 0;
    }
    else{
        if((*node) == NULL){
            e->pkt=pkt;
            (*node) = e;
        }
        else{
            (*node)->prev = e;
            e->next=(*node);
            (*node) = e;
        }
        return 1;
    }
}

int ccnl_dbl_contains_content(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx, int mode){

}

int ccnl_dbl_remove_content(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx){
    struct ccnl_content_store_node_s *currentNode = node;
    while(currentNode != NULL){
        if(ccnl_prefix)
    }
}