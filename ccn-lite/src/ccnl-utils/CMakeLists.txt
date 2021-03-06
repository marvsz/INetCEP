cmake_minimum_required(VERSION 2.8)
project(ccn-lite-utils)

set(PROJECT_LINK_LIBS libccnl-core.a libccnl-pkt.a libccnl-fwd.a libccnl-unix.a libccnl-nfn.a)
set(EXT_LINK_LIBS ssl crypto)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/../bin)

link_directories(
    ${CMAKE_BINARY_DIR}/../lib
)

# set include directories
include_directories(include ../ccnl-pkt/include ../ccnl-fwd/include ../ccnl-core/include ../ccnl-unix/include ../ccnl-nfn/include ../ccnl-sensor/include ../ccnl-dump/include)

add_library(common STATIC src/ccnl-common.c src/base64.c src/ccnl-socket.c)
add_library(ccnl-crypto STATIC src/ccnl-crypto.c src/ccnl-ext-hmac.c src/lib-sha256.c)

add_executable(ccn-lite-peek src/ccn-lite-peek.c)
add_executable(ccn-lite-sendI src/ccn-lite-sendI.c)
add_executable(ccn-lite-peekConstant src/ccn-lite-peekConstant.c)
add_executable(ccn-lite-rmI src/ccn-lite-rmI.c)
#add_executable(ccn-lite-peekcomputation ccn-lite-peekcomputation.c) #todo work to do
add_executable(ccn-lite-ctrl src/ccn-lite-ctrl.c)
add_executable(ccn-lite-fetch src/ccn-lite-fetch.c) 
if(OpenSSL_FOUND)
    add_executable(ccn-lite-ccnb2xml src/ccn-lite-ccnb2xml.c)
    add_executable(ccn-lite-cryptoserver src/ccn-lite-cryptoserver.c)
    #add_executable(ccn-lite-deF ccn-lite-deF.c)
    add_executable(ccn-lite-mkC src/ccn-lite-mkC.c)
    add_executable(ccn-lite-mkDSC src/ccn-lite-mkDSC.c)
    add_executable(ccn-lite-valid src/ccn-lite-valid.c)
    add_executable(ccn-lite-rpc src/ccn-lite-rpc.c)
endif()

if(USE_FRAG)
    add_executable(ccn-lite-mkF src/ccn-lite-mkF.c)
endif()
add_executable(ccn-lite-mkI src/ccn-lite-mkI.c)
add_executable(ccn-lite-pktdump src/ccn-lite-pktdump.c)
add_executable(ccn-lite-produce src/ccn-lite-produce.c)

add_executable(ccn-lite-simplenfn src/ccn-lite-simplenfn.c)
add_executable(ccn-lite-simplenfnConstant src/ccn-lite-simplenfnConstant.c)
add_executable(ccn-lite-mkS src/ccn-lite-mkS.c)
add_executable(ccn-lite-rmS src/ccn-lite-rmS.c)

target_link_libraries(ccn-lite-peek ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-peek ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common)

target_link_libraries(ccn-lite-sendI ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-sendI ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common ccnl-dump)
target_link_libraries(ccn-lite-sendI pthread)

target_link_libraries(ccn-lite-peekConstant ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-peekConstant ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common ccnl-dump)

target_link_libraries(ccn-lite-rmI ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-rmI ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common ccnl-dump)

#target_link_libraries(ccn-lite-peekcomputation ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})#todo work to do
#target_link_libraries(ccn-lite-peekcomputation ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn)

target_link_libraries(ccn-lite-ctrl ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-ctrl ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn crypto ccnl-sensor)

if(OpenSSL_FOUND)
    target_link_libraries(ccn-lite-ccnb2xml ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES})
    target_link_libraries(ccn-lite-ccnb2xml ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ccnl-crypto ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-cryptoserver ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES})
    target_link_libraries(ccn-lite-cryptoserver ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ccnl-crypto ${OPENSSL_LIBRARIES} common)

    #target_link_libraries(ccn-lite-deF ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES})
    #target_link_libraries(ccn-lite-deF ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-mkC ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES} common)
    target_link_libraries(ccn-lite-mkC ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ccnl-crypto ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-mkDSC ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES} common)
    target_link_libraries(ccn-lite-mkDSC ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ccnl-crypto ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-mkS ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES} common)
    target_link_libraries(ccn-lite-mkS ccnl-sensor ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ccnl-crypto ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-rmS ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES} common)
    target_link_libraries(ccn-lite-rmS ccnl-sensor ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ccnl-crypto ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-valid ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES} common ccnl-crypto)
    target_link_libraries(ccn-lite-valid ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-rpc ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES} common)
    target_link_libraries(ccn-lite-rpc ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn ${OPENSSL_LIBRARIES})

    target_link_libraries(ccn-lite-pktdump ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS} ${OPENSSL_LIBRARIES} common)
    target_link_libraries(ccn-lite-pktdump ccnl-dump ccnl-core ccnl-pkt ccnl-fwd  ccnl-unix ccnl-nfn ccnl-crypto ${OPENSSL_LIBRARIES})

endif()

target_link_libraries(ccn-lite-fetch ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-fetch ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common)

if(USE_FRAG)
    target_link_libraries(ccn-lite-mkF ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
    target_link_libraries(ccn-lite-mkF ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common)
endif()

target_link_libraries(ccn-lite-mkI ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-mkI ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common)

target_link_libraries(ccn-lite-produce ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-produce ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common ccnl-crypto)

target_link_libraries(ccn-lite-simplenfn ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-simplenfn ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common)

target_link_libraries(ccn-lite-simplenfnConstant ${PROJECT_LINK_LIBS} ${EXT_LINK_LIBS})
target_link_libraries(ccn-lite-simplenfnConstant ccnl-core ccnl-pkt ccnl-fwd ccnl-unix ccnl-nfn common ccnl-dump)
