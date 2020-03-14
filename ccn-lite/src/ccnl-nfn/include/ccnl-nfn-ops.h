//
// Created by Balázs Faludi on 27.06.17.
//

#ifndef CCNL_NFN_OPS_H
#define CCNL_NFN_OPS_H

#include "ccnl-nfn.h"

#ifndef CCN_LITE_MKC_OUT_SIZE
#define CCN_LITE_MKC_OUT_SIZE (65 * 1024)
#endif

#ifndef CCN_LITE_MKC_BODY_SIZE
#define CCN_LITE_MKC_BODY_SIZE (64 * 1024)
#endif

struct builtin_s *op_extensions;
extern struct builtin_s bifs[];
int get_int_len (int value);
#endif //CCNL_NFN_OPS_H
