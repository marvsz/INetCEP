//
// Created by johannes on 21.01.20.
//

#include <ccnl-malloc.h>
#include "../include/ccnl-contentStore.h"
#include "../include/ccnl-doublyLinkedList.h"
#include "../include/ccnl-prefixTree.h"

int ccnl_content_store_add(struct ccnl_pkt_s *pkt, struct ccnl_content_store_s *contentStore){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){
        return ccnl_dbl_insert_content(contentStore->root, pkt);
    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){
        return ccnl_pfxt_insert_content(contentStore->root, pkt);
    }
    return -1;
}

int ccnl_content_store_remove(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){
        return ccnl_dbl_remove_content(contentStore->root, pfx);
    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){
        return ccnl_pfxt_remove_content(contentStore->root, pfx);
    }
    return -1;
}

int ccnl_content_store_contains(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore, int mode){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){
        return ccnl_dbl_contains_content(contentStore->root, pfx, mode);
    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){
        return ccnl_pfxt_contains_content(contentStore->root, pfx, mode);
    }
    return -1;
}

struct ccnl_pkt_s* ccnl_content_store_get(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore, int mode){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){
        return ccnl_dbl_node_get(contentStore->root, pfx, mode)->pkt;
    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){
        return ccnl_pfxt_node_get(contentStore->root, pfx, mode)->pkt;
    }
    return NULL;
}

struct ccnl_content_store_s* ccnl_content_store_initialize(struct ccnl_content_store_setting_s *setting){
    struct ccnl_content_store_s *cs = (struct ccnl_content_store_s *) ccnl_calloc(1, sizeof(struct ccnl_content_store_s));
    cs->setting = setting;
    return cs;
}
