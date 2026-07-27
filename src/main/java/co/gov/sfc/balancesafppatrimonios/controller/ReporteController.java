package co.gov.sfc.balancesafppatrimonios.controller;

import co.gov.sfc.balancesafppatrimonios.config.BalancesAfpPatrimoniosProperties;
import co.gov.sfc.balancesafppatrimonios.model.BalanceDiario;
import co.gov.sfc.balancesafppatrimonios.model.CuentaPuc;
import co.gov.sfc.balancesafppatrimonios.model.EntidadReporte;
import co.gov.sfc.balancesafppatrimonios.model.ReporteForm;
import co.gov.sfc.balancesafppatrimonios.repository.BalancePatrimoniosRepository;
import co.gov.sfc.balancesafppatrimonios.service.ExcelCesantiasCortoPlazoService;
import co.gov.sfc.balancesafppatrimonios.service.ExcelCesantiasLargoPlazoService;
import co.gov.sfc.balancesafppatrimonios.service.ExcelCesantiasTotalService;
import co.gov.sfc.balancesafppatrimonios.service.ExcelConservadorService;
import co.gov.sfc.balancesafppatrimonios.service.ExcelMayorRiesgoService;
import co.gov.sfc.balancesafppatrimonios.service.ExcelPatrimoniosService;
import co.gov.sfc.balancesafppatrimonios.service.ReportesZipService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Controller
public class ReporteController {

    private static final Logger log =
            LoggerFactory.getLogger(ReporteController.class);

    private static final Locale ES_CO =
            Locale.of("es", "CO");

    private static final MediaType ZIP =
            MediaType.parseMediaType("application/zip");

    private final BalancePatrimoniosRepository repository;
    private final ExcelPatrimoniosService excelPatrimoniosService;
    private final ExcelCesantiasCortoPlazoService excelCesantiasCortoPlazoService;
    private final ExcelCesantiasLargoPlazoService excelCesantiasLargoPlazoService;
    private final ExcelCesantiasTotalService excelCesantiasTotalService;
    private final ExcelConservadorService excelConservadorService;
    private final ExcelMayorRiesgoService excelMayorRiesgoService;
    private final ReportesZipService reportesZipService;
    private final BalancesAfpPatrimoniosProperties properties;

    public ReporteController(
            BalancePatrimoniosRepository repository,
            ExcelPatrimoniosService excelPatrimoniosService,
            ExcelCesantiasCortoPlazoService excelCesantiasCortoPlazoService,
            ExcelCesantiasLargoPlazoService excelCesantiasLargoPlazoService,
            ExcelCesantiasTotalService excelCesantiasTotalService,
            ExcelConservadorService excelConservadorService,
            ExcelMayorRiesgoService excelMayorRiesgoService,
            ReportesZipService reportesZipService,
            BalancesAfpPatrimoniosProperties properties) {

        this.repository = repository;
        this.excelPatrimoniosService = excelPatrimoniosService;
        this.excelCesantiasCortoPlazoService =
                excelCesantiasCortoPlazoService;
        this.excelCesantiasLargoPlazoService =
                excelCesantiasLargoPlazoService;
        this.excelCesantiasTotalService = excelCesantiasTotalService;
        this.excelConservadorService = excelConservadorService;
        this.excelMayorRiesgoService = excelMayorRiesgoService;
        this.reportesZipService = reportesZipService;
        this.properties = properties;
    }

    @GetMapping("/")
    public String inicio(Model model) {

        ReporteForm form = new ReporteForm();

        LocalDate mesAnterior =
                LocalDate.now().minusMonths(1);

        form.setFechaCorte(
                mesAnterior.withDayOfMonth(
                        mesAnterior.lengthOfMonth()
                )
        );

        form.setRutaBaseSalida(
                properties.getSalidasDir().toString()
        );

        model.addAttribute("reporteForm", form);

        return "index";
    }

    @PostMapping("/reportes/balances-afp-patrimonios")
    public Object generar(
            @Valid
            @ModelAttribute("reporteForm")
            ReporteForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "index";
        }

        if (form.getFechaCorte().isAfter(LocalDate.now())) {

            bindingResult.rejectValue(
                    "fechaCorte",
                    "future",
                    "La fecha no puede estar en el futuro"
            );

            return "index";
        }

