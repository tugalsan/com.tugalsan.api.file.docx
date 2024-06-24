module com.tugalsan.api.file.docx {
    requires java.desktop;
    requires poi;
    requires poi.ooxml;
    requires ooxml.schemas;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.cast;
    requires com.tugalsan.api.file;
    
    requires com.tugalsan.api.url;
    requires com.tugalsan.api.list;
    requires com.tugalsan.api.union;
    requires com.tugalsan.api.callable;
    requires com.tugalsan.api.unsafe;
    requires com.tugalsan.api.charset;
    requires com.tugalsan.api.stream;
    requires com.tugalsan.api.string;
    requires com.tugalsan.api.file.img;
    requires com.tugalsan.api.file.common;
    exports com.tugalsan.api.file.docx.server;
}
