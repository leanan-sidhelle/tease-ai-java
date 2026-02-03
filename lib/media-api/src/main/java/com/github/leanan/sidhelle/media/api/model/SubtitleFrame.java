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
package com.github.leanan.sidhelle.media.api.model;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A subtitle-specific implementation of {@link MediaFrame}.
 * Holds the native {@link AVSubtitle} and its Java-consumable representations.
 * 
 * @param nativeSubtitle The underlying native FFmpeg subtitle struct.
 * @param textLines      Text content (for SRT/ASS).
 * @param images         Bitmaps (for PGS/DVD subtitles).
 * @param pts            Presentation timestamp.
 * @param source         The ordinal index of the input source.
 * @param stream         The stream index within the source.
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public record SubtitleFrame(
    long pts,
    int source,
    int stream,
    Metadata metadata,
    List<String> textLines,
    List<BufferedImage> images
) implements MediaFrame {

    /**
     * Factory method to construct a SubtitleFrame.
     * 
     * @param nativeSubtitle The raw decoded subtitle struct.
     * @param textLines      Parsed text content.
     * @param images         Converted bitmaps.
     * @param source         The source ID.
     * @param stream         The stream ID.
     * @return A new SubtitleFrame instance.
     */
    public static SubtitleFrame of(long pts,int source,int stream,List<String> textLines, List<BufferedImage> images) {
        return new SubtitleFrame(
            pts,
            source,
            stream,
            null,
            textLines,
            images);
    }
    
        /**
     * Factory method to construct a SubtitleFrame.
     * 
     * @param nativeSubtitle The raw decoded subtitle struct.
     * @param textLines      Parsed text content.
     * @param images         Converted bitmaps.
     * @param source         The source ID.
     * @param stream         The stream ID.
     * @return A new SubtitleFrame instance.
     */
    public static SubtitleFrame of(long pts,int source,int stream,Metadata metadata,List<String> textLines, List<BufferedImage> images) {
        return new SubtitleFrame(
            pts,
            source,
            stream,
            metadata,
            textLines,
            images);
    }

    @Override public long getPts() { return pts; }
    @Override public int getSourceIndex() { return source; }
    @Override public int getStreamIndex() { return stream; }
    @Override public Metadata getMetadata() { return metadata; }

    /**
     * Releases the native subtitle memory.
     * Uses {@link avcodec#avsubtitle_free} to ensure internal rects are freed.
     */
    @Override
    public void close() {
        //nothing to do here right now...
    }
}
