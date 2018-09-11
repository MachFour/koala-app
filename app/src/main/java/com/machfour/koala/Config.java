package com.machfour.koala;

class Config {
    private Config() {}

    // must match provider in AndroidManifest.xml
    static final String FILE_PROVIDER_AUTHORITY = "com.machfour.koala.fileprovider";
    // subdirectory of cache folder where captured images are stored;
    // must match value in file_paths.xml
    static final String IMAGE_CACHE_SUBDIR = "images/";


}
