package co.gov.sfc.balancesafppatrimonios.repository;

import co.gov.sfc.balancesafppatrimonios.config.BalancesAfpPatrimoniosProperties;
import co.gov.sfc.balancesafppatrimonios.model.BalanceDiario;
import co.gov.sfc.balancesafppatrimonios.model.CuentaPuc;
import co.gov.sfc.balancesafppatrimonios.model.EntidadReporte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.net.SocketTimeoutException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class BalancePatrimoniosRepository {

	private static final Logger log = LoggerFactory.getLogger(BalancePatrimoniosRepository.class);

	private static final String SQL_ENTIDADES = """
			SELECT DISTINCT
			    e.Codigo_Entidad AS codigo_entidad,
			    TRIM(
			        OREPLACE(
			            e.Nombre_Entidad,
			            '"',
			            ''
			        )
			    ) AS nombre_entidad
			FROM PROD_DWH_CONSULTA.ENTIDADES e
			WHERE e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente
			ORDER BY 1
			""";

	private static final String SQL_BALANCES = """
			SELECT
			    e.Codigo_Entidad AS codigo_entidad,

			    TRIM(
			        OREPLACE(
			            e.Nombre_Entidad,
			            '"',
			            ''
			        )
			    ) AS nombre_entidad,

			    CASE
			        WHEN e.Codigo_Entidad = :codigoSkandia
			         AND pa.Tipo_Patrimonio = :tipoPatrimonio
			         AND pa.Codigo_Patrimonio
			             IN (:patrimoniosSkandiaAlt)
			            THEN 'SKANDIA_ALT'

			        ELSE 'ENT_' ||
			             TRIM(
			                 CAST(
			                     e.Codigo_Entidad AS VARCHAR(20)
			                 )
			             )
			    END AS clave_reporte,

			    p.Codigo AS codigo_puc,
			    TRIM(p.Nombre) AS nombre_cuenta,
			    t.Fecha AS fecha,

			    SUM(
			        eip.Saldo_Sincierre_Total_Moneda_0
			    ) / 1000 AS valor_miles

			FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip

			INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e
			    ON eip.Ent_ID = e.Ent_ID

			INNER JOIN
			    PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa
			    ON eip.Paau_ID = pa.Paau_ID

			INNER JOIN PROD_DWH_CONSULTA.TIEMPO t
			    ON eip.Tie_ID = t.Tie_ID

			INNER JOIN PROD_DWH_CONSULTA.PUC p
			    ON eip.Puc_ID = p.Puc_ID

			WHERE eip.Tipo_Informe = :tipoInforme
			  AND e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente
			  AND pa.Tipo_Patrimonio = :tipoPatrimonio
			  AND p.Codigo IN (:codigosPuc)
			  AND t.Fecha
			      BETWEEN :fechaInicial AND :fechaCorte

			  AND (
			        pa.Codigo_Patrimonio
			            IN (:patrimoniosOrdinarios)

			        OR (
			            e.Codigo_Entidad = :codigoSkandia

			            AND pa.Codigo_Patrimonio
			                IN (:patrimoniosAdicionalesSkandia)
			        )
			  )

			GROUP BY 1, 2, 3, 4, 5, 6
			ORDER BY 1, 3, 4, 6
			""";

	private static final String SQL_CESANTIAS_CORTO_PLAZO = """
			SELECT
			    e.Codigo_Entidad AS codigo_entidad,

			    TRIM(
			        OREPLACE(
			            e.Nombre_Entidad,
			            '"',
			            ''
			        )
			    ) AS nombre_entidad,

			    'ENT_' ||
			    TRIM(
			        CAST(
			            e.Codigo_Entidad AS VARCHAR(20)
			        )
			    ) AS clave_reporte,

			    p.Codigo AS codigo_puc,
			    TRIM(p.Nombre) AS nombre_cuenta,
			    t.Fecha AS fecha,

			    SUM(
			        eip.Saldo_Sincierre_Total_Moneda_0
			    ) / 1000 AS valor_miles

			FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip

			INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e
			    ON eip.Ent_ID = e.Ent_ID

			INNER JOIN
			    PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa
			    ON eip.Paau_ID = pa.Paau_ID

			INNER JOIN PROD_DWH_CONSULTA.TIEMPO t
			    ON eip.Tie_ID = t.Tie_ID

			INNER JOIN PROD_DWH_CONSULTA.PUC p
			    ON eip.Puc_ID = p.Puc_ID

			WHERE eip.Tipo_Informe = :tipoInforme
			  AND e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente

			  AND pa.Tipo_Patrimonio =
			      :tipoPatrimonioCesantias

			  AND pa.Codigo_Patrimonio =
			      :codigoPatrimonioCesantias

			  AND p.Codigo IN (:codigosPuc)

			  AND t.Fecha
			      BETWEEN :fechaInicial AND :fechaCorte

			GROUP BY 1, 2, 3, 4, 5, 6
			ORDER BY 1, 4, 6
			""";

	private static final String SQL_CESANTIAS_LARGO_PLAZO = """
			SELECT
			    e.Codigo_Entidad AS codigo_entidad,

			    TRIM(
			        OREPLACE(
			            e.Nombre_Entidad,
			            '"',
			            ''
			        )
			    ) AS nombre_entidad,

			    'ENT_' ||
			    TRIM(
			        CAST(
			            e.Codigo_Entidad AS VARCHAR(20)
			        )
			    ) AS clave_reporte,

			    p.Codigo AS codigo_puc,
			    TRIM(p.Nombre) AS nombre_cuenta,
			    t.Fecha AS fecha,

			    SUM(
			        eip.Saldo_Sincierre_Total_Moneda_0
			    ) / 1000 AS valor_miles

			FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip

			INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e
			    ON eip.Ent_ID = e.Ent_ID

			INNER JOIN
			    PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa
			    ON eip.Paau_ID = pa.Paau_ID

			INNER JOIN PROD_DWH_CONSULTA.TIEMPO t
			    ON eip.Tie_ID = t.Tie_ID

			INNER JOIN PROD_DWH_CONSULTA.PUC p
			    ON eip.Puc_ID = p.Puc_ID

			WHERE eip.Tipo_Informe = :tipoInforme
			  AND e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente

			  AND pa.Tipo_Patrimonio =
			      :tipoPatrimonioCesantias

			  AND pa.Codigo_Patrimonio =
			      :codigoPatrimonioCesantias

			  AND p.Codigo IN (:codigosPuc)

			  AND t.Fecha
			      BETWEEN :fechaInicial AND :fechaCorte

			GROUP BY 1, 2, 3, 4, 5, 6
			ORDER BY 1, 4, 6
			""";

	private static final String SQL_CESANTIAS_TOTAL = """
			SELECT
			    e.Codigo_Entidad AS codigo_entidad,
			    TRIM(OREPLACE(e.Nombre_Entidad, '"', '')) AS nombre_entidad,
			    'ENT_' || TRIM(CAST(e.Codigo_Entidad AS VARCHAR(20))) AS clave_reporte,
			    p.Codigo AS codigo_puc,
			    TRIM(p.Nombre) AS nombre_cuenta,
			    t.Fecha AS fecha,
			    SUM(eip.Saldo_Sincierre_Total_Moneda_0) / 1000 AS valor_miles
			FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip
			INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e ON eip.Ent_ID = e.Ent_ID
			INNER JOIN PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa ON eip.Paau_ID = pa.Paau_ID
			INNER JOIN PROD_DWH_CONSULTA.TIEMPO t ON eip.Tie_ID = t.Tie_ID
			INNER JOIN PROD_DWH_CONSULTA.PUC p ON eip.Puc_ID = p.Puc_ID
			WHERE eip.Tipo_Informe = :tipoInforme
			  AND e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente
			  AND pa.Tipo_Patrimonio = :tipoPatrimonioCesantias
			  AND pa.Codigo_Patrimonio IN (:codigosPatrimonioCesantias)
			  AND p.Codigo IN (:codigosPuc)
			  AND t.Fecha BETWEEN :fechaInicial AND :fechaCorte
			GROUP BY 1, 2, 3, 4, 5, 6
			ORDER BY 1, 4, 6
			""";

	private static final String SQL_CONSERVADOR = """
			SELECT
			    e.Codigo_Entidad AS codigo_entidad,
			    TRIM(OREPLACE(e.Nombre_Entidad, '"', '')) AS nombre_entidad,
			    'ENT_' || TRIM(CAST(e.Codigo_Entidad AS VARCHAR(20))) AS clave_reporte,
			    p.Codigo AS codigo_puc,
			    TRIM(p.Nombre) AS nombre_cuenta,
			    t.Fecha AS fecha,
			    SUM(eip.Saldo_Sincierre_Total_Moneda_0) / 1000 AS valor_miles
			FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip
			INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e ON eip.Ent_ID = e.Ent_ID
			INNER JOIN PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa ON eip.Paau_ID = pa.Paau_ID
			INNER JOIN PROD_DWH_CONSULTA.TIEMPO t ON eip.Tie_ID = t.Tie_ID
			INNER JOIN PROD_DWH_CONSULTA.PUC p ON eip.Puc_ID = p.Puc_ID
			WHERE eip.Tipo_Informe = :tipoInforme
			  AND e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente
			  AND pa.Tipo_Patrimonio = :tipoPatrimonioConservador
			  AND pa.Codigo_Patrimonio = :codigoPatrimonioConservador
			  AND p.Codigo IN (:codigosPuc)
			  AND t.Fecha BETWEEN :fechaInicial AND :fechaCorte
			GROUP BY 1, 2, 3, 4, 5, 6
			ORDER BY 1, 4, 6
			""";

	private static final String SQL_MODERADO = """
			SELECT
			    e.Codigo_Entidad AS codigo_entidad,
			    TRIM(OREPLACE(e.Nombre_Entidad, '"', '')) AS nombre_entidad,
			    'ENT_' || TRIM(CAST(e.Codigo_Entidad AS VARCHAR(20))) AS clave_reporte,
			    p.Codigo AS codigo_puc,
			    TRIM(p.Nombre) AS nombre_cuenta,
			    t.Fecha AS fecha,
			    SUM(eip.Saldo_Sincierre_Total_Moneda_0) / 1000 AS valor_miles
			FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip
			INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e ON eip.Ent_ID = e.Ent_ID
			INNER JOIN PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa ON eip.Paau_ID = pa.Paau_ID
			INNER JOIN PROD_DWH_CONSULTA.TIEMPO t ON eip.Tie_ID = t.Tie_ID
			INNER JOIN PROD_DWH_CONSULTA.PUC p ON eip.Puc_ID = p.Puc_ID
			WHERE eip.Tipo_Informe = :tipoInforme
			  AND e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente
			  AND pa.Tipo_Patrimonio = :tipoPatrimonioModerado
			  AND pa.Codigo_Patrimonio = :codigoPatrimonioModerado
			  AND p.Codigo IN (:codigosPuc)
			  AND t.Fecha BETWEEN :fechaInicial AND :fechaCorte
			GROUP BY 1, 2, 3, 4, 5, 6
			ORDER BY 1, 4, 6
			""";

	private static final String SQL_MAYOR_RIESGO = """
			SELECT
			    e.Codigo_Entidad AS codigo_entidad,
			    TRIM(OREPLACE(e.Nombre_Entidad, '"', '')) AS nombre_entidad,
			    'ENT_' || TRIM(CAST(e.Codigo_Entidad AS VARCHAR(20))) AS clave_reporte,
			    p.Codigo AS codigo_puc,
			    TRIM(p.Nombre) AS nombre_cuenta,
			    t.Fecha AS fecha,
			    SUM(eip.Saldo_Sincierre_Total_Moneda_0) / 1000 AS valor_miles
			FROM PROD_DWH_CONSULTA.ESTFIN_INDIV_PA eip
			INNER JOIN PROD_DWH_CONSULTA.ENTIDADES e ON eip.Ent_ID = e.Ent_ID
			INNER JOIN PROD_DWH_CONSULTA.PATRIMONIOS_AUTONOMOS pa ON eip.Paau_ID = pa.Paau_ID
			INNER JOIN PROD_DWH_CONSULTA.TIEMPO t ON eip.Tie_ID = t.Tie_ID
			INNER JOIN PROD_DWH_CONSULTA.PUC p ON eip.Puc_ID = p.Puc_ID
			WHERE eip.Tipo_Informe = :tipoInforme
			  AND e.Tipo_Entidad = :tipoEntidad
			  AND e.Estado = :estadoVigente
			  AND pa.Tipo_Patrimonio = :tipoPatrimonioMayorRiesgo
			  AND pa.Codigo_Patrimonio = :codigoPatrimonioMayorRiesgo
			  AND p.Codigo IN (:codigosPuc)
			  AND t.Fecha BETWEEN :fechaInicial AND :fechaCorte
			GROUP BY 1, 2, 3, 4, 5, 6
			ORDER BY 1, 4, 6
			""";

	private static final String SQL_CUENTAS = """
			SELECT
			    p.Codigo AS codigo_puc,
			    MAX(TRIM(p.Nombre)) AS nombre_cuenta
			FROM PROD_DWH_CONSULTA.PUC p
			WHERE p.Codigo IN (:codigosPuc)
			GROUP BY 1
			ORDER BY 1
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final BalancesAfpPatrimoniosProperties properties;

	public BalancePatrimoniosRepository(@Qualifier("balancesAfpPatrimoniosJdbcTemplate") JdbcTemplate jdbcTemplate,

			BalancesAfpPatrimoniosProperties properties) {

		this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

		this.properties = properties;
	}


	public List<EntidadReporte> consultarEntidadesFisicas() {

		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("tipoEntidad", properties.getTipoEntidad())
				.addValue("estadoVigente", properties.getEstadoEntidadVigente());

		return ejecutarConsultaConReintento(
				"consulta de entidades AFP vigentes",
				SQL_ENTIDADES,
				params,
				(rs, rowNum) -> {

					int codigo = rs.getInt("codigo_entidad");

					String nombre =
							limpiarNombre(rs.getString("nombre_entidad"));

					return new EntidadReporte(
							codigo,
							nombre,
							"ENT_" + codigo,
							nombreHoja(nombre, codigo),
							false
					);
				}
		);
	}

	public List<EntidadReporte> construirEntidadesSistemaTotal(
			List<EntidadReporte> entidadesFisicas) {

		List<EntidadReporte> entidades =
				new ArrayList<>(entidadesFisicas);

		boolean skandiaVigente =
				entidades.stream()
						.anyMatch(entidad ->
								entidad.codigoEntidad()
										== properties.getCodigoEntidadSkandia());

		if (!skandiaVigente) {
			return entidades;
		}

		int posicion = entidades.size();

		for (int i = 0; i < entidades.size(); i++) {

			if (entidades.get(i).codigoEntidad()
					== properties.getCodigoEntidadSkandia()) {

				posicion = i + 1;
				break;
			}
		}

		entidades.add(
				posicion,
				new EntidadReporte(
						properties.getCodigoEntidadSkandia(),
						"SKANDIA ALTERNATIVO",
						"SKANDIA_ALT",
						"SKANDIA_ALT",
						true
				)
		);

		return entidades;
	}

	public List<CuentaPuc> consultarCuentas() {

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("codigosPuc", properties.getCodigosPuc());

		List<CuentaPuc> encontradas = ejecutarConsultaConReintento("consulta de cuentas PUC", SQL_CUENTAS, params,
				(rs, rowNum) -> new CuentaPuc(rs.getInt("codigo_puc"), rs.getString("nombre_cuenta")));

		Map<Integer, CuentaPuc> cuentaPorCodigo = encontradas.stream()
				.collect(Collectors.toMap(CuentaPuc::codigoPuc, Function.identity(), (actual, duplicada) -> actual));

		return properties.getCodigosPuc().stream()
				.map(codigo -> cuentaPorCodigo.getOrDefault(codigo, new CuentaPuc(codigo, "")))
				.toList();
	}

	public List<BalanceDiario> consultarMes(LocalDate fechaCorte) {

		LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

		MapSqlParameterSource params = parametrosComunes(fechaInicial, fechaCorte)
				.addValue("tipoPatrimonio", properties.getTipoPatrimonio())
				.addValue("codigoSkandia", properties.getCodigoEntidadSkandia())
				.addValue("patrimoniosOrdinarios", properties.getCodigosPatrimonioObligatorio())
				.addValue("patrimoniosAdicionalesSkandia", properties.getCodigosPatrimonioAdicionalSkandia())
				.addValue("patrimoniosSkandiaAlt", properties.getCodigosPatrimonioSkandiaAlternativo());

		return ejecutarBalances("Sistema Total", SQL_BALANCES, params);
	}

	public List<BalanceDiario> consultarCesantiasCortoPlazo(LocalDate fechaCorte) {

		LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

		MapSqlParameterSource params = parametrosComunes(fechaInicial, fechaCorte)
				.addValue("tipoPatrimonioCesantias", properties.getTipoPatrimonioCesantiasCortoPlazo())
				.addValue("codigoPatrimonioCesantias", properties.getCodigoPatrimonioCesantiasCortoPlazo());

		return ejecutarBalances("Cesantías Corto Plazo", SQL_CESANTIAS_CORTO_PLAZO, params);
	}

	public List<BalanceDiario> consultarCesantiasLargoPlazo(LocalDate fechaCorte) {

		LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

		MapSqlParameterSource params = parametrosComunes(fechaInicial, fechaCorte)
				.addValue("tipoPatrimonioCesantias", properties.getTipoPatrimonioCesantiasLargoPlazo())
				.addValue("codigoPatrimonioCesantias", properties.getCodigoPatrimonioCesantiasLargoPlazo());

		return ejecutarBalances("Cesantías Largo Plazo", SQL_CESANTIAS_LARGO_PLAZO, params);
	}

	public List<BalanceDiario> consultarCesantiasTotal(LocalDate fechaCorte) {
		LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

		MapSqlParameterSource params = parametrosComunes(fechaInicial, fechaCorte)
				.addValue("tipoPatrimonioCesantias", properties.getTipoPatrimonioCesantiasCortoPlazo())
				.addValue("codigosPatrimonioCesantias", properties.getCodigosPatrimonioCesantiasTotal());

		return ejecutarBalances("Cesantías Total", SQL_CESANTIAS_TOTAL, params);
	}

	public List<BalanceDiario> consultarConservador(LocalDate fechaCorte) {
		LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

		MapSqlParameterSource params = parametrosComunes(fechaInicial, fechaCorte)
				.addValue("tipoPatrimonioConservador", properties.getTipoPatrimonioConservador())
				.addValue("codigoPatrimonioConservador", properties.getCodigoPatrimonioConservador());

		return ejecutarBalances("Conservador", SQL_CONSERVADOR, params);
	}

	public List<BalanceDiario> consultarModerado(LocalDate fechaCorte) {
		LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

		MapSqlParameterSource params = parametrosComunes(fechaInicial, fechaCorte)
				.addValue("tipoPatrimonioModerado", properties.getTipoPatrimonioModerado())
				.addValue("codigoPatrimonioModerado", properties.getCodigoPatrimonioModerado());

		return ejecutarBalances("Moderado", SQL_MODERADO, params);
	}

	public List<BalanceDiario> consultarMayorRiesgo(LocalDate fechaCorte) {
		LocalDate fechaInicial = fechaCorte.withDayOfMonth(1);

		MapSqlParameterSource params = parametrosComunes(fechaInicial, fechaCorte)
				.addValue("tipoPatrimonioMayorRiesgo", properties.getTipoPatrimonioMayorRiesgo())
				.addValue("codigoPatrimonioMayorRiesgo", properties.getCodigoPatrimonioMayorRiesgo());

		return ejecutarBalances("Mayor Riesgo", SQL_MAYOR_RIESGO, params);
	}

	private MapSqlParameterSource parametrosComunes(LocalDate fechaInicial, LocalDate fechaCorte) {

		return new MapSqlParameterSource().addValue("tipoInforme", properties.getTipoInforme())
				.addValue("tipoEntidad", properties.getTipoEntidad())
				.addValue("estadoVigente", properties.getEstadoEntidadVigente())
				.addValue("codigosPuc", properties.getCodigosPuc()).addValue("fechaInicial", Date.valueOf(fechaInicial))
				.addValue("fechaCorte", Date.valueOf(fechaCorte));
	}

	private List<BalanceDiario> ejecutarBalances(String nombreConsulta, String sql, MapSqlParameterSource params) {

		return ejecutarConsultaConReintento(nombreConsulta, sql, params,
				(rs, rowNum) -> new BalanceDiario(rs.getInt("codigo_entidad"), rs.getString("nombre_entidad"),
						rs.getString("clave_reporte"), rs.getInt("codigo_puc"), rs.getString("nombre_cuenta"),
						rs.getDate("fecha").toLocalDate(), rs.getBigDecimal("valor_miles")));
	}

	/*
	 * Método general que ejecuta una consulta y, únicamente ante Error 802 o
	 * SocketTimeoutException, la vuelve a ejecutar.
	 */
	private <T> List<T> ejecutarConsultaConReintento(String nombreConsulta, String sql, MapSqlParameterSource params,
			RowMapper<T> rowMapper) {

		int maximoIntentos = properties.getDatasource().getMaxQueryAttempts();

		long esperaMilisegundos = properties.getDatasource().getRetryDelayMs();

		for (int intento = 1; intento <= maximoIntentos; intento++) {

			try {
				log.info("Ejecutando {}. Intento {} de {}.", nombreConsulta, intento, maximoIntentos);

				List<T> resultado = jdbcTemplate.query(sql, params, rowMapper);

				log.info("{} finalizó correctamente " + "en el intento {}. " + "Registros obtenidos: {}.",
						nombreConsulta, intento, resultado.size());

				return resultado;

			} catch (DataAccessException ex) {

				boolean timeoutTransitorio = esTimeoutTeradata(ex);

				if (!timeoutTransitorio) {

					/*
					 * No se reintentan errores de sintaxis, parámetros, permisos, datos, etc.
					 */
					log.error("{} falló por un error que " + "no es reintentable.", nombreConsulta, ex);

					throw ex;
				}

				if (intento >= maximoIntentos) {

					log.error("{} agotó los {} intentos " + "por timeout de Teradata.", nombreConsulta, maximoIntentos,
							ex);

					throw ex;
				}

				log.warn("{} presentó Error 802 o timeout " + "de recepción en el intento {}. "
						+ "Se reintentará en {} ms.", nombreConsulta, intento, esperaMilisegundos);

				esperarAntesDeReintentar(esperaMilisegundos);
			}
		}

		throw new IllegalStateException("No fue posible ejecutar " + nombreConsulta + ".");
	}

	/*
	 * Revisa toda la cadena de causas de la excepción.
	 */
	private boolean esTimeoutTeradata(Throwable throwable) {

		Throwable actual = throwable;

		while (actual != null) {

			if (actual instanceof SocketTimeoutException) {

				return true;
			}

			if (actual instanceof SQLException sqlException) {

				if (sqlException.getErrorCode() == 802) {
					return true;
				}

				String mensaje = sqlException.getMessage();

				if (mensaje != null && mensaje.contains("Timeout occurred " + "for Packet receive")) {

					return true;
				}

				if (mensaje != null && mensaje.contains("Read timed out")) {

					return true;
				}
			}

			String mensaje = actual.getMessage();

			if (mensaje != null && (mensaje.contains("Timeout occurred " + "for Packet receive")
					|| mensaje.contains("Read timed out"))) {

				return true;
			}

			actual = actual.getCause();
		}

		return false;
	}

	private void esperarAntesDeReintentar(long milisegundos) {

		try {
			Thread.sleep(milisegundos);

		} catch (InterruptedException ex) {

			Thread.currentThread().interrupt();

			throw new IllegalStateException("El reintento de la consulta " + "fue interrumpido.", ex);
		}
	}

	private static String limpiarNombre(String nombre) {

		if (nombre == null || nombre.isBlank()) {
			return "ENTIDAD";
		}

		return nombre.replace("\"", "").trim();
	}

	private static String nombreHoja(String nombre, int codigo) {

		String normalizado = limpiarNombre(nombre).toUpperCase(Locale.ROOT).replaceAll("[\\\\/?*\\[\\]:]", "_")
				.replaceAll("\\s+", "_");

		if (normalizado.length() > 31) {
			normalizado = normalizado.substring(0, 31);
		}

		return normalizado.isBlank() ? "ENTIDAD_" + codigo : normalizado;
	}
}
