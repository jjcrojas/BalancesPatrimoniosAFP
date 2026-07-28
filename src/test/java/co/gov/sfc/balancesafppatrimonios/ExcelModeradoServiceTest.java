package co.gov.sfc.balancesafppatrimonios;

import co.gov.sfc.balancesafppatrimonios.config.BalancesAfpPatrimoniosProperties;
import co.gov.sfc.balancesafppatrimonios.model.BalanceDiario;
import co.gov.sfc.balancesafppatrimonios.model.CuentaPuc;
import co.gov.sfc.balancesafppatrimonios.model.EntidadReporte;
import co.gov.sfc.balancesafppatrimonios.service.ExcelModeradoService;
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

class ExcelModeradoServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generaModeradoConLaMismaEstructuraInteractiva() throws Exception {
        BalancesAfpPatrimoniosProperties properties = new BalancesAfpPatrimoniosProperties();
        properties.setGuardarCopiaLocal(false);
        properties.setSalidasDir(tempDir);
        properties.setTipoEntidad(23);
        properties.setEstadoEntidadVigente(1);
        properties.setTipoInforme(17);
        properties.setTipoPatrimonioModerado(6);
        properties.setCodigoPatrimonioModerado(1000);

        LocalDate fecha = LocalDate.of(2026, 6, 30);
        EntidadReporte entidad = new EntidadReporte(2, "PROTECCION", "ENT_2", "PROTECCION", false);
        EntidadReporte skandiaAlternativo = new EntidadReporte(
                9, "SKANDIA ALTERNATIVO", "SKANDIA_ALT", "SKANDIA_ALT", true
        );
        CuentaPuc cuenta = new CuentaPuc(100000, "ACTIVO");
        BalanceDiario saldo = new BalanceDiario(
                2, "PROTECCION", "ENT_2", 100000, "ACTIVO", fecha, new BigDecimal("123.456")
        );
        BalanceDiario saldoSkandiaAlternativo = new BalanceDiario(
                9, "SKANDIA", "SKANDIA_ALT", 100000, "ACTIVO", fecha, new BigDecimal("45.678")
        );

        ExcelModeradoService.GeneratedReport report =
                new ExcelModeradoService(properties).generar(
                        fecha,
                        tempDir,
                        List.of(entidad, skandiaAlternativo),
                        List.of(cuenta),
                        List.of(saldo, saldoSkandiaAlternativo)
                );

        assertTrue(report.fileName().startsWith("MODERADO Junio 2026"));
        assertEquals(null, report.archivoGuardado());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report.content()))) {
            assertNotNull(workbook.getSheet("restot"));
            assertNotNull(workbook.getSheet("PROTECCION"));
            assertNotNull(workbook.getSheet("SKANDIA_ALT"));
            assertEquals("Moderado", workbook.getSheet("restot").getRow(6).getCell(1).getStringCellValue());
            assertEquals("1000", workbook.getSheet("restot").getRow(5).getCell(1).getStringCellValue());
            assertEquals(30, workbook.getSheet("restot").getRow(8).getCell(4).getNumericCellValue());
            assertEquals(
                    45.678,
                    workbook.getSheet("SKANDIA_ALT").getRow(5).getCell(32).getNumericCellValue(),
                    0.001
            );
        }
    }
}
