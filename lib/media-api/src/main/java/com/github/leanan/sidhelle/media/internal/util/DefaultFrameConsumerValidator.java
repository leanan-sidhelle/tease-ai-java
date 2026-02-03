/**
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * 
 * For more information, please refer to <https://unlicense.org>
 */
package com.github.leanan.sidhelle.media.internal.util;

import com.github.leanan.sidhelle.media.api.model.MediaFrame;
import com.github.leanan.sidhelle.media.util.AudioFrameConsumer;
import com.github.leanan.sidhelle.media.util.FrameConsumer;
import com.github.leanan.sidhelle.media.util.FrameConsumerValidator;
import com.github.leanan.sidhelle.media.util.SubtitleFrameConsumer;
import com.github.leanan.sidhelle.media.util.VideoFrameConsumer;
import org.bytedeco.ffmpeg.global.avutil;

/**
 * Standard implementation of consumer validation for Video, Audio, and Subtitles.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class DefaultFrameConsumerValidator implements FrameConsumerValidator {

    @Override
    public void validate(int mediaType, FrameConsumer<? extends MediaFrame> consumer) {
        if (consumer == null) return;

        switch (mediaType) {
            case avutil.AVMEDIA_TYPE_VIDEO -> check(consumer, 
                    VideoFrameConsumer.class, "Video");
            case avutil.AVMEDIA_TYPE_AUDIO -> check(consumer, 
                    AudioFrameConsumer.class, "Audio");
            case avutil.AVMEDIA_TYPE_SUBTITLE -> check(consumer, 
                    SubtitleFrameConsumer.class, "Subtitle");
            case avutil.AVMEDIA_TYPE_DATA -> {
            }
            default -> {
                String msg = String.format(
                        "No consumers mapped for mediaType=%d.",mediaType);
                throw new IllegalArgumentException(msg);
            }
        }
        /*
         * Custom hook: Currently allows any FrameConsumer<MediaFrame>.
         * You can add a 'DataFrameConsumer' check here later.
         */
        // Unknown or Attachment types - allow generic consumer
    }

    /** 
     * Helper to perform the instance check and format the error message.
     * @param consumer
     * @param expected
     * @param typeName 
     */
    protected void check(FrameConsumer<?> consumer, Class<?> expected, String typeName) {
        if (!expected.isInstance(consumer)) {
            throw new IllegalArgumentException(String.format(
                "Incompatible Consumer: Expected %sFrameConsumer but received %s",
                typeName, consumer.getClass().getSimpleName()
            ));
        }
    }
}
