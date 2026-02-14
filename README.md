# File Converter

Native desktop batch file converter using CloudConvert API. Converts MOD→MOV, JPEG→WEBP, DOCX→PDF.

## Requirements

- Java 21+
- CloudConvert API key

## Setup

1. Set your CloudConvert API key:
   - Environment: `export CLOUDCONVERT_API_KEY=your-key`
   - Or add to `~/.file-converter/settings.json`: `{"apiKey": "your-key", ...}`

2. Build: `./gradlew installDist`

3. Run: `./build/install/file-converter/bin/file-converter`

## Packaging (macOS)

`./gradlew jpackageApp` produces a native app in `build/jpackage/`.

## Usage

1. Select output directory
2. Add files (MOD, JPEG, DOCX)
3. Choose profile (MOD→MOV, JPEG→WEBP, DOCX→PDF)
4. Click Start
