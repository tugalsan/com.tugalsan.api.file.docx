module com.tugalsan.api.file.docx {
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.ooxml.schemas;
    requires org.apache.poi.scratchpad;
    requires org.apache.commons.io;
    
    requires java.desktop;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.cast;
    requires com.tugalsan.api.file;    
    requires com.tugalsan.api.url;
    requires com.tugalsan.api.list;
    requires com.tugalsan.api.union;
    requires com.tugalsan.api.function;
    
    requires com.tugalsan.api.charset;
    requires com.tugalsan.api.stream;
    requires com.tugalsan.api.string;
    requires com.tugalsan.api.file.img;
    requires com.tugalsan.api.file.common;
    exports com.tugalsan.api.file.docx.server;
}
