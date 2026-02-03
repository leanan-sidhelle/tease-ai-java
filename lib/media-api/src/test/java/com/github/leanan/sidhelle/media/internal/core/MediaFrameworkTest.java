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
package com.github.leanan.sidhelle.media.internal.core;

import com.github.leanan.sidhelle.media.api.core.MediaSourceState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.exception.FFmpegException;
import com.github.leanan.sidhelle.media.util.MediaUtils;
import com.github.leanan.sidhelle.media.util.SourceTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import org.bytedeco.javacpp.PointerPointer;


/**
 * Unit tests for the {@link MediaState} hierarchy.
 * <p>
 * This class validates the manual initialization of native FFmpeg contexts
 * and ensures that the hierarchical state model correctly manages resource
 * lifetimes using the {@link DefaultMediaState} implementation.
 * </p>
 * 
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class MediaFrameworkTest {

    /** The central media registry under test. */
    private DefaultMediaState mediaState;

    /** Path to the test-pattern.mp4 file in src/test/resources. */
    private String testVideoPath;
    
    /**
     * Performs one-time setup for the native FFmpeg environment.
     */
    @BeforeAll
    public static void initFFmpeg() {
        /* Initialize the native network protocols and muxers */
        avformat_network_init();
    }

    /**
     * Initializes the {@link MediaState} instance and resolves the test file path
     * before each individual test case execution.
     * @throws java.io.IOException
     */
    @BeforeEach
    public void setUp() throws IOException {
        /* Create a new registry instance for each test */

        /* Locate the test resource in the classpath */
        URL resource = getClass().getClassLoader().getResource("test-pattern.mp4");
        assertNotNull(resource, "test-pattern.mp4 not found in src/test/resources");
        testVideoPath = new File(resource.getFile()).getAbsolutePath();
        
        /* Manually initialize and load test-pattern.mp4 */
        AVFormatContext fmt_ctx = new AVFormatContext(null);
        int ret = avformat_open_input(fmt_ctx,testVideoPath, null, null);
        if(ret<0) {
            String msg = String.format("Error loading %s.",testVideoPath);
            throw new IOException(msg);
        }
        
        /* Scan streams for information */
        ret = avformat_find_stream_info(fmt_ctx,(PointerPointer)null);
        if(ret<0) {
            String msg = "Error reading stream information.";
            throw new IOException(msg);
        }
        
        /* Manually create codecs for each of the streams. */
        List<StreamState> streams = new ArrayList<>();
        for(int x=0;x<fmt_ctx.nb_streams();x++) {
            AVCodecContext codec_ctx = avcodec_alloc_context3(null);
            avcodec_parameters_to_context(codec_ctx,fmt_ctx.streams(x).codecpar());
            AVCodec codec = avcodec_find_decoder(codec_ctx.codec_id());
            if(codec == null) {
                String msg = String.format("Unable to find codec for id=%d.",
                        codec_ctx.codec_id());
                throw new IllegalStateException(msg);
            }
            ret = avcodec_open2(codec_ctx, codec,(PointerPointer)null);
            if(ret<0) {
                //TODO:add codec name to message?
                throw new IllegalStateException("Unable to open codec.");
            }
            streams.add(new DefaultStreamState(0,x,fmt_ctx,codec_ctx.codec_type(),
                    codec_ctx,null,null,null));
        }
        MediaSourceState source = new DefaultMediaSourceState(resource,fmt_ctx, 
            streams,this::demux);
        mediaState = new DefaultMediaState(Arrays.asList(source)); 
    }
    
    protected void demux(SourceState<?> state) {
        //TODO:
    }

    /**
     * Cleans up the {@link MediaState} and all associated native contexts
     * after each test case execution.
     * 
     * @throws Exception if native memory deallocation fails.
     */
    @AfterEach
    public void tearDown() throws Exception {
        /* Recursively closes all sources and streams registered in the state */
        if (mediaState != null) {
            mediaState.close();
        }
    }

    /**
     * Tests the manual assembly of a {@link MediaSourceState} and its child 
     * {@link StreamState} objects from a real MP4 file.
     * 
     * @throws Exception if native FFmpeg calls return error codes.
     */
    @Test
    @DisplayName("Manual Initialization of MediaSourceState and StreamState")
    void testManualLoading() throws Exception {
        /* Allocate the native format context for demuxing */
        AVFormatContext fmtCtx = avformat.avformat_alloc_context();
        
        /* Open the input file */
        int ret = avformat_open_input(fmtCtx, testVideoPath, null, null);
        assertEquals(0, ret, "Could not open test file: " + testVideoPath);

        /* Find stream information to populate metadata */
        ret = avformat_find_stream_info(fmtCtx,(AVDictionary)null);
        assertTrue(ret >= 0, "Could not find stream information");

        /* Define a basic demuxing task using the native format context */
        SourceTask demuxTask = s -> {
            AVPacket pkt = avcodec.av_packet_alloc();
            if(s instanceof MediaSourceState media) {
                int code = av_read_frame(media.formatContext(),pkt);
                if(code<0) {
                    String msg = MediaUtils.getErrorMessage(code);
                    throw new FFmpegException(msg,code);
                }
            }
        };
        
        /* Manually create codecs for each of the streams. */
        List<StreamState> streams = new ArrayList<>();
        for(int x=0;x<fmtCtx.nb_streams();x++) {
            AVCodecContext codec_ctx = avcodec_alloc_context3(null);
            avcodec_parameters_to_context(codec_ctx,fmtCtx.streams(x).codecpar());
            AVCodec codec = avcodec_find_decoder(codec_ctx.codec_id());
            if(codec == null) {
                String msg = String.format("Unable to find codec for id=%d.",
                        codec_ctx.codec_id());
                throw new IllegalStateException(msg);
            }
            ret = avcodec_open2(codec_ctx, codec,(PointerPointer)null);
            if(ret<0) {
                //TODO:add codec name to message?
                throw new IllegalStateException("Unable to open codec.");
            }
            streams.add(new DefaultStreamState(0,x,fmtCtx,codec_ctx.codec_type(),
                    codec_ctx,null,null,null));
        }
        
        /* Create the source state implementation */
        File sourceFile = new File(testVideoPath);
        DefaultMediaSourceState<File> sourceState = 
                new DefaultMediaSourceState(sourceFile,fmtCtx,streams,demuxTask);

        /* Validate the hierarchy */
        assertEquals(1, mediaState.getSources().size(), "MediaState should have 1 source");
        assertFalse(sourceState.getStreams().isEmpty(), "Source should have at least one stream");
        
        /* Verify that we can access the native pointers via the API */
        assertStream(sourceState.getStreams().get(0),AVMEDIA_TYPE_VIDEO,"Should be video stream.");
        assertStream(sourceState.getStreams().get(1),AVMEDIA_TYPE_AUDIO,"Should be audio stream.");
        //TODO:other streams...
    }
    
    protected void assertStream(StreamState stream,int type,String msg) {
        assertNotNull(stream.codecContext(),"StreamState must hold a valid AVCodecContext");
        assertEquals(type,stream.getMediaType(),msg);
    }

    /**
     * Validates the error propagation of the DemuxTask when reaching End of File.
     */
    @Test
    @Disabled
    @DisplayName("Verify DemuxTask behavior at EOF")
    void testDemuxTaskEOF() throws Exception {
        /* Implementation note: In a full test, we would loop call() 
           until avutil.AVERROR_EOF is reached. */
        assertNotNull(mediaState, "MediaState must be initialized by @BeforeEach");
        fail("Not fully implemented yet.");
    }
}
