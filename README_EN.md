# SleepArchiver

Русский: see [README.md](README.md)

Desktop application to manage sleep data from Sleeptracker watches (SleepTracker, Sleep Tracker).

## Description

SleepArchiver is a desktop application for downloading, storing and analyzing sleep data from Sleeptracker watches. The app helps track sleep quality, duration, number of awakenings and other metrics.

This repository is a modern fork of the original Pavel Fatin project (2013), rewritten and updated.

## Features

- Download data from Sleeptracker watches over a serial port
- Store sleep history as XML (GZIP)
- Visualize sleep data (quality chart, awakenings)
- Edit sleep entries (time, conditions, notes)
- Import/export data as CSV
- Sleep statistics and metrics
- Multi-language UI (English, Russian)
- Cross-platform (macOS, Windows, Linux)

## Technologies

### Current (2026)
- **Java 21**
- **JavaFX**
- **Gradle 8.12**
- **jSerialComm** (replacement for RXTX)
- **Jakarta JAXB**
- **Apache Commons CSV**
- **JUnit 5** (69 tests)

### Original (2013)
- Java 6, Swing, Ant, RXTX, JGoodies Forms, AppFramework

## Requirements

- Java 21 or newer
- Gradle 8.12 or newer (wrapper included)
- Sleeptracker watch with USB cable

## Installation

### Prebuilt packages

Download installers from the Releases page: https://github.com/JoTesla/sleeparchiver/releases

- **macOS**: SleepArchiver-2.0.0.dmg
- **Windows**: SleepArchiver-2.0.0.msi
- **Linux**: SleepArchiver-2.0.0.deb

#### macOS installation notes

The DMG is not code-signed, so macOS may block launching the app. To install and run:

1. Open the downloaded `.dmg` and drag SleepArchiver to the `Applications` folder.
2. Launch SleepArchiver via Launchpad, Spotlight or Finder — macOS may present a warning about an unsigned app. Click "Cancel" (do not move the app to Trash).
3. Open System Settings → Privacy & Security.
4. In the lower part of the window you should see a message about the blocked app — click "Open Anyway".

Tip: as an alternative, Control‑click the app in Finder and choose "Open", then confirm in the dialog.

## Building from source

```bash
# Clone repository
git clone https://github.com/JoTesla/sleeparchiver.git
cd sleeparchiver

# Build
./gradlew build

# Run
./gradlew run

# Run tests
./gradlew test

# Create platform packages (DMG, MSI, DEB)
./gradlew jpackage
```

## Creating a release

Tag and push a new version — GitHub Actions will build platform installers and publish a Release with artifacts.

```bash
git tag v2.0.0
git push origin v2.0.0
```

## Usage

### Important before connecting

- The watch must have the alarm turned ON — if the alarm is off, the watch does not record sleep data.
- Before connecting to the computer, navigate the watch to the "Date" screen (the screen that shows sleep data).

### Data acquisition process

1. Make sure the alarm on the watch was enabled during sleep.
2. On the watch, go to the "Date" screen (sleep data screen).
3. Connect the Sleeptracker watch to the computer via USB.
4. Start the application.
5. Select the serial port and baud rate (usually 19200).
6. Click "Acquire" to read data from the watch.
7. Data is saved automatically as `.xmz` (GZIP-compressed XML).

Note: After reading, the data remains on the watch, but the watch exits transfer mode. To read again, reopen the Date screen on the watch.

## Data format

- **.xmz** — main storage format (GZIP-compressed XML via JAXB)
- **.csv** — tabular import/export

## Project structure

```
src/main/java/              - application source code
├── model/                  - data model (Night, Device, Document)
├── gui/                    - JavaFX UI
│   ├── main/               - main window and commands
│   ├── night/              - edit night dialog
│   ├── conditions/         - sleep conditions
│   ├── download/           - download dialog
│   └── preferences/        - settings
└── lang/                   - i18n utilities

src/main/resources/         - resources (icons, translations)
src/test/java/              - JUnit 5 tests
```

## License

GPL v3 — see LICENSE

## Authors

- Original project: Pavel Fatin (2010–2013)
- Fork and migration: Evgen Tamarovsky (JoTesla) (2026)

## History

- 2010–2013: original Java 6 / Swing / Ant version
- 2026: migrated to Java 21 / JavaFX / Gradle with Russian localization

## Known issues & notes

- The alarm must be ON to record sleep data.
- The watch only sends data once when the Date screen is opened.
- After readout the watch leaves transfer mode; to read again reopen the Date screen.
- The correct baud rate is important (typically 19200).
- Handshake may not work — the watch may stream immediately when Date is opened.

## Links

- GitHub: https://github.com/JoTesla/sleeparchiver
- Original project: https://code.google.com/archive/p/sleeparchiver/
