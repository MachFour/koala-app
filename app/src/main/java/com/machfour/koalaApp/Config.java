package com.machfour.koalaApp;

class Config {
    private Config() {}

    // must match provider in AndroidManifest.xml
    static final String FILE_PROVIDER_AUTHORITY = "com.machfour.koalaApp.fileprovider";
    // subdirectory of cache folder where captured images are stored;
    // must match value in file_paths.xml
    static final String IMAGE_CACHE_SUBDIR = "images/";


}
