# Converto

Native desktop batch file converter using CloudConvert API. Converts MOD→MOV, JPEG→WEBP, DOCX→PDF, MOV→MP4, and more.

## Requirements

- macOS (for the packaged app)
- Java 21+ (for building)
- CloudConvert API key

## Quick start

### Launch the app (if already built)

1. Open Terminal.
2. Go to the project folder: `cd /path/to/file-converter`  
   (Current location: `/Volumes/SecureData/c26/active/file-converter`)
3. Run: `./run-converto.sh`

The app will open. If you see “Converto.app not found”, build it first (see below).

### Build and launch (first time, or after code changes)

1. Open Terminal.
2. Go to the project folder: `cd /path/to/file-converter`  
   (Current location: `/Volumes/SecureData/c26/active/file-converter`)
3. Run: `./run-converto.sh --rebuild`

This builds the app and then launches it.

4. **Optional** (run this single command):  
   `cd /Volumes/SecureData/c26/active/file-converter; ./run-converto.sh --rebuild`

## Setup (API key)

Before converting files, set your CloudConvert API key:

- **Option A:** Add to `settings.json` in the project folder:  
  `{"apiKey": "your-key", "lastOutputDir": "", "lastProfileId": ""}`
- **Option B:** In Terminal before launching:  
  `export CLOUDCONVERT_API_KEY=your-key`

## Usage

1. Select output directory
2. Add files (MOD, JPEG, DOCX, MOV, MP4, etc.)
3. Choose a conversion profile (e.g. MOD→MOV, MOV→MP4, DOCX→PDF)
4. Click Start

## Logging

- Logs are written to `~/.file-converter.log`
- Application package logs (`app.*`) default to `DEBUG`
- Override levels with env vars:
  - `APP_LOG_LEVEL` (default: `DEBUG`)
  - `ROOT_LOG_LEVEL` (default: `INFO`)

### Log viewer (web preview)

To view logs in a browser with filters and copy-to-clipboard:

1. From the project folder, run: `python3 serve-log.py`
2. Open [http://localhost:8765](http://localhost:8765) in your browser
