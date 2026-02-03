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

package com.github.leanan.sidhelle.media.util;

import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.internal.util.DefaultMediaStateBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

/**
 * Fluent builder for configuring and initializing a {@link MediaState}.
 * <p>
 * This API provides explicit methods for stream selection, timing control,
 * and support for diverse data sources including Java {@link InputStream}s.
 * </p>
 * 
 * @author L. Sidhelle &lt;leanan-sidhelle@yahoo.com&gt;
 */
public interface MediaStateBuilder {

    /**
     * Get a new MediaStateBuilder.
     * @return A new instance of the default builder implementation. 
     */
    public static MediaStateBuilder builder() {
        return new DefaultMediaStateBuilder();
    }
    
    /**
     * Finalizes configuration and performs native FFmpeg initialization.
     * 
     * @return A thread-safe, read-only {@link MediaState}.
     */
    public MediaState build();

    /** 
     * Adds a file source with optional start and duration limits. 
     * 
     * @param file  The media file on disk.
     * @param start The seek position. Use {@link Duration#ZERO} for the beginning.
     * @param limit The maximum duration to read. Use {@code null} for no limit.
     * @return This builder instance for chaining.
     * @throws IOException if the file is inaccessible.
     */
    public MediaStateBuilder source(File file, Duration start, Duration limit) throws IOException;

    /** 
     * Adds a file source with optional start and duration limits. 
     * 
     * @param file  The media file on disk.
     * @return This builder instance for chaining.
     * @throws IOException if the file is inaccessible.
     */
    public MediaStateBuilder source(File file) throws IOException;

    /** 
     * Adds a URL source with optional start and duration limits. 
     * 
     * @param url  The media network url.
     * @param start The seek position. Use {@link Duration#ZERO} for the beginning.
     * @param limit The maximum duration to read. Use {@code null} for no limit.
     * @return This builder instance for chaining.
     * @throws IOException if the file is inaccessible.
     */
    public MediaStateBuilder source(URL url, Duration start, Duration limit) throws IOException;

        /** 
     * Adds a URL source with optional start and duration limits. 
     * 
     * @param url  The media network url.
     * @return This builder instance for chaining.
     * @throws IOException if the file is inaccessible.
     */
    public MediaStateBuilder source(URL url) throws IOException;

    /** 
     * Adds an {@link InputStream} as a source. 
     * <p>
     * Note: Seeking behavior (start time) on an InputStream depends on whether 
     * the underlying stream supports seeking.
     * </p>
     * 
     * @param stream The input stream (e.g., from a ZipEntry).
     * @param limit  The maximum duration to read.
     * @return This builder instance for chaining.
     * @throws IOException if the stream cannot be read.
     */
    public MediaStateBuilder source(InputStream stream, Duration limit) throws IOException;

    /** 
     * Adds an {@link InputStream} as a source. 
     * <p>
     * Note: Seeking behavior (start time) on an InputStream depends on whether 
     * the underlying stream supports seeking.
     * </p>
     * 
     * @param stream The input stream (e.g., from a ZipEntry).
     * @return This builder instance for chaining.
     * @throws IOException if the stream cannot be read.
     */
    public MediaStateBuilder source(InputStream stream) throws IOException;

    /** 
     * Disables processing for all video streams across all sources.
     * @return 
     */
    public MediaStateBuilder disableVideo();

    /** 
     * Disables processing for all audio streams across all sources.
     * @return 
     */
    public MediaStateBuilder disableAudio();

    /** 
     * Maps a specific stream selector to a video consumer.
     * 
     * @param selector The FFmpeg stream specifier (e.g., "v:0").
     * @param consumer The handler for decoded {@link library.api.model.VideoFrame} records.
     * @return This builder instance for chaining.
     */
    public MediaStateBuilder map(String selector, VideoFrameConsumer consumer);

    /** Maps a specific stream selector to an audio consumer. 
     * @param selector The FFmpeg stream specifier (e.g., "v:0").
     * @param consumer The handler for decoded {@link library.api.model.AudioFrame} records.
     * @return This builder instance for chaining.
     */
    public MediaStateBuilder map(String selector, AudioFrameConsumer consumer);

    /** 
     * Maps a specific stream selector to a subtitle consumer. 
     * 
     * @param selector The FFmpeg stream specifier (e.g., "v:0").
     * @param consumer The handler for decoded {@link library.api.model.SubtitleFrame} records.
     * @return This builder instance for chaining.
     */
    public MediaStateBuilder map(String selector, SubtitleFrameConsumer consumer);
    
}

