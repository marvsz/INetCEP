/**
 * @addtogroup CCNL-core
 * @{
 *
 * @file ccnl-content-store.h
 * @brief CCN lite, core CCNx content store definition and helper functions
 *
 * Copyright (C) 2011-18 University of Basel
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
 */

#ifndef CCNL_CONTENT_STORE_H
#define CCNL_CONTENT_STORE_H

#include <stdbool.h>
#include <ccnl-content.h>
#ifndef CCNL_LINUXKERNEL
#include <stdint.h>
#else
#include <linux/types.h>
#endif

struct ccnl_pkt_s;
struct ccnl_prefix_s;



typedef struct ccnl_tree_node_s {
    char* nodePrefix;
    int nodePrefixLength;
    int depth;
    struct ccnl_tree_node_s *next;
    struct ccnl_tree_node_s *prev;
    struct ccnl_tree_node_s *children;
    struct ccnl_pkt_s *pkt;
    ccnl_content_flags flags;
    uint32_t last_used;
    int served_cnt;
} ccnl_tree_node;

struct ccnl_tree_node_s* ccnl_tree_node_new(char* nodePrefix, int nodePrefixLength, int depth, ccnl_tree_node* prev, ccnl_tree_node* next);

int ccnl_tree_node_free(struct ccnl_tree_node_s *node);

struct ccnl_tree_node_s* ccnl_tree_node_find(struct ccnl_tree_node_s* node, struct ccnl_prefix_s *pfx);

int ccnl_insert_content();

int ccnl_remove_content();

#endif //CCNL_CONTENT_STORE_H
/** @} */
