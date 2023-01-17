package com.tugalsan.api.file.docx.server;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.awt.*;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import com.tugalsan.api.file.img.server.*;
import com.tugalsan.api.string.server.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.unsafe.client.*;

public class TS_FileDocx implements AutoCloseable {

    final private static TS_Log d = TS_Log.of(TS_FileDocx.class);

    public static Dimension getPageDimension(XWPFDocument doc) {
        var dim = new Dimension();
        var document = doc.getDocument();
        var body = document.getBody();
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        var section = body.getSectPr();
        if (!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        var pageSize = section.getPgSz();
//        d.setSize(DxaUtil.dxa2points(pageSize.getW()), DxaUtil.dxa2points(pageSize.getH()));
        dim.setSize(pageSize.getW().intValue(), pageSize.getH().intValue());
        return dim;
    }

    private boolean landscape = false;
    private int pageSizeAX = 4;

    public int getPageSizeAX() {
        return pageSizeAX;
    }

    public boolean isLandscape() {
        return landscape;
    }

    public void setCurrentPage(boolean landscape, int pageSizeAX) {
        d.ci("setCurrentPage.", landscape, pageSizeAX);
        this.landscape = landscape;
        this.pageSizeAX = pageSizeAX;
        var document = doc.getDocument();
        var body = document.getBody();
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        var section = body.getSectPr();
        if (!section.isSetPgSz()) {
            section.addNewPgSz();
        }
        var pageSize = section.getPgSz();
        pageSize.setOrient(landscape ? STPageOrientation.LANDSCAPE : STPageOrientation.PORTRAIT);
        pageSizeAX = pageSizeAX < 3 ? 3 : pageSizeAX;
        pageSizeAX = pageSizeAX > 7 ? 7 : pageSizeAX;
        switch (pageSizeAX) {
            case 3 -> {
                pageSize.setW(BigInteger.valueOf(landscape ? 23811 : 16838));
                pageSize.setH(BigInteger.valueOf(landscape ? 16838 : 23811));
                d.ci("setCurrentPage->A" + 3);
            }
            case 4 -> {
                pageSize.setW(BigInteger.valueOf(landscape ? 16838 : 11906));
                pageSize.setH(BigInteger.valueOf(landscape ? 11906 : 16838));
                d.ci("setCurrentPage->A" + 4);
            }
            case 5 -> {
                pageSize.setW(BigInteger.valueOf(landscape ? 11906 : 8391));
                pageSize.setH(BigInteger.valueOf(landscape ? 8391 : 11906));
                d.ci("setCurrentPage->A" + 5);
            }
            case 6 -> {
                pageSize.setW(BigInteger.valueOf(landscape ? 8392 : 5954));
                pageSize.setH(BigInteger.valueOf(landscape ? 5954 : 8392));
                d.ci("setCurrentPage->A" + 6);
            }
            case 7 -> {
                pageSize.setW(BigInteger.valueOf(landscape ? 5936 : 4196));
                pageSize.setH(BigInteger.valueOf(landscape ? 4196 : 5936));
                d.ci("setCurrentPage->A" + 7);
            }
            default -> {
            }
        }
    }

    private XWPFDocument doc = null;
    private Path filePath;

    public XWPFDocument getDoc() {
        return doc;
    }

    public TS_FileDocx(Path filePath) {
        TGS_UnSafe.execute(() -> {
            this.filePath = filePath;
            doc = new XWPFDocument();
        });
    }

    public Path getFile() {
        return filePath;
    }

    @Override
    public void close() {
        TGS_UnSafe.execute(() -> {
            d.ci("close.");
            doc.createParagraph();
            try ( var fileOut = Files.newOutputStream(filePath)) {
                doc.write(fileOut);
                fileOut.close();
            }
        });
    }

    public boolean addImage(XWPFParagraph p, CharSequence imgFile) {
        d.ci("addImage", "p", p, "imgFile", imgFile);
        return addImage(p, imgFile, 200, 200);
    }

    public boolean addImage(XWPFParagraph p, CharSequence imgFile, int width, int height) {
        return TGS_UnSafe.compile(() -> {
            var imgFileStr = imgFile.toString();
            d.ci("addImage", "p", p, "imgFile", imgFileStr, "width", width, "height", height);
            if (p == null) {
                d.ce("addImage.ERROR: TK_DOCXFile.addImage.p == null");
                return false;
            }
            if (d.infoEnable) {
                var bi = TS_FileImageUtils.readImageFromFile(Path.of(imgFileStr), true);
                d.ci("addImage", "imgWidth", bi.getWidth(), "imgHeight", bi.getHeight(), "inputWidth", width, "inputheight", height);
            }

            Integer format = null;
            if (imgFileStr.endsWith(".emf")) {
                format = XWPFDocument.PICTURE_TYPE_EMF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".wmf") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".wmf")) {
                format = XWPFDocument.PICTURE_TYPE_WMF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".pict") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".pict")) {
                format = XWPFDocument.PICTURE_TYPE_PICT;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".pıct") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".pıct")) {
                format = XWPFDocument.PICTURE_TYPE_PICT;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".jpeg") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
                format = XWPFDocument.PICTURE_TYPE_JPEG;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".jpg") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".jpg")) {
                format = XWPFDocument.PICTURE_TYPE_JPEG;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".png") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".png")) {
                format = XWPFDocument.PICTURE_TYPE_PNG;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".dib") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".dib")) {
                format = XWPFDocument.PICTURE_TYPE_DIB;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".dıb") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".dıb")) {
                format = XWPFDocument.PICTURE_TYPE_DIB;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".gif") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".gif")) {
                format = XWPFDocument.PICTURE_TYPE_GIF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".gıf") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".gıf")) {
                format = XWPFDocument.PICTURE_TYPE_GIF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tiff") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tiff")) {
                format = XWPFDocument.PICTURE_TYPE_TIFF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tıff") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tıff")) {
                format = XWPFDocument.PICTURE_TYPE_TIFF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tif") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tif")) {
                format = XWPFDocument.PICTURE_TYPE_TIFF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tıf") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".tıf")) {
                format = XWPFDocument.PICTURE_TYPE_TIFF;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".eps") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".eps")) {
                format = XWPFDocument.PICTURE_TYPE_EPS;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".bmp") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".bmp")) {
                format = XWPFDocument.PICTURE_TYPE_BMP;
            } else if (imgFileStr.toLowerCase(Locale.ROOT).endsWith(".wpg") || imgFileStr.toLowerCase(Locale.ROOT).endsWith(".wpg")) {
                format = XWPFDocument.PICTURE_TYPE_WPG;
            }
            if (format == null) {
                d.ce("addImage.Unsupported picture: " + imgFileStr + ". Expected emf|wmf|pict|jpeg|png|dib|gif|tiff|eps|bmp|wpg");
                return false;
            } else {
//        r.setText(imgFile);
//        r.addBreak();
                d.ci("addImage.INFO: TK_DOCXFile.run.addPicture.BEGIN...");
                try ( var is = new FileInputStream(imgFileStr)) {
                    var r = p.createRun();
                    r.addPicture(is, format, imgFileStr, Units.toEMU(width), Units.toEMU(height)); // 200x200 pixels
                }
                d.ci("addImage.INFO: TK_DOCXFile.run.addPicture.END");
                return true;
            }
        });
    }

    public String mergeCell_bySpan(XWPFTable table, int rowIdx, int colIdx, int rowSpan, int colSpan, int[] widthsPercent) {
        d.ci("mergeCell_bySpan table:" + table + ", RI:" + rowIdx + ", CI: " + colIdx + ", RSP: " + rowSpan + ", CSP:" + colSpan);
        if (rowSpan < 1) {
            return "ERROR: mergeCell_bySpan.rowSpan:" + rowSpan + " < 1";
        }
        if (colSpan < 1) {
            return "ERROR: mergeCell_bySpan.colSpan:" + colSpan + " < 1";
        }
        return mergeCell_byIndex(table, rowIdx, rowIdx + rowSpan - 1, colIdx, colIdx + colSpan - 1, widthsPercent);
    }

    private String mergeCell_byIndex(XWPFTable table, int rowIdxFrom, int rowIdxTo, int colIdxFrom, int colIdxTo, int[] widthsPercent) {
        return TGS_UnSafe.compile(() -> {
            d.ci("mergeCell_byIndex -> RF:" + rowIdxFrom + ", RT:" + rowIdxTo + ", CF:" + colIdxFrom + ", CT:" + colIdxTo);
            if (rowIdxTo < rowIdxFrom) {
                return "ERROR: mergeCell_byIndex.rowIdxTo:" + rowIdxTo + " < rowIdxFrom:" + rowIdxFrom;
            }
            if (colIdxTo < colIdxFrom) {
                return "ERROR: mergeCell_byIndex.colIdxTo:" + colIdxTo + " < colIdxFrom:" + colIdxFrom;
            }
            while (table.getRows().size() <= rowIdxTo) {
                d.ci("mergeCell_byIndex.incRow -> rs: " + table.getRows().size());
                addTableRow(table, widthsPercent.length);
            }
            if (rowIdxFrom != rowIdxTo) {
                mergeTableCells_Rows(table, rowIdxFrom, rowIdxTo, colIdxFrom);
            }
            if (colIdxFrom != colIdxTo) {
                TGS_StreamUtils.reverse(rowIdxFrom, rowIdxTo + 1).forEach(ri -> {
                    mergeTableCells_Cols(table, ri, colIdxFrom, colIdxTo);
                    var newColumnWidth = IntStream.range(colIdxFrom, colIdxTo + 1).map(ci -> widthsPercent[ci]).sum();
                    setTableColWidth(table, ri, colIdxFrom, newColumnWidth);
                });
            }
            return null;
        }, e -> {
            d.ce("mergeCell_byIndex.ERROR.e:" + e.getMessage());
            e.printStackTrace();
            return null;
        });
    }

    private boolean mergeTableCells_Rows(XWPFTable table, int rowIdxFrom, int rowIdxTo, int colIdx) {
        return TGS_UnSafe.compile(() -> {
            d.ci("mergeTableCells_Rows -> RF:" + rowIdxFrom + ", RT:" + rowIdxTo + ", CI:" + colIdx);
            for (var rowIndex = rowIdxFrom; rowIndex <= rowIdxTo; rowIndex++) {
                var cell = table.getRow(rowIndex).getCell(colIdx);
                var vmerge = CTVMerge.Factory.newInstance();
                if (rowIndex == rowIdxFrom) {
                    // The first merged cell is set with RESTART merge value
                    vmerge.setVal(STMerge.RESTART);
                } else {
                    // Cells which join (merge) the first one, are set with CONTINUE
                    vmerge.setVal(STMerge.CONTINUE);
                    // and the content should be removed
                    TGS_StreamUtils.reverse(0, cell.getParagraphs().size()).forEach(i -> {
                        cell.removeParagraph(0);
                    });
                    cell.addParagraph();
                }
                // Try getting the TcPr. Not simply setting an new one every time.
                var tcPr = cell.getCTTc().getTcPr();
                if (tcPr == null) {
                    tcPr = cell.getCTTc().addNewTcPr();
                }
                tcPr.setVMerge(vmerge);
            }
            return true;
        }, e -> {
            d.ce("mergeTableCells_Rows.ERROR.e:" + e.getMessage());
            e.printStackTrace();
            return false;
        });
    }

    //merging horizontally by setting grid span instead of using CTHMerge
    private boolean mergeTableCells_Cols(XWPFTable table, int rowIdx, int colIdxFrom, int colIdxTo) {
        return TGS_UnSafe.compile(() -> {
            d.ci("mergeTableCells_Cols -> RI:" + rowIdx + ", CF:" + colIdxFrom + ", CT:" + colIdxTo);
            var cell = table.getRow(rowIdx).getCell(colIdxFrom);
            // Try getting the TcPr. Not simply setting an new one every time.
            var tcPr = cell.getCTTc().getTcPr();
            if (tcPr == null) {
                tcPr = cell.getCTTc().addNewTcPr();
            }
            // The first merged cell has grid span property set
            if (tcPr.isSetGridSpan()) {
                tcPr.getGridSpan().setVal(BigInteger.valueOf(colIdxTo - colIdxFrom + 1));
            } else {
                tcPr.addNewGridSpan().setVal(BigInteger.valueOf(colIdxTo - colIdxFrom + 1));
            }
            // Cells which join (merge) the first one, must be removed
            for (var colIndex = colIdxTo; colIndex > colIdxFrom; colIndex--) {
                table.getRow(rowIdx).getCtRow().removeTc(colIndex);
                table.getRow(rowIdx).removeCell(colIndex);
            }
            return true;
        }, e -> {
            d.ce("mergeTableCells_Cols.ERROR.e:" + e.getMessage());
            e.printStackTrace();
            return false;
        });
    }

    public static double TABLE_WITH_FACTOR_A3_PORT() {
        return 0.0975d;
    }

    public static double TABLE_WITH_FACTOR_A4_PORT() {
        return 0.0633d;
    }

    public static double TABLE_WITH_FACTOR_A5_PORT() {
        return 0.0389d;
    }

    public static double TABLE_WITH_FACTOR_A6_PORT() {
        return 0.0220d;
    }

    public static double TABLE_WITH_FACTOR_A7_PORT() {
        return 0.0098d;
    }

    public static double TABLE_WITH_FACTOR_A3_LAND() {
        return 0.1459d;
    }

    public static double TABLE_WITH_FACTOR_A4_LAND() {
        return 0.0975d;
    }

    public static double TABLE_WITH_FACTOR_A5_LAND() {
        return 0.0633d;
    }

    public static double TABLE_WITH_FACTOR_A6_LAND() {
        return 0.0389d;
    }

    public static double TABLE_WITH_FACTOR_A7_LAND() {
        return 0.0219d;
    }

    private double getTableWidthFactor() {
        return switch (pageSizeAX) {
            case 7 ->
                landscape ? TABLE_WITH_FACTOR_A7_LAND() : TABLE_WITH_FACTOR_A7_PORT();
            case 6 ->
                landscape ? TABLE_WITH_FACTOR_A6_LAND() : TABLE_WITH_FACTOR_A6_PORT();
            case 5 ->
                landscape ? TABLE_WITH_FACTOR_A5_LAND() : TABLE_WITH_FACTOR_A5_PORT();
            case 3 ->
                landscape ? TABLE_WITH_FACTOR_A3_LAND() : TABLE_WITH_FACTOR_A3_PORT();
            default ->
                landscape ? TABLE_WITH_FACTOR_A4_LAND() : TABLE_WITH_FACTOR_A4_PORT();
        }; //4
    }

    public void setTableColWidth(XWPFTable table, int rowIdx, int colIdx, int widthPercent) {
        d.ci("setTableColWidth", "table", table, "rowIdx", rowIdx, "colIdx", colIdx, "widthPercent", widthPercent);
        var factor = (int) Math.floor(widthPercent * 1440 * getTableWidthFactor());
        var tblWidth = CTTblWidth.Factory.newInstance();
        tblWidth.setW(BigInteger.valueOf(factor));
        tblWidth.setType(STTblWidth.DXA);
        var tcPr = table.getRow(rowIdx).getCell(colIdx).getCTTc().getTcPr();
        if (tcPr != null) {
            tcPr.setTcW(tblWidth);
        } else {
            tcPr = CTTcPr.Factory.newInstance();
            tcPr.setTcW(tblWidth);
            table.getRow(rowIdx).getCell(colIdx).getCTTc().setTcPr(tcPr);
        }
    }

    public void setTableColWidths(XWPFTable table, int[] widthsPercent) {
        var rowSize = table.getRows().size();
        var colSize = widthsPercent.length;
        var factors = new int[colSize];
        IntStream.range(0, colSize).parallel().forEach(ci -> factors[ci] = (int) Math.floor(widthsPercent[ci] * 1440 * getTableWidthFactor()));
        IntStream.range(0, colSize).forEachOrdered(ci -> {
            if (ci == 0) {
                table.getCTTbl().addNewTblGrid().addNewGridCol().setW(BigInteger.valueOf(factors[ci]));
            } else {
                table.getCTTbl().getTblGrid().addNewGridCol().setW(BigInteger.valueOf(factors[ci]));
            }
        });
        IntStream.range(0, colSize).forEachOrdered(ci -> {//MAY WORK WITH PARALLEL
            var tblWidth = CTTblWidth.Factory.newInstance();
            tblWidth.setW(BigInteger.valueOf(factors[ci]));
            tblWidth.setType(STTblWidth.DXA);
            IntStream.range(0, rowSize).forEachOrdered(ri -> {//MAY WORK WITH PARALLEL
                var tcPr = table.getRow(ri).getCell(ci).getCTTc().getTcPr();
                if (tcPr == null) {
                    tcPr = CTTcPr.Factory.newInstance();
                    tcPr.setTcW(tblWidth);
                    table.getRow(ri).getCell(ci).getCTTc().setTcPr(tcPr);
                } else {
                    tcPr.setTcW(tblWidth);
                }
            });
        });
    }

    public XWPFTable createTable(int rowSize, int colSize) {
        d.ci("createTable", "rowSize", rowSize, "colSize", colSize);
        if (tablefix) {
            if (!lastItemIsCreateParagraph) {//TABLE WIDTH FIX
                var p = doc.createParagraph();
                addText(p, " ", false, false, false, 3, "000000");
            }
        }
        lastItemIsCreateParagraph = false;
        var table = doc.createTable(rowSize, colSize);//SET INIT VALUES
        IntStream.range(0, rowSize).parallel().forEach(ri -> IntStream.range(0, colSize).parallel().forEach(ci -> table.getRow(ri).getCell(ci).setText("")));
        return table;
    }
    private boolean tablefix = false;

    public static XWPFTableRow addTableRow(XWPFTable table, int colSize) {
        var newRow = table.createRow();
        IntStream.range(0, colSize).parallel().forEach(ci -> newRow.getCell(ci).setText(""));
        return newRow;
    }

    private void styleParagraph(XWPFParagraph p, int allign_center1_right2_leftDefault, boolean isBordered) {
        d.ci("styleParagraph", "p", p, "allign_center1_right2_leftDefault", allign_center1_right2_leftDefault, "isBordered", isBordered);
        switch (allign_center1_right2_leftDefault) {
            case 1 ->
                p.setAlignment(ParagraphAlignment.CENTER);
            case 2 ->
                p.setAlignment(ParagraphAlignment.RIGHT);
            default -> //0
                p.setAlignment(ParagraphAlignment.LEFT);
        }
        if (isBordered) {
            p.setBorderBottom(Borders.SINGLE);
            p.setBorderTop(Borders.SINGLE);
            p.setBorderRight(Borders.SINGLE);
            p.setBorderLeft(Borders.SINGLE);
        }
        p.setVerticalAlignment(TextAlignment.CENTER);
    }

    public XWPFParagraph createParagraph() {
        return createParagraph(0, false);
    }
    boolean lastItemIsCreateParagraph = false;

    public XWPFParagraph createParagraph(int allign_center1_right2_leftDefault, boolean isBordered) {
        d.ci("createParagraph", "allign_center1_right2_leftDefault", allign_center1_right2_leftDefault, "isBordered", isBordered);
        var p = doc.createParagraph();
        p.setWordWrapped(true);
        styleParagraph(p, allign_center1_right2_leftDefault, isBordered);
        lastItemIsCreateParagraph = true;
        return p;
    }

    public XWPFParagraph getParagraph(XWPFTable table, int r, int c, int allign_center1_right2_leftDefault) {
        d.ci("getParagraph", "r", r, "c", c, "allign_center1_right2_leftDefault", allign_center1_right2_leftDefault);
        return getParagraph(table.getRow(r).getCell(c), allign_center1_right2_leftDefault);
    }

    public XWPFParagraph getParagraph(XWPFTableCell cell, int allign_center1_right2_leftDefault) {
        d.ci("getParagraph", "cell", cell, "allign_center1_right2_leftDefault", allign_center1_right2_leftDefault);
        var p = cell.getParagraphs().get(0);
        styleParagraph(p, allign_center1_right2_leftDefault, false);
        p.setWordWrapped(true);
        return p;
    }

    public void addText(XWPFParagraph p, CharSequence text, boolean isBold, boolean isItalic, boolean isUnderlined, int fontSize, CharSequence fontColor) {
        d.ci("addText", "p", p, "text", text);
        var tokens = TS_StringUtils.toList(text, "\n");
        var tokenCount = 0;
        for (String t : tokens) {
            tokenCount++;
            if (t.isEmpty() || tokenCount > 1) {
                addNewLine(p);
            }
            if (!t.isEmpty()) {
                var fontedRun = createFontedRun(p, isBold, isItalic, isUnderlined, fontSize, fontColor);
                fontedRun.setText(t);
            }
        }
    }

    public void addNewLine(XWPFParagraph p) {
        d.ci("addNewLine", "XWPFParagraph", p);
        p.createRun().addBreak();
    }

    public void insertPage() {
        d.ci("insertPage");
        if (firstPageTriggered) {
            var r = createParagraph().createRun();
            r.addCarriageReturn();                 //separate previous text from break
            r.addBreak(BreakType.PAGE);
            r.addBreak(BreakType.TEXT_WRAPPING);   //cancels effect of page break
        } else {
            firstPageTriggered = true;
        }
    }
    private boolean firstPageTriggered = false;

    private XWPFRun createFontedRun(XWPFParagraph p, boolean isBold, boolean isItalic, boolean isUnderlined, int fontSize, CharSequence fontColor) {
        d.ci("createFontedRun", "p", p, "isBold", isBold, "isItalic", isItalic, "isUnderlined", isUnderlined, "fontSize", fontSize, "fontColor", fontColor);
        var r = p.createRun();
        r.setFontSize(fontSize);
        r.setFontFamily("Arial");
        r.setBold(isBold);
        r.setItalic(isItalic);
        if (isUnderlined) {
            r.setUnderline(UnderlinePatterns.DOT_DOT_DASH);
        }
        r.setColor(fontColor.toString());
        return r;
    }

    public String COLOR_RED = "FF0000";
    public String COLOR_GREEN = "00FF00";
    public String COLOR_BLUE = "0000FF";
    public String COLOR_WHITE = "FFFFFF";
    public String COLOR_BLACK = "000000";
    public String COLOR_GRAYLIGHT = "222222";
    public String COLOR_GRAY = "777777";
    public String COLOR_GRAYDARK = "AAAAAA";
}
