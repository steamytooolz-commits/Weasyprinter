# PyPrint: Python & WeasyPrint PDF Studio for Android

PyPrint is a high-performance Android application that embeds a full **CPython 3.11** runtime directly on device via **Chaquopy**, allowing users to author, preview, and generate publication-quality PDF documents using **WeasyPrint**, **ReportLab**, and **FPDF2**.

---

## Key Features

- **WeasyPrint HTML5/CSS3 Engine**: Full support for CSS `@page` media rules, headers, footers, page numbering, typography, and flexbox/grid layout styling.
- **Dynamic Templating (Jinja2 & Markdown)**: Inject variables, calculate totals, and convert Markdown text straight into styled PDFs.
- **Pure Python Direct Drawing (FPDF2 & ReportLab)**: Vector-accurate programmatic document generation without external server calls.
- **Offline & Private**: Documents are processed 100% on the mobile device with zero cloud latency and strict data privacy.
- **Native Document Library**: Local SQLite database via Android **Room** for tracking, searching, filtering, and managing generated PDF artifacts.
- **Built-in PDF Viewer & Printer**: High-res multi-page canvas rendering with Android `PdfRenderer`, system sharing, and direct WiFi/cloud printing via Android `PrintManager`.

---

## GitHub Actions CI Workflow

The automated CI workflow is configured in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) to execute on every `push` and `pull_request` to `main`/`master`, as well as on manual `workflow_dispatch`.

### What the CI Pipeline Does:
1. **Ubuntu Runner Initialization**: Sets up `ubuntu-latest` with JDK 21 (Eclipse Temurin) and Python 3.11.
2. **WeasyPrint & Native Dependencies Installation**: Installs required Linux host rendering packages (`libcairo2`, `libpango-1.0-0`, `libpangocairo-1.0-0`, `libgdk-pixbuf2.0-0`, `libffi-dev`, `shared-mime-info`, `fonts-dejavu-core`, `fonts-liberation`, `ghostscript`).
3. **Environment & Keystore Setup**: Automatically initializes `.env` from `.env.example` and decodes `debug.keystore.base64` to `debug.keystore`.
4. **Gradle 9.3.1 Caching**: Automatically restores and caches Gradle wrapper, plugins, and dependencies with `gradle/actions/setup-gradle@v4`.
5. **Chaquopy Python Wheel Resolution**: Prepares target Python packages (`weasyprint`, `reportlab`, `fpdf2`, `jinja2`, `markdown`) for native Android ABI targets (`arm64-v8a`, `x86_64`).
6. **Comprehensive Test Suite**:
   - `DocumentDatabaseTest`: In-memory Room database operations (insert, search, filter by docType, delete, cascade file cleanup).
   - `DocumentModelsTest`: Financial computations, tax calculations, discounts, and data structures.
   - `HtmlTemplateValidationTest`: Verifies HTML/CSS structures, WeasyPrint `@page` layout rules, and preset templates.
   - `ExampleRobolectricTest`: Robolectric context and Android resource validations.
7. **Debug APK Assembly**: Compiles and signs the debug APK with `debugConfig` via `./gradlew :app:assembleDebug`.
8. **Artifact Publishing**:
   - `pyprint-weasyprint-debug-apk`: Downloadable ready-to-install debug APK (`app-debug.apk`).
   - `pyprint-test-reports`: Full JUnit and Robolectric HTML test reports.

---

## Local Development & CLI Commands

### Run the Full Test Suite:
```bash
./gradlew testDebugUnitTest
```

### Build the Debug APK:
```bash
./gradlew assembleDebug
```
Output location: `app/build/outputs/apk/debug/app-debug.apk`

---

## Architecture Overview

```
├── .github/workflows/ci.yml      # Automated GitHub CI pipeline
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── PyPrintApp.kt     # Application class (CPython start & Room init)
│   │   │   ├── MainActivity.kt   # Jetpack Compose navigation & root scaffold
│   │   │   ├── data/             # Room DB, Entity & Repository layer
│   │   │   ├── python/           # Chaquopy JNI bridge & data models
│   │   │   └── ui/               # Templates, HTML/CSS Studio, REPL, Viewer
│   │   ├── python/pdf_engine.py  # Python PDF generation core (WeasyPrint/FPDF2)
│   │   └── res/                  # App icons, theme, and string resources
│   └── src/test/                 # Comprehensive unit & Robolectric test suite
├── gradle/
│   ├── libs.versions.toml        # Gradle Version Catalog
│   └── wrapper/                  # Gradle 9.3.1 wrapper
└── build.gradle.kts              # Root build configuration
```
