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
package com.github.leanan.sidhelle.media.internal.codec;

import com.github.leanan.sidhelle.media.api.model.AudioFrame;
import org.junit.jupiter.api.*;

import org.bytedeco.ffmpeg.avcodec.AVPacket;

import static org.junit.jupiter.api.Assertions.*;


import com.github.leanan.sidhelle.media.api.core.MediaState;
import com.github.leanan.sidhelle.media.api.core.SourceState;
import com.github.leanan.sidhelle.media.api.core.StreamState;
import com.github.leanan.sidhelle.media.api.exception.FFmpegException;
import com.github.leanan.sidhelle.media.util.MediaStateBuilder;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;

import java.io.File;
import java.net.URL;


/**
 * Unit tests for {@link AudioDecoder} using the test-pattern.mp4 resource.
 * <p>
 * Validates that AAC encoded data is correctly converted into 16-bit PCM.
 * This test handles interleaved stream backpressure by draining unused video packets.
 * </p>
 * @author L. Sidhelle &ltleanan-sidhelle@yahoo.com&gt
 */
public class AudioDecoderTest {

    /**
     * TODO:
     */
    private String testVideoPath;
    
    /**
     * TODO:
     */
    private AudioDecoder audioDecoder;

    /**
     * TODO:
     */
    @BeforeEach
    public void setUp() {
        avformat.avformat_network_init();
        URL resource = getClass().getClassLoader().getResource("test-pattern.mp4");
        assertNotNull(resource, "test-pattern.mp4 not found in src/test/resources");
        testVideoPath = new File(resource.getFile()).getAbsolutePath();
        audioDecoder = new AudioDecoder();
    }

    /**
     * TODO:
     * @throws Exception 
     */
    @AfterEach
    public void tearDown() throws Exception {
        if (audioDecoder != null) {
            audioDecoder.close();
        }
    }

    /**
     * Drains and frees all packets from streams we aren't currently testing.
     * <p>
     * Since the MP4 file is interleaved, we must clear the video packet queue 
     * to allow the demuxer to continue reading until it finds the next audio packet.
     * </p>
     */
    private void drainUnusedStreams(SourceState<?> source, int targetStreamIndex) {
        for (StreamState stream : source.getStreams()) {
            if (stream.stream() != targetStreamIndex) {
                AVPacket pkt;
                while ((pkt = stream.packetQueue().poll()) != null) {
                    avcodec.av_packet_free(pkt);
                }
            }
        }
    }

    @Test
    @DisplayName("Test Audio Decoding and PCM Conversion")
    void testAudioDecoding() throws Exception {
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath))
                .build()) {

            SourceState<?> source = state.getSources().get(0);
            StreamState audioStream = source.getStreams().stream()
                    .filter(s -> s.getMediaType() == avutil.AVMEDIA_TYPE_AUDIO)
                    .findFirst()
                    .orElseThrow();

            int decodedFrameCount = 0;
            int packetReadLimit = 200;

            while (decodedFrameCount < 5 && packetReadLimit-- > 0) {
                try {
                    /* 1. Attempt to demux a packet */
                    source.getDemuxTask().apply(source);
                } catch (FFmpegException e) {
                    /* 2. Handle Backpressure: If queues are full, drain non-audio packets */
                    if (e.getErrorCode() == avutil.AVERROR_EAGAIN()) {
                        drainUnusedStreams(source, audioStream.stream());
                        continue;
                    }
                    throw e;
                }

                /* 3. Run the audio decoder */
                audioDecoder.decode(audioStream,false);

                /* 4. Verify decoded PCM record */
                AudioFrame frame = (AudioFrame) audioStream.frameQueue().poll();
                if (frame != null) {
                    decodedFrameCount++;
                    assertNotNull(frame.audioData(), "PCM data should not be null");
                    assertEquals(1, frame.getStreamIndex(), "Stream index should be 1 for audio");
                    assertEquals(0, frame.getSourceIndex(), "Source index should be 0");
                }
            }

            assertTrue(decodedFrameCount > 0, "No audio frames were decoded");
        }
    }

    @Test
    @DisplayName("Test Audio Decoder Reset")
    void testAudioDecoderReset() throws Exception {
        try (MediaState state = MediaStateBuilder.builder()
                .source(new File(testVideoPath))
                .build()) {

            StreamState audioStream = state.getSources().get(0).getStreams().stream()
                    .filter(s -> s.getMediaType() == avutil.AVMEDIA_TYPE_AUDIO)
                    .findFirst().orElseThrow();
            
            assertDoesNotThrow(() -> audioDecoder.reset(audioStream));
        }
    }
}
