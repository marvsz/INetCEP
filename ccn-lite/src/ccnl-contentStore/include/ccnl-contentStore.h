//
// Created by johannes on 21.01.20.
//

#ifndef CCNL_CONTENTSTORE_H
#define CCNL_CONTENTSTORE_H

#include <stdbool.h>
#include <ccnl-content.h>
#include <stdint.h>

struct ccnl_pkt_s;
struct ccnl_prefix_s;

typedef enum ccnl_content_store_flags_e {
    CCNL_CONTENT_STORE_FLAGS_DOUBLY_LINKED_LIST = 0x00,
    CCNL_CONTENT_STORE_FLAGS_PREFIX_TREE = 0x01
} ccnl_content_store_flags;

typedef struct ccnl_content_store_setting_s {
    int contentcnt;
    int max_cache_entries;
    int content_store_type;
} ccnl_content_store_setting;

typedef struct ccnl_content_store_node_s {
    char* nodePrefix;
    int nodePrefixLength;
    int depth;
    struct ccnl_content_store_node_s *next;
    struct ccnl_content_store_node_s *prev;
    struct ccnl_content_store_node_s *children;
    struct ccnl_pkt_s *pkt;
    ccnl_content_flags flags;
    uint32_t last_used;
    int served_cnt;
} ccnl_content_store_node;

typedef struct ccnl_content_store_s {
    struct ccnl_content_store_setting_s* setting;
    struct ccnl_content_store_node_s* root;
    ccnl_content_store_flags flags;
} content_store;



int ccnl_content_store_add(struct ccnl_pkt_s *pkt, struct ccnl_content_store_s *contentStore);

int ccnl_content_store_remove(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore);

int ccnl_content_store_contains(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore, int mode);

struct ccnl_pkt_s* ccnl_content_store_get(struct ccnl_prefix_s *pfx, struct ccnl_content_store_s *contentStore, int mode);

struct ccnl_content_store_s* ccnl_content_store_initialize(struct ccnl_content_store_setting_s *setting);

#endif //CCNL_CONTENTSTORE_H
