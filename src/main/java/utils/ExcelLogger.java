package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class ExcelLogger {

    private static final List<LogEntry> ENTRIES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Map<Integer, Map<Integer, Booking>>> WORKER_SCHEDULE =
            Collections.synchronizedMap(new HashMap<>());
    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    private ExcelLogger() {}

    public static void log(String source, String message) {
        ENTRIES.add(new LogEntry(Instant.now(), source, message));
    }

    public static void log(String source, String event, String message) {
        ENTRIES.add(new LogEntry(Instant.now(), source, event + " | " + message));
    }

    /**
     * Фиксируем бронирование слота работника с привязкой к изделию и станку.
     */
    public static void recordWorkerBooking(String workerId, int day, int slot,
                                           String productId, String machineId, String skill) {
        synchronized (WORKER_SCHEDULE) {
            WORKER_SCHEDULE
                    .computeIfAbsent(workerId, k -> new HashMap<>())
                    .computeIfAbsent(slot, k -> new HashMap<>())
                    .put(day, new Booking(machineId, productId, skill));
        }
    }

    /**
     * Сохраняет лог в указанный путь.
     */
    public static void save(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Workbook wb = new XSSFWorkbook()) {
                CellStyle titleStyle = createTitleStyle(wb);
                CellStyle headerStyle = createHeaderStyle(wb);
                CellStyle wrap = createWrapStyle(wb);
                CellStyle slotHeaderStyle = createSlotHeaderStyle(wb);
                CellStyle bookingStyle = createBookingStyle(wb, IndexedColors.LIGHT_YELLOW.getIndex());
                CellStyle bookingAltStyle = createBookingStyle(wb, IndexedColors.LEMON_CHIFFON.getIndex());

                Sheet sheet = wb.createSheet("Log");

                // Title
                Row title = sheet.createRow(0);
                createStyledCell(title, 0, "Журнал событий", titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

                // Header
                Row header = sheet.createRow(1);
                createStyledCell(header, 0, "№", headerStyle);
                createStyledCell(header, 1, "Время", headerStyle);
                createStyledCell(header, 2, "Источник", headerStyle);
                createStyledCell(header, 3, "Сообщение", headerStyle);

                int rowIdx = 2;
                synchronized (ENTRIES) {
                    for (LogEntry e : ENTRIES) {
                        Row row = sheet.createRow(rowIdx);
                        createStyledCell(row, 0, rowIdx - 1, slotHeaderStyle);
                        createStyledCell(row, 1, TS_FORMATTER.format(e.timestamp()), bookingStyle);
                        createStyledCell(row, 2, e.source(), bookingStyle);
                        createStyledCell(row, 3, e.message(), bookingStyle, wrap);
                        rowIdx++;
                    }
                }

                for (int i = 0; i <= 3; i++) {
                    sheet.autoSizeColumn(i);
                }
                sheet.createFreezePane(0, 2);

                addWorkerSheets(wb, titleStyle, headerStyle, slotHeaderStyle, bookingStyle, bookingAltStyle, wrap);

                try (OutputStream out = Files.newOutputStream(path)) {
                    wb.write(out);
                }
                System.out.println("Excel log saved to: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to save Excel log: " + e.getMessage());
        }
    }

    /**
     * Сохранение по умолчанию в files/report.xlsx.
     */
    public static void saveDefault() {
        save(Path.of("files", "report.xlsx"));
    }

    private static void addWorkerSheets(Workbook wb,
                                        CellStyle titleStyle,
                                        CellStyle headerStyle,
                                        CellStyle slotHeaderStyle,
                                        CellStyle bookingStyle,
                                        CellStyle bookingAltStyle,
                                        CellStyle wrap) {
        synchronized (WORKER_SCHEDULE) {
            for (Map.Entry<String, Map<Integer, Map<Integer, Booking>>> workerEntry : WORKER_SCHEDULE.entrySet()) {
                String workerId = workerEntry.getKey();
                Map<Integer, Map<Integer, Booking>> slots = workerEntry.getValue(); // slot -> day -> booking
                Sheet sheet = wb.createSheet(safeSheetName("W_" + workerId));

                int maxSlot = -1;
                int maxDay = -1;
                for (Map.Entry<Integer, Map<Integer, Booking>> slotEntry : slots.entrySet()) {
                    int slot = slotEntry.getKey();
                    maxSlot = Math.max(maxSlot, slot);
                    for (Integer day : slotEntry.getValue().keySet()) {
                        maxDay = Math.max(maxDay, day);
                    }
                }

                Row title = sheet.createRow(0);
                createStyledCell(title, 0, "Расписание работника " + workerId, titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(1, maxDay + 1)));

                Row header = sheet.createRow(1);
                createStyledCell(header, 0, "Слот / День", headerStyle);
                for (int d = 0; d <= maxDay; d++) {
                    createStyledCell(header, d + 1, "День " + d, headerStyle);
                }

                int rowIdx = 2;
                for (int slot = 0; slot <= maxSlot; slot++) {
                    Row row = sheet.createRow(rowIdx);
                    createStyledCell(row, 0, "Слот " + slot, slotHeaderStyle);
                    Map<Integer, Booking> dayMap = slots.getOrDefault(slot, Map.of());
                    for (int d = 0; d <= maxDay; d++) {
                        Booking b = dayMap.get(d);
                        if (b != null) {
                            String value = b.productId + " (M:" + b.machineId + ")";
                            if (b.skill != null && !b.skill.isEmpty()) {
                                value += " [" + b.skill + "]";
                            }
                            CellStyle style = (slot % 2 == 0) ? bookingStyle : bookingAltStyle;
                            createStyledCell(row, d + 1, value, style, wrap);
                        }
                    }
                    rowIdx++;
                }

                for (int c = 0; c <= maxDay + 1; c++) {
                    sheet.autoSizeColumn(c);
                }
                sheet.createFreezePane(1, 2);
            }
        }
    }

    private static String safeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private static CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createSlotHeaderStyle(Workbook wb) {
        CellStyle style = createHeaderStyle(wb);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        return style;
    }

    private static CellStyle createBookingStyle(Workbook wb, short color) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private static CellStyle createWrapStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private static void createStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void createStyledCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void createStyledCell(Row row, int col, String value, CellStyle baseStyle, CellStyle wrapStyle) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        CellStyle combined = row.getSheet().getWorkbook().createCellStyle();
        combined.cloneStyleFrom(baseStyle);
        combined.setWrapText(wrapStyle.getWrapText());
        combined.setVerticalAlignment(wrapStyle.getVerticalAlignment());
        cell.setCellStyle(combined);
    }

    private static final class LogEntry {
        private final Instant timestamp;
        private final String source;
        private final String message;

        private LogEntry(Instant timestamp, String source, String message) {
            this.timestamp = timestamp;
            this.source = source;
            this.message = message;
        }

        public Instant timestamp() {
            return timestamp;
        }

        public String source() {
            return source;
        }

        public String message() {
            return message;
        }
    }

    private static final class Booking {
        private final String machineId;
        private final String productId;
        private final String skill;

        private Booking(String machineId, String productId, String skill) {
            this.machineId = machineId;
            this.productId = productId;
            this.skill = skill;
        }
    }
}
