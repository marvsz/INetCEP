//
// Created by johannes on 21.01.20.
//

#include "../include/ccnl-contentStore.h"
#include "../include/ccnl-doublyLinkedList.h"
#include "../include/ccnl-prefixTree.h"

int ccnl_content_store_add(struct ccnl_pkt_s *pkt, struct ccnl_content_store_s *contentStore){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){

    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){

    }
}

int ccnl_content_store_remove(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){

    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){

    }
}

int ccnl_content_store_contains(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore, int mode){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){

    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){

    }
}

struct ccnl_pkt_s* ccnl_content_store_get(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore, int mode){
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST){

    }
    if(contentStore->setting->content_store_type == CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE){

    }
}

struct ccnl_content_store_s* ccnl_content_store_initialize(struct ccnl_content_store_setting_s *setting){

}
