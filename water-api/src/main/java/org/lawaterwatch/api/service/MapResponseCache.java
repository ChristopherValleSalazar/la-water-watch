package org.lawaterwatch.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

//GET /api/systems/map serialized and gzipped once per data load instead of on every request
//Load testing showed Tomcat's live gzip of the ~600 KB map cost ~30 ms CPU per request (28x lower throughput)
//The map only changes when the DataLoader runs, so every visitor can share the same bytes
@Service
public class MapResponseCache {

    private static final Logger log = LoggerFactory.getLogger(MapResponseCache.class);

    //Both encodings of the same JSON, the ETags differ because the bytes on the wire differ
    public record Body(byte[] json, byte[] gzip, String etag, String gzipEtag) {}

    private final WaterSystemService waterSystemService;
    private final ObjectMapper objectMapper;

    //null until the first build, then swapped as a whole on rebuild
    private volatile Body body;

    public MapResponseCache(WaterSystemService waterSystemService, ObjectMapper objectMapper) {
        this.waterSystemService = waterSystemService;
        this.objectMapper = objectMapper;
    }

    //Called by the DataLoader after the database changes
    public void rebuild() {
        byte[] json = objectMapper.writeValueAsBytes(waterSystemService.map());
        byte[] gzip = gzip(json);
        String hash = sha256(json);
        body = new Body(json, gzip, hash, hash + "-gz");
        log.info("map response cached: {} KB json, {} KB gzip", json.length / 1024, gzip.length / 1024);
    }

    public Body get() {
        Body current = body;
        if (current == null) {
            //Only when the DataLoader hasn't run yet (e.g. tests), build once and share
            synchronized (this) {
                if (body == null) {
                    rebuild();
                }
                current = body;
            }
        }
        return current;
    }

    //Built once, so spend the extra CPU on the smallest output
    static byte[] gzip(byte[] data) {
        var out = new ByteArrayOutputStream(data.length / 4);
        try (var gz = new GZIPOutputStream(out) {
            {
                def.setLevel(Deflater.BEST_COMPRESSION);
            }
        }) {
            gz.write(data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data), 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
