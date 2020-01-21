//
// Created by johannes on 21.01.20.
//

#ifndef CCNL_PREFIXTREE_H
#define CCNL_PREFIXTREE_H

#include <stdbool.h>
#include <ccnl-content.h>
#include <stdint.h>

struct ccnl_pkt_s;
struct ccnl_prefix_s;

struct ccnl_content_store_node_s* ccnl_pfxt_node_new(char* nodePrefix, int nodePrefixLength, int depth, struct ccnl_content_store_node_s* prev, struct ccnl_content_store_node_s* next);

int ccnl_pfxt_node_free(struct ccnl_content_store_node_s *node);

struct ccnl_content_store_node_s* ccnl_pfxt_node_get(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx, int mode);

int ccnl_pfxt_insert_content(struct ccnl_content_store_node_s *node, struct ccnl_pkt_s *pkt);

int ccnl_pfxt_contains_content(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx, int mode);

int ccnl_pfxt_remove_content(struct ccnl_content_store_node_s *node, struct ccnl_prefix_s *pfx);

#endif //CCNL_PREFIXTREE_H