        try {
            LocalDate fechaCorte =
                    form.getFechaCorte();

            Path rutaBaseSalida =
                    Path.of(
                            form.getRutaBaseSalida().trim()
                    );

            List<CuentaPuc> cuentas =
                    repository.consultarCuentas();

            List<EntidadReporte> entidadesFisicas =
                    repository.consultarEntidadesFisicas();

            List<EntidadReporte> entidadesSistemaTotal =
                    repository.construirEntidadesSistemaTotal(
                            entidadesFisicas
                    );

            List<BalanceDiario> datosSistemaTotal =
                    repository.consultarMes(fechaCorte);

            ExcelPatrimoniosService.GeneratedReport sistemaTotal =
                    excelPatrimoniosService.generar(
                            fechaCorte,
                            rutaBaseSalida,
                            entidadesSistemaTotal,
                            cuentas,
                            datosSistemaTotal
                    );

            List<BalanceDiario> datosCortoPlazo =
                    repository.consultarCesantiasCortoPlazo(
                            fechaCorte
                    );

            ExcelCesantiasCortoPlazoService.GeneratedReport cortoPlazo =
                    excelCesantiasCortoPlazoService.generar(
                            fechaCorte,
                            rutaBaseSalida,
                            entidadesFisicas,
                            cuentas,
                            datosCortoPlazo
                    );

            List<BalanceDiario> datosLargoPlazo =
                    repository.consultarCesantiasLargoPlazo(
                            fechaCorte
                    );

            ExcelCesantiasLargoPlazoService.GeneratedReport largoPlazo =
                    excelCesantiasLargoPlazoService.generar(
                            fechaCorte,
                            rutaBaseSalida,
                            entidadesFisicas,
                            cuentas,
                            datosLargoPlazo
                    );

            List<BalanceDiario> datosCesantiasTotal =
                    repository.consultarCesantiasTotal(fechaCorte);

            ExcelCesantiasTotalService.GeneratedReport cesantiasTotal =
                    excelCesantiasTotalService.generar(
                            fechaCorte,
                            rutaBaseSalida,
                            entidadesFisicas,
                            cuentas,
                            datosCesantiasTotal
                    );

            List<BalanceDiario> datosConservador =
                    repository.consultarConservador(fechaCorte);

            ExcelConservadorService.GeneratedReport conservador =
                    excelConservadorService.generar(
                            fechaCorte,
                            rutaBaseSalida,
                            entidadesFisicas,
                            cuentas,
                            datosConservador
                    );

            List<BalanceDiario> datosMayorRiesgo =
                    repository.consultarMayorRiesgo(fechaCorte);

            ExcelMayorRiesgoService.GeneratedReport mayorRiesgo =
                    excelMayorRiesgoService.generar(
                            fechaCorte,
                            rutaBaseSalida,
                            entidadesFisicas,
                            cuentas,
                            datosMayorRiesgo
                    );

            registrarGuardado(
                    sistemaTotal.archivoGuardado()
            );

            registrarGuardado(
                    cortoPlazo.archivoGuardado()
            );

            registrarGuardado(
                    largoPlazo.archivoGuardado()
            );

            registrarGuardado(
                    cesantiasTotal.archivoGuardado()
            );

            registrarGuardado(
                    conservador.archivoGuardado()
            );

            registrarGuardado(
                    mayorRiesgo.archivoGuardado()
            );

            byte[] zip =
                    reportesZipService.crearZip(
                            List.of(
                                    new ReportesZipService.ArchivoZip(
                                            sistemaTotal.fileName(),
                                            sistemaTotal.content()
                                    ),
                                    new ReportesZipService.ArchivoZip(
                                            cortoPlazo.fileName(),
                                            cortoPlazo.content()
                                    ),
                                    new ReportesZipService.ArchivoZip(
                                            largoPlazo.fileName(),
                                            largoPlazo.content()
                                    ),
                                    new ReportesZipService.ArchivoZip(
                                            cesantiasTotal.fileName(),
                                            cesantiasTotal.content()
                                    ),
                                    new ReportesZipService.ArchivoZip(
                                            conservador.fileName(),
                                            conservador.content()
                                    ),
                                    new ReportesZipService.ArchivoZip(
                                            mayorRiesgo.fileName(),
                                            mayorRiesgo.content()
                                    )
                            )
                    );

            ContentDisposition disposition =
                    ContentDisposition.attachment()
                            .filename(
                                    nombreZip(fechaCorte),
                                    StandardCharsets.UTF_8
                            )
                            .build();

            return ResponseEntity.ok()
                    .contentType(ZIP)
                    .contentLength(zip.length)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposition.toString()
                    )
                    .body(
                            new ByteArrayResource(zip)
                    );

        } catch (Exception ex) {

            log.error(
                    "Error generando los reportes AFP "
                            + "para la fecha {} y ruta {}",
                    form.getFechaCorte(),
                    form.getRutaBaseSalida(),
                    ex
            );

            model.addAttribute(
                    "error",
                    "No fue posible generar los reportes. "
                            + "Revise la consola de Eclipse."
            );

            return "index";
        }
    }

    private void registrarGuardado(Path archivo) {

        if (archivo != null) {

            log.info(
                    "Reporte guardado en {}",
                    archivo
            );
        }
    }

    private String nombreZip(LocalDate fechaCorte) {

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

        return "REPORTES AFP "
                + mes
                + " "
                + fechaCorte.getYear()
                + ".zip";
    }
}
