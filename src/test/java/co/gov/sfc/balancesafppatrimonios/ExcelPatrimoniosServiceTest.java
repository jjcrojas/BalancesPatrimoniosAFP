package co.gov.sfc.balancesafppatrimonios;

import co.gov.sfc.balancesafppatrimonios.config.BalancesAfpPatrimoniosProperties;
import co.gov.sfc.balancesafppatrimonios.model.BalanceDiario;
import co.gov.sfc.balancesafppatrimonios.model.CuentaPuc;
import co.gov.sfc.balancesafppatrimonios.model.EntidadReporte;
import co.gov.sfc.balancesafppatrimonios.service.ExcelPatrimoniosService;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelPatrimoniosServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generaRestotEntidadesYSkandiaAlternativo()
            throws Exception {

        BalancesAfpPatrimoniosProperties properties =
                crearPropertiesParaPrueba();

        ExcelPatrimoniosService service =
                new ExcelPatrimoniosService(properties);

        LocalDate fecha =
                LocalDate.of(2026, 5, 31);

        List<EntidadReporte> entidades =
                List.of(
                        new EntidadReporte(
                                2,
                                "PROTECCION",
                                "ENT_2",
                                "PROTECCION",
                                false
                        ),
                        new EntidadReporte(
                                9,
                                "SKANDIA",
                                "ENT_9",
                                "SKANDIA",
                                false
                        ),
                        new EntidadReporte(
                                9,
                                "SKANDIA ALTERNATIVO",
                                "SKANDIA_ALT",
                                "SKANDIA_ALT",
                                true
                        )
                );

        List<CuentaPuc> cuentas =
                List.of(
                        new CuentaPuc(
                                100000,
                                "ACTIVO"
                        )
                );

        List<BalanceDiario> datos =
                List.of(
                        new BalanceDiario(
                                2,
                                "PROTECCION",
                                "ENT_2",
                                100000,
                                "ACTIVO",
                                fecha,
                                new BigDecimal(
                                        "201785692.180"
                                )
                        ),
                        new BalanceDiario(
                                9,
                                "SKANDIA",
                                "ENT_9",
                                100000,
                                "ACTIVO",
                                fecha,
                                new BigDecimal(
                                        "27971582.544"
                                )
                        ),
                        new BalanceDiario(
                                9,
                                "SKANDIA",
                                "SKANDIA_ALT",
                                100000,
                                "ACTIVO",
                                fecha,
                                new BigDecimal(
                                        "463120.116"
                                )
                        )
                );

        ExcelPatrimoniosService.GeneratedReport report =
                service.generar(
                        fecha,
                        tempDir,
                        entidades,
                        cuentas,
                        datos
                );

        assertNotNull(report);
        assertTrue(report.content().length > 0);
        assertTrue(report.fileName().endsWith(".xlsx"));

        /*
         * Como guardarCopiaLocal está desactivado en esta prueba,
         * no debe haberse creado un archivo físico.
         */
        assertEquals(null, report.archivoGuardado());

        try (
            XSSFWorkbook workbook =
                    new XSSFWorkbook(
                            new ByteArrayInputStream(
                                    report.content()
                            )
                    )
        ) {
            assertNotNull(
                    workbook.getSheet("restot")
            );

            assertNotNull(
                    workbook.getSheet("PROTECCION")
            );

            assertNotNull(
                    workbook.getSheet("SKANDIA")
            );

            assertNotNull(
                    workbook.getSheet("SKANDIA_ALT")
            );

            /*
             * E9 es fila 9, columna E.
             * En Apache POI los índices empiezan en cero:
             * fila 8, celda 4.
             */
            double diaSeleccionado =
                    workbook.getSheet("restot")
                            .getRow(8)
                            .getCell(4)
                            .getNumericCellValue();

            assertEquals(31, diaSeleccionado);
        }
    }

    private BalancesAfpPatrimoniosProperties
            crearPropertiesParaPrueba() {

        BalancesAfpPatrimoniosProperties properties =
                new BalancesAfpPatrimoniosProperties();

        /*
         * En una prueba unitaria Spring no carga application.properties.
         * Por eso se asignan explícitamente los valores necesarios.
         */
        properties.setSalidasDir(tempDir);

        properties.setGuardarCopiaLocal(false);

        properties.setMaxPoiFileMb(80);

        properties.setTipoEntidad(23);

        properties.setEstadoEntidadVigente(1);

        properties.setTipoPatrimonio(6);

        properties.setTipoInforme(17);

        properties.setCodigoEntidadSkandia(9);

        properties.setCodigosPuc(
                List.of(
                        100000,
                        110000,
                        110500,
                        110505,
                        110510,
                        110515,
                        110520
                )
        );

        properties.setCodigosPatrimonioObligatorio(
                List.of(
                        1000,
                        5000,
                        6000,
                        7000,
                        8000
                )
        );

        properties.setCodigosPatrimonioAdicionalSkandia(
                List.of(4)
        );

        properties.setCodigosPatrimonioSkandiaAlternativo(
                List.of(
                        4,
                        8000
                )
        );

        return properties;
    }
}