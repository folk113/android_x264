
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "include/x264/x264.h"

static char *TAG = "example";

typedef struct X264_Param {
    x264_t *handle;
    x264_param_t param;
    x264_picture_t pic;
    x264_picture_t pic_out;
    x264_nal_t *nal;
    int i_nal;
    int i_frame;
    int luma_size;
    int chroma_size;
} X264_Param;

X264_Param x264_param;

int x264_init(int width, int height) {

    if (x264_param_default_preset(&x264_param.param, "medium", NULL) < 0)
    {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "x264_param_default_preset failed");
        return -1;
    }
    x264_param.param.i_bitdepth = 8;
    x264_param.param.i_csp = X264_CSP_YV12;
    x264_param.param.i_width = width;
    x264_param.param.i_height = height;
    x264_param.param.b_vfr_input = 1;
    x264_param.param.b_repeat_headers = 1;
    x264_param.param.b_annexb = 1;
    x264_param.i_frame = 0;

    if (x264_param_apply_profile(&x264_param.param, "high") < 0)
    {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "x264_param_apply_profile failed");
        return -1;
    }

    if (x264_picture_alloc(&x264_param.pic, x264_param.param.i_csp, x264_param.param.i_width,
                           x264_param.param.i_height) < 0)
    {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "x264_picture_alloc failed");
        return -1;
    }
    x264_param.handle = x264_encoder_open(&x264_param.param);
    if (!x264_param.handle)
    {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "x264_encoder_open failed");
        return -1;
    }
    x264_param.luma_size = width * height;
    x264_param.chroma_size = x264_param.luma_size / 4;

    return 0;
}

void x264_release() {
    if (x264_param.handle)
    {
        x264_encoder_close(x264_param.handle);
        x264_param.handle = NULL;
    }
    x264_picture_clean(&x264_param.pic);
}

int x264_encode(uint8_t *data_in, uint8_t *data_out) {
    if(x264_param.handle == NULL)
        return -1;
    memcpy(x264_param.pic.img.plane[0], data_in, x264_param.luma_size);
    memcpy(x264_param.pic.img.plane[1], data_in + x264_param.luma_size, x264_param.chroma_size);
    memcpy(x264_param.pic.img.plane[2], data_in + x264_param.luma_size + x264_param.chroma_size,
           x264_param.chroma_size);
    x264_param.pic.i_pts = x264_param.i_frame++;
    __android_log_print(ANDROID_LOG_INFO, TAG,
                        "data_in[0]=%d data_in[1]=%d data_in[0]=%d data_in[1]=%d", data_in[0],
                        data_in[1], data_in[3], data_in[4]);
    __android_log_print(ANDROID_LOG_INFO, TAG, "luma_size=%d chroma_size=%d i_frame=%d",
                        x264_param.luma_size, x264_param.chroma_size, x264_param.i_frame);

    int i_frame_size = x264_encoder_encode(x264_param.handle, &x264_param.nal, &x264_param.i_nal, &x264_param.pic,
                                           &x264_param.pic_out);
    __android_log_print(ANDROID_LOG_INFO, TAG, "i_frame_size=%d", i_frame_size);
    if (i_frame_size > 0) {
        memcpy(data_out, x264_param.nal->p_payload, i_frame_size);
    }

    return i_frame_size;
}
