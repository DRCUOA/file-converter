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
            Map.of("video_codec", "x264", "crf", 23)
    );

    public static final ConversionProfile JPEG_TO_WEBP = new ConversionProfile(
            "jpg-webp",
            "JPEG → WEBP",
            "jpg",
            "webp",
            Map.of("quality", 85)
    );

    public static final ConversionProfile PNG_TO_JPG = new ConversionProfile(
            "png-jpg",
            "PNG → JPG",
            "png",
            "jpg",
            Map.of()
    );

    public static final ConversionProfile PNG_TO_WEBP = new ConversionProfile(
            "png-webp",
            "PNG → WEBP",
            "png",
            "webp",
            Map.of("quality", 85)
    );

    public static final ConversionProfile WEBP_TO_PNG = new ConversionProfile(
            "webp-png",
            "WEBP → PNG",
            "webp",
            "png",
            Map.of()
    );

    public static final ConversionProfile DOCX_TO_PDF = new ConversionProfile(
            "docx-pdf",
            "DOCX → PDF",
            "docx",
            "pdf",
            Map.of("engine", "office")
    );

    public static final ConversionProfile DOC_TO_PDF = new ConversionProfile(
            "doc-pdf",
            "DOC → PDF",
            "doc",
            "pdf",
            Map.of("engine", "office")
    );

    public static final ConversionProfile PDF_TO_DOCX = new ConversionProfile(
            "pdf-docx",
            "PDF → DOCX",
            "pdf",
            "docx",
            Map.of()
    );

    public static final ConversionProfile PPTX_TO_PDF = new ConversionProfile(
            "pptx-pdf",
            "PPTX → PDF",
            "pptx",
            "pdf",
            Map.of("engine", "office")
    );

    public static final ConversionProfile XLSX_TO_PDF = new ConversionProfile(
            "xlsx-pdf",
            "XLSX → PDF",
            "xlsx",
            "pdf",
            Map.of("engine", "office")
    );

    public static final ConversionProfile MOV_TO_MP4 = new ConversionProfile(
            "mov-mp4",
            "MOV → MP4",
            "mov",
            "mp4",
            Map.of()
    );

    public static final ConversionProfile MP4_TO_MOV = new ConversionProfile(
            "mp4-mov",
            "MP4 → MOV",
            "mp4",
            "mov",
            Map.of()
    );

    public static final ConversionProfile MP4_TO_MP3 = new ConversionProfile(
            "mp4-mp3",
            "MP4 → MP3",
            "mp4",
            "mp3",
            Map.of()
    );

    public static final ConversionProfile WAV_TO_MP3 = new ConversionProfile(
            "wav-mp3",
            "WAV → MP3",
            "wav",
            "mp3",
            Map.of()
    );

    public static List<ConversionProfile> all() {
        return List.of(
                MOD_TO_MOV,
                JPEG_TO_WEBP,
                PNG_TO_JPG,
                PNG_TO_WEBP,
                WEBP_TO_PNG,
                DOCX_TO_PDF,
                DOC_TO_PDF,
                PDF_TO_DOCX,
                PPTX_TO_PDF,
                XLSX_TO_PDF,
                MOV_TO_MP4,
                MP4_TO_MOV,
                MP4_TO_MP3,
                WAV_TO_MP3
        );
    }
}
