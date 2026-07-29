package co.gov.sfc.balancesafppatrimonios.service;

import co.gov.sfc.balancesafppatrimonios.config.BalancesAfpPatrimoniosProperties;
import co.gov.sfc.balancesafppatrimonios.model.BalanceDiario;
import co.gov.sfc.balancesafppatrimonios.model.CuentaPuc;
import co.gov.sfc.balancesafppatrimonios.model.EntidadReporte;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ExcelVoluntariasService {

    private static final Locale ES_CO = Locale.of("es", "CO");

    private final BalancesAfpPatrimoniosProperties properties;

    public ExcelVoluntariasService(
            BalancesAfpPatrimoniosProperties properties) {
        this.properties = properties;
    }

    public GeneratedReport generar(
            LocalDate fechaCorte,
            Path rutaBaseSalida,
            List<EntidadReporte> entidades,
            List<CuentaPuc> cuentas,
            List<BalanceDiario> datos) {

        if (entidades.isEmpty()) {
            throw new IllegalStateException(
                    "No se encontraron entidades vigentes "
                            + "para el tipo de entidad configurado."
            );
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Styles styles = new Styles(workbook);

            Map<String, Map<DailyKey, BigDecimal>> datosPorClaveReporte =
                    datos.stream()
                            .collect(
                                    Collectors.groupingBy(
                                            BalanceDiario::claveReporte,
                                            Collectors.toMap(
                                                    dato -> new DailyKey(
                                                            dato.codigoPuc(),
                                                            dato.fecha().getDayOfMonth()
                                                    ),
                                                    BalanceDiario::valorMiles,
                                                    BigDecimal::add,
                                                    LinkedHashMap::new
                                            )
                                    )
                            );

            /*
             * Se crean primero las hojas de las entidades para conocer
             * los nombres definitivos de las hojas. Esos nombres se usan
             * después en las fórmulas de la hoja consolidado.
             */
            Map<String, String> hojaPorClaveReporte =
                    new LinkedHashMap<>();

            for (EntidadReporte entidad : entidades) {
                String nombreHoja = crearHojaEntidad(
                        workbook,
                        styles,
                        fechaCorte,
                        entidad,
                        cuentas,
                        datosPorClaveReporte.getOrDefault(
                                entidad.claveReporte(),
                                Map.of()
                        )
                );

                hojaPorClaveReporte.put(
                        entidad.claveReporte(),
                        nombreHoja
                );
            }

            crearHojaConsolidado(
                    workbook,
                    styles,
                    fechaCorte,
                    entidades,
                    cuentas,
                    hojaPorClaveReporte
            );

            /*
             * consolidado se presenta como primera hoja.
             */
            workbook.setSheetOrder("consolidado", 0);
            workbook.setActiveSheet(0);

            /*
             * Excel recalculará automáticamente las fórmulas al abrir
             * el archivo y cada vez que cambie el día seleccionado.
             */
            workbook.setForceFormulaRecalculation(true);

            if (workbook instanceof XSSFWorkbook xssfWorkbook) {
                var calcPr = xssfWorkbook.getCTWorkbook().isSetCalcPr()
                        ? xssfWorkbook.getCTWorkbook().getCalcPr()
                        : xssfWorkbook.getCTWorkbook().addNewCalcPr();

                calcPr.setCalcMode(
                        org.openxmlformats.schemas.spreadsheetml.x2006.main.STCalcMode.AUTO
                );
                calcPr.setFullCalcOnLoad(true);
                calcPr.setForceFullCalc(true);
            }

            workbook.write(output);

            byte[] content = output.toByteArray();
            String fileName = nombreArchivo(fechaCorte);

            Path archivoGuardado = null;

            if (properties.isGuardarCopiaLocal()) {
                archivoGuardado = guardarCopia(
                        rutaBaseSalida,
                        fechaCorte,
                        fileName,
                        content
                );
            }

            return new GeneratedReport(
                    fileName,
                    content,
                    archivoGuardado
            );

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No fue posible generar el archivo Excel.",
                    ex
            );
        }
    }

    private void crearHojaConsolidado(
            Workbook workbook,
            Styles styles,
            LocalDate fechaCorte,
            List<EntidadReporte> entidades,
            List<CuentaPuc> cuentas,
            Map<String, String> hojaPorClaveReporte) {

        Sheet sheet = workbook.createSheet("consolidado");
        sheet.createFreezePane(2, 12);

        int ultimaColumna =
                Math.max(5, entidades.size() + 2);

        Row row0 = sheet.createRow(0);
        Cell title = row0.createCell(0);
        title.setCellValue("Sistema de Información Financiera");
        title.setCellStyle(styles.title);
        sheet.addMergedRegion(
                new CellRangeAddress(
                        0,
                        0,
                        0,
                        ultimaColumna
                )
        );

        Row row1 = sheet.createRow(1);
        Cell subtitle = row1.createCell(0);
        subtitle.setCellValue(
                "Valores reportados de cuentas PUC "
                        + "de patrimonios autónomos"
        );
        subtitle.setCellStyle(styles.subtitle);
        sheet.addMergedRegion(
                new CellRangeAddress(
                        1,
                        1,
                        0,
                        ultimaColumna
                )
        );

        addLabelValue(
                sheet,
                styles,
                3,
                "Tipo Entidad",
                properties.getTipoEntidadVoluntarias()
                        + " - entidades con Estado = "
                        + properties.getEstadoEntidadVigente()
        );

        addLabelValue(
                sheet,
                styles,
                4,
                "Tipo Informe",
                properties.getTipoInformeVoluntarias().toString()
        );

        addLabelValue(
                sheet,
                styles,
                5,
                "Selección",
                "Códigos de patrimonio definidos por entidad"
        );

        addLabelValue(
                sheet,
                styles,
                6,
                "Producto",
                "Voluntarias"
        );

        addLabelValue(
                sheet,
                styles,
                7,
                "Tipo de Informe",
                properties.getTipoInforme().toString()
        );

        /*
         * Fila 9 de Excel:
         * A9/B9 muestran la fecha de corte.
         * C9:D9 contienen la instrucción.
         * E9 es el selector desplegable del día.
         */
        Row selectorRow = sheet.createRow(8);
        selectorRow.setHeightInPoints(27);

        Cell fechaLabel = selectorRow.createCell(0);
        fechaLabel.setCellValue("Fecha de corte:");
        fechaLabel.setCellStyle(styles.label);

        Cell fechaValue = selectorRow.createCell(1);
        fechaValue.setCellValue(
                fechaCorte.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
        );

        sheet.addMergedRegion(
                new CellRangeAddress(8, 8, 2, 3)
        );

        Cell selectorLabel = selectorRow.createCell(2);
        selectorLabel.setCellValue(
                "Seleccione el día para totalizar"
        );
        selectorLabel.setCellStyle(styles.daySelectorLabel);

        Cell selector = selectorRow.createCell(4);

        /*
         * El valor inicial es el último día consultado.
         * Cuando la fecha de corte es fin de mes, coincide con
         * el último día del mes.
         */
        selector.setCellValue(fechaCorte.getDayOfMonth());
        selector.setCellStyle(styles.daySelector);

        agregarValidacionDia(
                sheet,
                fechaCorte.lengthOfMonth()
        );

        /*
         * Instrucción adicional visible al pasar el cursor.
         */
        CreationHelper helper =
                workbook.getCreationHelper();

        Drawing<?> drawing =
                sheet.createDrawingPatriarch();

        ClientAnchor anchor =
                helper.createClientAnchor();

        anchor.setCol1(4);
        anchor.setCol2(7);
        anchor.setRow1(8);
        anchor.setRow2(11);

        Comment comment =
                drawing.createCellComment(anchor);

        comment.setString(
                helper.createRichTextString(
                        "Use la lista desplegable y seleccione un día entre 1 y "
                                + fechaCorte.lengthOfMonth()
                                + ". La hoja se recalcula automáticamente."
                )
        );

        comment.setAuthor("BalancesAFPPatrimonios");
        selector.setCellComment(comment);

        Row unitRow = sheet.createRow(9);
        sheet.addMergedRegion(
                new CellRangeAddress(9, 9, 2, 4)
        );

        Cell unit = unitRow.createCell(2);
        unit.setCellValue("Cifras en miles de pesos");
        unit.setCellStyle(styles.unitsCentered);

        Row header = sheet.createRow(11);

        Cell codigoPucHeader = header.createCell(0);
        codigoPucHeader.setCellValue("CÓDIGO PUC");
        codigoPucHeader.setCellStyle(styles.header);

        Cell nombreCuentaHeader = header.createCell(1);
        nombreCuentaHeader.setCellValue(
                "NOMBRE DE LA CUENTA"
        );
        nombreCuentaHeader.setCellStyle(styles.header);

        int column = 2;

        for (EntidadReporte entidad : entidades) {
            Cell cell = header.createCell(column++);
            cell.setCellValue(nombreColumna(entidad));
            cell.setCellStyle(styles.header);
        }

        Cell systemHeader = header.createCell(column);
        systemHeader.setCellValue("SISTEMA");
        systemHeader.setCellStyle(styles.header);

        /*
         * En cada hoja de entidad:
         * - fila 5 de Excel: encabezados;
         * - fila 6 de Excel: primera cuenta.
         *
         * La versión anterior usaba 13, que corresponde a la primera
         * fila de datos de consolidado. Por eso MATCH no encontraba las
         * cuentas y IFERROR devolvía 0.
         */
        int primeraFilaDatosEntidadExcel = 6;
        int ultimaFilaDatosEntidadExcel =
                primeraFilaDatosEntidadExcel + cuentas.size() - 1;

        int rowIndex = 12;

        for (CuentaPuc cuenta : cuentas) {
            Row row = sheet.createRow(rowIndex);

            row.createCell(0)
                    .setCellValue(cuenta.codigoPuc());

            row.createCell(1)
                    .setCellValue(cuenta.nombreCuenta());

            int dataColumn = 2;

            for (EntidadReporte entidad : entidades) {
                String nombreHoja =
                        hojaPorClaveReporte.get(
                                entidad.claveReporte()
                        );

                Cell cell = row.createCell(dataColumn);

                String formula =
                        crearFormulaValorDia(
                                nombreHoja,
                                rowIndex + 1,
                                primeraFilaDatosEntidadExcel,
                                ultimaFilaDatosEntidadExcel
                        );

                cell.setCellFormula(formula);
                cell.setCellStyle(styles.number);
                dataColumn++;
            }

            Cell total = row.createCell(dataColumn);

            String primeraColumnaEntidad =
                    CellReference.convertNumToColString(2);

            String ultimaColumnaEntidad =
                    CellReference.convertNumToColString(
                            dataColumn - 1
                    );

            total.setCellFormula(
                    "SUM("
                            + primeraColumnaEntidad
                            + (rowIndex + 1)
                            + ":"
                            + ultimaColumnaEntidad
                            + (rowIndex + 1)
                            + ")"
            );

            total.setCellStyle(styles.totalNumber);
            rowIndex++;
        }

        /*
         * Los filtros quedan únicamente en:
         * A: código PUC
         * B: nombre de la cuenta
         */
        sheet.setAutoFilter(
                new CellRangeAddress(
                        11,
                        rowIndex - 1,
                        0,
                        1
                )
        );

        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 42 * 256);
        sheet.setColumnWidth(2, 22 * 256);
        sheet.setColumnWidth(3, 22 * 256);
        sheet.setColumnWidth(4, 12 * 256);

        for (int i = 2;
             i <= entidades.size() + 2;
             i++) {

            sheet.setColumnWidth(i, 22 * 256);
        }
    }

    private String crearHojaEntidad(
            Workbook workbook,
            Styles styles,
            LocalDate fechaCorte,
            EntidadReporte entidad,
            List<CuentaPuc> cuentas,
            Map<DailyKey, BigDecimal> daily) {

        String nombreHoja =
                nombreHojaUnico(
                        workbook,
                        entidad.nombreHoja()
                );

        Sheet sheet =
                workbook.createSheet(nombreHoja);

        /*
         * Se inmovilizan las columnas A, B y C.
         */
        sheet.createFreezePane(3, 5);

        Row titleRow = sheet.createRow(0);

        Cell title = titleRow.createCell(0);
        title.setCellValue(
                "Balances diarios - "
                        + entidad.nombreEntidad()
        );
        title.setCellStyle(styles.title);

        /*
         * A, B, C y D1...D31 = columnas 0 a 33.
         */
        sheet.addMergedRegion(
                new CellRangeAddress(0, 0, 0, 33)
        );

        Row periodRow = sheet.createRow(1);
        periodRow.createCell(0)
                .setCellValue("Periodo:");

        periodRow.createCell(1)
                .setCellValue(
                        fechaCorte.getMonth()
                                .getDisplayName(
                                        TextStyle.FULL,
                                        ES_CO
                                )
                                + " de "
                                + fechaCorte.getYear()
                );

        Row ruleRow = sheet.createRow(2);
        ruleRow.createCell(0)
                .setCellValue("Clasificación:");

        ruleRow.createCell(1)
                .setCellValue(
                        "Voluntarias: códigos de patrimonio definidos por entidad"
                );

        Row unitsRow = sheet.createRow(3);
        unitsRow.createCell(0)
                .setCellValue("Unidad:");

        unitsRow.createCell(1)
                .setCellValue("Miles de pesos");

        Row header = sheet.createRow(4);

        String[] fixedHeaders = {
                "CÓDIGO ENTIDAD",
                "CÓDIGO PUC",
                "NOMBRE DE LA CUENTA"
        };

        for (int i = 0;
             i < fixedHeaders.length;
             i++) {

            Cell cell = header.createCell(i);
            cell.setCellValue(fixedHeaders[i]);
            cell.setCellStyle(styles.header);
        }

        /*
         * D1 comienza en la columna D, índice 3.
         */
        for (int day = 1;
             day <= 31;
             day++) {

            Cell cell =
                    header.createCell(day + 2);

            cell.setCellValue("D" + day);
            cell.setCellStyle(styles.header);
        }

        int rowIndex = 5;

        for (CuentaPuc cuenta : cuentas) {
            Row row = sheet.createRow(rowIndex++);

            row.createCell(0)
                    .setCellValue(
                            entidad.codigoEntidad()
                    );

            row.createCell(1)
                    .setCellValue(
                            cuenta.codigoPuc()
                    );

            row.createCell(2)
                    .setCellValue(
                            cuenta.nombreCuenta()
                    );

            for (int day = 1;
                 day <= 31;
                 day++) {

                BigDecimal value =
                        BigDecimal.ZERO;

                if (day <= fechaCorte.lengthOfMonth()
                        && day
                        <= fechaCorte.getDayOfMonth()) {

                    value = daily.getOrDefault(
                            new DailyKey(
                                    cuenta.codigoPuc(),
                                    day
                            ),
                            BigDecimal.ZERO
                    );
                }

                Cell cell =
                        row.createCell(day + 2);

                cell.setCellValue(
                        value.doubleValue()
                );

                cell.setCellStyle(styles.number);
            }
        }

        /*
         * Los filtros quedan únicamente en:
         * B: código PUC
         * C: nombre de la cuenta
         */
        sheet.setAutoFilter(
                new CellRangeAddress(
                        4,
                        rowIndex - 1,
                        1,
                        2
                )
        );

        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 44 * 256);

        /*
         * D1...D31 con ancho suficiente para evitar ####.
         */
        for (int column = 3;
             column <= 33;
             column++) {

            sheet.setColumnWidth(
                    column,
                    22 * 256
            );
        }

        return nombreHoja;
    }

    private void agregarValidacionDia(
            Sheet sheet,
            int ultimoDiaMes) {

        String[] dias =
                IntStream.rangeClosed(
                                1,
                                ultimoDiaMes
                        )
                        .mapToObj(
                                Integer::toString
                        )
                        .toArray(String[]::new);

        DataValidationHelper helper =
                sheet.getDataValidationHelper();

        DataValidationConstraint constraint =
                helper.createExplicitListConstraint(dias);

        CellRangeAddressList range =
                new CellRangeAddressList(
                        8,
                        8,
                        4,
                        4
                );

        DataValidation validation =
                helper.createValidation(
                        constraint,
                        range
                );

        validation.setShowErrorBox(true);
        validation.createErrorBox(
                "Día inválido",
                "Use la lista desplegable y seleccione un día entre 1 y "
                        + ultimoDiaMes
                        + "."
        );

        validation.setShowPromptBox(true);
        validation.createPromptBox(
                "Día a totalizar",
                "Abra la lista y seleccione "
                        + "el día que desea consultar."
        );

        sheet.addValidationData(validation);
    }

    private String crearFormulaValorDia(
            String nombreHoja,
            int filaRestotExcel,
            int primeraFilaEntidadExcel,
            int ultimaFilaEntidadExcel) {

        String hojaEscapada =
                nombreHoja.replace("'", "''");

        /*
         * En las hojas de entidad:
         * B = código PUC
         * D:AH = D1:D31
         *
         * E9 de consolidado contiene el número de día seleccionado.
         */
        return "IFERROR("
                + "INDEX('"
                + hojaEscapada
                + "'!$D$"
                + primeraFilaEntidadExcel
                + ":$AH$"
                + ultimaFilaEntidadExcel
                + ","
                + "MATCH($A"
                + filaRestotExcel
                + ",'"
                + hojaEscapada
                + "'!$B$"
                + primeraFilaEntidadExcel
                + ":$B$"
                + ultimaFilaEntidadExcel
                + ",0),"
                + "$E$9"
                + "),0)";
    }

    private static String nombreColumna(
            EntidadReporte entidad) {

        return entidad.nombreEntidad()
                .toUpperCase(ES_CO);
    }

    private static String nombreHojaUnico(
            Workbook workbook,
            String base) {

        String safe =
                base == null || base.isBlank()
                        ? "ENTIDAD"
                        : base;

        safe = safe.replaceAll(
                "[\\\\/?*\\[\\]:]",
                "_"
        );

        if (safe.length() > 31) {
            safe = safe.substring(0, 31);
        }

        String candidate = safe;
        int suffix = 2;

        while (workbook.getSheet(candidate) != null) {
            String ending = "_" + suffix++;
            int max =
                    Math.max(
                            1,
                            31 - ending.length()
                    );

            candidate =
                    safe.substring(
                            0,
                            Math.min(
                                    safe.length(),
                                    max
                            )
                    )
                            + ending;
        }

        return candidate;
    }

    private void addLabelValue(
            Sheet sheet,
            Styles styles,
            int rowIndex,
            String label,
            String value) {

        Row row = sheet.createRow(rowIndex);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label + ":");
        labelCell.setCellStyle(styles.label);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);

        sheet.addMergedRegion(
                new CellRangeAddress(
                        rowIndex,
                        rowIndex,
                        1,
                        6
                )
        );
    }

    private String nombreArchivo(
            LocalDate fechaCorte) {

        String mes =
                fechaCorte.getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                ES_CO
                        );

        mes =
                mes.substring(0, 1)
                        .toUpperCase(ES_CO)
                        + mes.substring(1);

        return "VOLUNTARIAS "
                + mes
                + " "
                + fechaCorte.getYear()
                + ".xlsx";
    }

    private Path guardarCopia(
            Path rutaBaseSalida,
            LocalDate fechaCorte,
            String fileName,
            byte[] content)
            throws IOException {

        Path base =
                rutaBaseSalida != null
                        ? rutaBaseSalida
                        : properties.getSalidasDir();

        String mes =
                fechaCorte.getMonthValue()
                        + " "
                        + fechaCorte.getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                ES_CO
                        );

        mes =
                mes.substring(0, mes.indexOf(' ') + 1)
                        + mes.substring(mes.indexOf(' ') + 1, mes.indexOf(' ') + 2)
                        .toUpperCase(ES_CO)
                        + mes.substring(mes.indexOf(' ') + 2);

        Path directory =
                base.toAbsolutePath()
                        .normalize()
                        .resolve("Balances")
                        .resolve(
                                Integer.toString(
                                        fechaCorte.getYear()
                                )
                        )
                        .resolve(mes);

        Files.createDirectories(directory);

        Path outputFile =
                directory.resolve(fileName);

        Files.write(outputFile, content);

        return outputFile;
    }

    private record DailyKey(
            int codigoPuc,
            int day) {
    }

    public record GeneratedReport(
            String fileName,
            byte[] content,
            Path archivoGuardado) {
    }

    private static final class Styles {

        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle header;
        private final CellStyle number;
        private final CellStyle totalNumber;
        private final CellStyle label;
        private final CellStyle daySelectorLabel;
        private final CellStyle daySelector;
        private final CellStyle unitsCentered;

        private Styles(Workbook workbook) {

            Font titleFont =
                    workbook.createFont();

            titleFont.setBold(true);
            titleFont.setFontHeightInPoints(
                    (short) 14
            );

            Font bold =
                    workbook.createFont();

            bold.setBold(true);

            Font selectorFont =
                    workbook.createFont();

            selectorFont.setBold(true);
            selectorFont.setColor(
                    IndexedColors.WHITE
                            .getIndex()
            );
            selectorFont.setFontHeightInPoints(
                    (short) 13
            );

            title =
                    workbook.createCellStyle();

            title.setFont(titleFont);

            subtitle =
                    workbook.createCellStyle();

            subtitle.setFont(bold);

            header =
                    workbook.createCellStyle();

            header.setFont(bold);
            header.setAlignment(
                    HorizontalAlignment.CENTER
            );
            header.setVerticalAlignment(
                    VerticalAlignment.CENTER
            );
            header.setWrapText(true);
            header.setBorderBottom(
                    BorderStyle.THIN
            );
            header.setBorderTop(
                    BorderStyle.THIN
            );
            header.setBorderLeft(
                    BorderStyle.THIN
            );
            header.setBorderRight(
                    BorderStyle.THIN
            );

            DataFormat format =
                    workbook.createDataFormat();

            number =
                    workbook.createCellStyle();

            number.setDataFormat(
                    format.getFormat(
                            "#,##0.000"
                    )
            );

            totalNumber =
                    workbook.createCellStyle();

            totalNumber.cloneStyleFrom(number);
            totalNumber.setFont(bold);

            label =
                    workbook.createCellStyle();

            label.setFont(bold);

            daySelectorLabel =
                    workbook.createCellStyle();

            daySelectorLabel.setFont(bold);
            daySelectorLabel.setAlignment(
                    HorizontalAlignment.RIGHT
            );
            daySelectorLabel.setVerticalAlignment(
                    VerticalAlignment.CENTER
            );

            daySelector =
                    workbook.createCellStyle();

            /*
             * Selector con apariencia de botón:
             * fondo azul claro, texto azul oscuro y bordes con contraste
             * para simular un ligero relieve.
             */
            selectorFont.setColor(
                    IndexedColors.DARK_BLUE.getIndex()
            );

            daySelector.setFont(selectorFont);
            daySelector.setAlignment(
                    HorizontalAlignment.CENTER
            );
            daySelector.setVerticalAlignment(
                    VerticalAlignment.CENTER
            );
            daySelector.setFillForegroundColor(
                    IndexedColors.PALE_BLUE.getIndex()
            );
            daySelector.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND
            );

            daySelector.setBorderTop(
                    BorderStyle.MEDIUM
            );
            daySelector.setTopBorderColor(
                    IndexedColors.WHITE.getIndex()
            );

            daySelector.setBorderLeft(
                    BorderStyle.MEDIUM
            );
            daySelector.setLeftBorderColor(
                    IndexedColors.WHITE.getIndex()
            );

            daySelector.setBorderBottom(
                    BorderStyle.MEDIUM
            );
            daySelector.setBottomBorderColor(
                    IndexedColors.DARK_BLUE.getIndex()
            );

            daySelector.setBorderRight(
                    BorderStyle.MEDIUM
            );
            daySelector.setRightBorderColor(
                    IndexedColors.DARK_BLUE.getIndex()
            );

            unitsCentered =
                    workbook.createCellStyle();

            unitsCentered.setFont(bold);
            unitsCentered.setAlignment(
                    HorizontalAlignment.CENTER
            );
        }
    }
}
