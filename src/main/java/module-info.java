module com.tugalsan.api.file.docx {
    requires java.desktop;
    requires poi;
    requires poi.ooxml;
    requires ooxml.schemas;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.compiler;
    requires com.tugalsan.api.unsafe;
    requires com.tugalsan.api.stream;
    requires com.tugalsan.api.string;
    requires com.tugalsan.api.file.img;
    exports com.tugalsan.api.file.docx.server;
}
