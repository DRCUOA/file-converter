package app.core;

import java.util.List;
import java.util.Map;

/**
 * Predefined conversion profiles for MVP.
 */
public final class Profiles {

    private Profiles() {
    }

    public static final ConversionProfile MOD_TO_MOV = new ConversionProfile(
            "mod-mov",
            "MOD → MOV",
            "mod",
            "mov",
            Map.of("video_codec", "h264", "crf", 23)
    );

    public static final ConversionProfile JPEG_TO_WEBP = new ConversionProfile(
            "jpg-webp",
            "JPEG → WEBP",
            "jpg",
            "webp",
            Map.of("quality", 85)
    );

    public static final ConversionProfile DOCX_TO_PDF = new ConversionProfile(
            "docx-pdf",
            "DOCX → PDF",
            "docx",
            "pdf",
            Map.of("engine", "office")
    );

    public static List<ConversionProfile> all() {
        return List.of(MOD_TO_MOV, JPEG_TO_WEBP, DOCX_TO_PDF);
    }
}
