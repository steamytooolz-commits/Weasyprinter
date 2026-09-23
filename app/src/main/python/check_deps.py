# -*- coding: utf-8 -*-
"""
PyPrint Environment & Dependency Diagnostics Script
Checks availability, version information, and shared library paths for
cairo, pango, weasyprint, cffi, fontconfig, and related C shared libraries in Chaquopy.
"""

import sys
import os
import json
import logging
import ctypes
import ctypes.util

logger = logging.getLogger("check_deps")

def check_dependencies() -> str:
    """
    Diagnostic checker that inspects Python packages and native C library linkage.
    Returns a formatted JSON report string.
    """
    report = {
        "python_version": sys.version,
        "platform": sys.platform,
        "executable": sys.executable,
        "packages": {},
        "shared_libraries": {},
        "system_fonts": []
    }

    # 1. Inspect WeasyPrint
    try:
        import weasyprint
        report["packages"]["weasyprint"] = {
            "available": True,
            "version": getattr(weasyprint, "__version__", "Unknown"),
            "file": getattr(weasyprint, "__file__", "Builtin")
        }
    except Exception as e:
        report["packages"]["weasyprint"] = {"available": False, "error": str(e)}

    # 2. Inspect Cairo
    try:
        import cairo
        report["packages"]["cairo"] = {
            "available": True,
            "version": getattr(cairo, "__version__", getattr(cairo, "version", "Available")),
            "file": getattr(cairo, "__file__", "Native")
        }
    except Exception as e:
        report["packages"]["cairo"] = {"available": False, "error": str(e)}

    # 3. Inspect Pango
    try:
        import pango
        report["packages"]["pango"] = {
            "available": True,
            "version": getattr(pango, "__version__", "Available"),
            "file": getattr(pango, "__file__", "Native")
        }
    except Exception as e:
        report["packages"]["pango"] = {"available": False, "error": str(e)}

    # 4. Inspect CFFI
    try:
        import cffi
        report["packages"]["cffi"] = {
            "available": True,
            "version": getattr(cffi, "__version__", "Available"),
            "file": getattr(cffi, "__file__", "Native")
        }
    except Exception as e:
        report["packages"]["cffi"] = {"available": False, "error": str(e)}

    # 5. Check shared C libraries resolution via ctypes
    c_libs = ["cairo", "pango-1.0", "pangocairo-1.0", "pangoft2-1.0", "fontconfig", "gobject-2.0", "glib-2.0", "freetype", "harfbuzz"]
    for lib in c_libs:
        try:
            resolved_path = ctypes.util.find_library(lib)
            report["shared_libraries"][lib] = {
                "resolved_path": resolved_path or "Not found via find_library (Managed by Native Resolver)",
                "status": "Resolved" if resolved_path else "Unresolved"
            }
        except Exception as e:
            report["shared_libraries"][lib] = {"status": "Error", "error": str(e)}

    # 6. Check Android System Fonts
    font_dirs = ["/system/fonts", "/product/fonts", "/system/etc/fonts", "/data/fonts"]
    fonts_found = []
    for fdir in font_dirs:
        if os.path.exists(fdir) and os.path.isdir(fdir):
            for root, _, files in os.walk(fdir):
                for f in files:
                    if f.lower().endswith((".ttf", ".otf", ".ttc")):
                        fonts_found.append(os.path.join(root, f))
                        if len(fonts_found) >= 10:
                            break
                if len(fonts_found) >= 10:
                    break

    report["system_fonts"] = {
        "count": len(fonts_found),
        "sample_paths": fonts_found[:5]
    }

    return json.dumps(report, indent=2)


if __name__ == "__main__":
    diag_result = check_dependencies()
    print("=== CHAQUOPY DEPENDENCY DIAGNOSTICS ===")
    print(diag_result)
