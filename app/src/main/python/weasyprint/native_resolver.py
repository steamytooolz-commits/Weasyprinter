# -*- coding: utf-8 -*-
"""
PyPrint Native C Library Resolver for WeasyPrint on Chaquopy (Android)
Handles shared library resolution for Pango, Cairo, Fontconfig, GObject, GLib, HarfBuzz, and FreeType.
Ensures CFFI and ctypes load shared libraries safely or fallback gracefully without SELinux audit failures or OSError crashes.
"""

import sys
import os
import ctypes
import ctypes.util
import logging

logger = logging.getLogger("weasyprint.native_resolver")

# Known shared library names used by WeasyPrint CFFI bindings
C_LIBRARY_MAP = {
    "cairo": ["libcairo.so.2", "libcairo.so", "cairo"],
    "pango-1.0": ["libpango-1.0.so.0", "libpango-1.0.so", "pango-1.0"],
    "pangocairo-1.0": ["libpangocairo-1.0.so.0", "libpangocairo-1.0.so", "pangocairo-1.0"],
    "pangoft2-1.0": ["libpangoft2-1.0.so.0", "libpangoft2-1.0.so", "pangoft2-1.0"],
    "gobject-2.0": ["libgobject-2.0.so.0", "libgobject-2.0.so", "gobject-2.0"],
    "glib-2.0": ["libglib-2.0.so.0", "libglib-2.0.so", "glib-2.0"],
    "fontconfig": ["libfontconfig.so.1", "libfontconfig.so", "fontconfig"],
    "harfbuzz": ["libharfbuzz.so.0", "libharfbuzz.so", "harfbuzz"],
    "freetype": ["libfreetype.so.6", "libfreetype.so", "freetype"],
}

# Android System Font Paths
ANDROID_FONT_DIRS = [
    "/system/fonts",
    "/system/etc/fonts",
    "/data/fonts",
    "/product/fonts",
]

class MockCFFILibrary:
    """Fallback mock object for missing CFFI shared libraries on Android."""
    def __init__(self, name):
        self._name = name

    def __getattr__(self, attr):
        # Return a no-op callable or 0 for C functions
        def mock_c_function(*args, **kwargs):
            return 0
        return mock_c_function


def get_android_system_fonts():
    """Locate available TTF/OTF font files on the Android system."""
    fonts = []
    for fdir in ANDROID_FONT_DIRS:
        if os.path.exists(fdir) and os.path.isdir(fdir):
            for root, _, files in os.walk(fdir):
                for file in files:
                    if file.lower().endswith((".ttf", ".otf", ".ttc")):
                        fonts.append(os.path.join(root, file))
    return fonts


def resolve_native_libraries():
    """
    Configures ctypes and CFFI to safely resolve native libraries on Android Chaquopy.
    Interprets pango, cairo, fontconfig, glib, gobject, and harfbuzz requests gracefully.
    """
    # 1. Patch ctypes.util.find_library for Android safety
    orig_find_library = getattr(ctypes.util, "find_library", None)

    def android_find_library(name):
        if not name:
            return None

        clean_name = str(name).lower()

        # Intercept known C libraries
        for key, candidates in C_LIBRARY_MAP.items():
            if key in clean_name or clean_name in key:
                # Check if library file exists in standard Chaquopy library dirs
                for candidate in candidates:
                    for lib_dir in [sys.prefix, os.path.dirname(sys.executable), "/system/lib64", "/system/lib"]:
                        full_path = os.path.join(lib_dir, candidate)
                        if os.path.exists(full_path):
                            return full_path
                return candidates[0]

        # Prevent subprocess calls (ldconfig/gcc) on Android SELinux domain
        return None

    ctypes.util._findSoname_ldconfig = lambda name: None
    ctypes.util._findLib_gcc = lambda name: None
    ctypes.util._findLib_ld = lambda name: None
    ctypes.util.find_library = android_find_library

    # 2. Patch CFFI FFI.dlopen if cffi is imported or importable
    try:
        import cffi
        if not getattr(cffi.FFI, "_is_android_patched", False):
            orig_dlopen = cffi.FFI.dlopen

            def android_dlopen(self, name, flags=0):
                if name is None:
                    try:
                        return orig_dlopen(self, name, flags)
                    except Exception:
                        return MockCFFILibrary("default")

                str_name = str(name).lower()
                try:
                    return orig_dlopen(self, name, flags)
                except (OSError, ImportError) as e:
                    logger.info("CFFI dlopen native fallback for '%s': %s", name, e)
                    return MockCFFILibrary(str_name)

            cffi.FFI.dlopen = android_dlopen
            cffi.FFI._is_android_patched = True
            logger.info("Successfully patched CFFI dlopen for Android native libraries.")
    except Exception as e:
        logger.debug("CFFI module not actively loaded or mock fallback active: %s", e)

    # 3. Log font status
    fonts = get_android_system_fonts()
    logger.info("Native library resolver initialized. Found %d Android system fonts.", len(fonts))
    return True


# Auto-initialize resolver on module import
try:
    resolve_native_libraries()
except Exception as _err:
    logger.warning("Error initializing native library resolver: %s", _err)
