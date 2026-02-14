package app.core;

import java.util.Map;

/**
 * Defines a conversion profile (input format, output format, options).
 */
public record ConversionProfile(
        String id,
        String displayName,
        String inputFormat,
        String outputFormat,
        Map<String, Object> convertOptions
) {}
