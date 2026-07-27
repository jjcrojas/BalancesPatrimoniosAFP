package co.gov.sfc.balancesafppatrimonios.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "balances-afp-patrimonios")
public class BalancesAfpPatrimoniosProperties {

	@NotNull
	private Path salidasDir;

	@NotEmpty
	private List<Integer> codigosPuc;

	@NotEmpty
	private List<Integer> codigosPatrimonioObligatorio;

	@NotEmpty
	private List<Integer> codigosPatrimonioAdicionalSkandia;

	@NotEmpty
	private List<Integer> codigosPatrimonioSkandiaAlternativo;

	@NotNull
	private Integer codigoEntidadSkandia;

	@NotNull
	private Integer tipoEntidad;

	@NotNull
	private Integer estadoEntidadVigente;

	@NotNull
	private Integer tipoPatrimonio;

	@NotNull
	private Integer tipoInforme;

	@NotNull
	private Integer tipoPatrimonioCesantiasCortoPlazo;

	@NotNull
	private Integer codigoPatrimonioCesantiasCortoPlazo;

	@NotNull
	private Integer tipoPatrimonioCesantiasLargoPlazo;

	@NotNull
	private Integer codigoPatrimonioCesantiasLargoPlazo;

	@NotEmpty
	private List<Integer> codigosPatrimonioCesantiasTotal;

	@NotNull
	private Integer tipoPatrimonioConservador;

	@NotNull
	private Integer codigoPatrimonioConservador;

	@NotNull
	private Boolean guardarCopiaLocal;

	@NotNull
	private Integer maxPoiFileMb;

	@Valid
	@NotNull
	private Datasource datasource = new Datasource();

	public Path getSalidasDir() {
		return salidasDir;
	}

	public void setSalidasDir(Path salidasDir) {
		this.salidasDir = salidasDir;
	}

	public List<Integer> getCodigosPuc() {
		return codigosPuc;
	}

	public void setCodigosPuc(List<Integer> codigosPuc) {
		this.codigosPuc = codigosPuc;
	}

	public List<Integer> getCodigosPatrimonioObligatorio() {
		return codigosPatrimonioObligatorio;
	}

	public void setCodigosPatrimonioObligatorio(List<Integer> codigosPatrimonioObligatorio) {

		this.codigosPatrimonioObligatorio = codigosPatrimonioObligatorio;
	}

	public List<Integer> getCodigosPatrimonioAdicionalSkandia() {
		return codigosPatrimonioAdicionalSkandia;
	}

	public void setCodigosPatrimonioAdicionalSkandia(List<Integer> codigosPatrimonioAdicionalSkandia) {

		this.codigosPatrimonioAdicionalSkandia = codigosPatrimonioAdicionalSkandia;
	}

	public List<Integer> getCodigosPatrimonioSkandiaAlternativo() {
		return codigosPatrimonioSkandiaAlternativo;
	}

	public void setCodigosPatrimonioSkandiaAlternativo(List<Integer> codigosPatrimonioSkandiaAlternativo) {

		this.codigosPatrimonioSkandiaAlternativo = codigosPatrimonioSkandiaAlternativo;
	}

	public Integer getCodigoEntidadSkandia() {
		return codigoEntidadSkandia;
	}

	public void setCodigoEntidadSkandia(Integer codigoEntidadSkandia) {

		this.codigoEntidadSkandia = codigoEntidadSkandia;
	}

	public Integer getTipoEntidad() {
		return tipoEntidad;
	}

	public void setTipoEntidad(Integer tipoEntidad) {
		this.tipoEntidad = tipoEntidad;
	}

	public Integer getEstadoEntidadVigente() {
		return estadoEntidadVigente;
	}

	public void setEstadoEntidadVigente(Integer estadoEntidadVigente) {

		this.estadoEntidadVigente = estadoEntidadVigente;
	}

	public Integer getTipoPatrimonio() {
		return tipoPatrimonio;
	}

	public void setTipoPatrimonio(Integer tipoPatrimonio) {

		this.tipoPatrimonio = tipoPatrimonio;
	}

	public Integer getTipoInforme() {
		return tipoInforme;
	}

	public void setTipoInforme(Integer tipoInforme) {
		this.tipoInforme = tipoInforme;
	}

	public Integer getTipoPatrimonioCesantiasCortoPlazo() {
		return tipoPatrimonioCesantiasCortoPlazo;
	}

	public void setTipoPatrimonioCesantiasCortoPlazo(Integer tipoPatrimonioCesantiasCortoPlazo) {

		this.tipoPatrimonioCesantiasCortoPlazo = tipoPatrimonioCesantiasCortoPlazo;
	}

	public Integer getCodigoPatrimonioCesantiasCortoPlazo() {
		return codigoPatrimonioCesantiasCortoPlazo;
	}

	public void setCodigoPatrimonioCesantiasCortoPlazo(Integer codigoPatrimonioCesantiasCortoPlazo) {

		this.codigoPatrimonioCesantiasCortoPlazo = codigoPatrimonioCesantiasCortoPlazo;
	}

	public Integer getTipoPatrimonioCesantiasLargoPlazo() {
		return tipoPatrimonioCesantiasLargoPlazo;
	}

	public void setTipoPatrimonioCesantiasLargoPlazo(Integer tipoPatrimonioCesantiasLargoPlazo) {

		this.tipoPatrimonioCesantiasLargoPlazo = tipoPatrimonioCesantiasLargoPlazo;
	}

	public Integer getCodigoPatrimonioCesantiasLargoPlazo() {
		return codigoPatrimonioCesantiasLargoPlazo;
	}

	public void setCodigoPatrimonioCesantiasLargoPlazo(Integer codigoPatrimonioCesantiasLargoPlazo) {

		this.codigoPatrimonioCesantiasLargoPlazo = codigoPatrimonioCesantiasLargoPlazo;
	}

	public List<Integer> getCodigosPatrimonioCesantiasTotal() {
		return codigosPatrimonioCesantiasTotal;
	}

	public void setCodigosPatrimonioCesantiasTotal(List<Integer> codigosPatrimonioCesantiasTotal) {
		this.codigosPatrimonioCesantiasTotal = codigosPatrimonioCesantiasTotal;
	}

	public Integer getTipoPatrimonioConservador() {
		return tipoPatrimonioConservador;
	}

	public void setTipoPatrimonioConservador(Integer tipoPatrimonioConservador) {
		this.tipoPatrimonioConservador = tipoPatrimonioConservador;
	}

	public Integer getCodigoPatrimonioConservador() {
		return codigoPatrimonioConservador;
	}

	public void setCodigoPatrimonioConservador(Integer codigoPatrimonioConservador) {
		this.codigoPatrimonioConservador = codigoPatrimonioConservador;
	}

	public Boolean getGuardarCopiaLocal() {
		return guardarCopiaLocal;
	}

	public boolean isGuardarCopiaLocal() {
		return Boolean.TRUE.equals(guardarCopiaLocal);
	}

	public void setGuardarCopiaLocal(Boolean guardarCopiaLocal) {

		this.guardarCopiaLocal = guardarCopiaLocal;
	}

	public Integer getMaxPoiFileMb() {
		return maxPoiFileMb;
	}

	public void setMaxPoiFileMb(Integer maxPoiFileMb) {

		this.maxPoiFileMb = maxPoiFileMb;
	}

	public Datasource getDatasource() {
		return datasource;
	}

	public void setDatasource(Datasource datasource) {

		this.datasource = datasource;
	}

	public static class Datasource {

		@NotNull
		private String url;

		@NotNull
		private String driverClassName;

		@NotNull
		private String username;

		@NotNull
		private String password;

		@NotNull
		private Integer maximumPoolSize;

		@NotNull
		private Integer minimumIdle;

		@NotNull
		private Long connectionTimeout;

		@NotNull
		private Long validationTimeout;

		@NotNull
		private Long initializationFailTimeout;

		/*
		 * Tiempo máximo de ejecución de cada consulta, expresado en segundos.
		 */
		@NotNull
		private Integer queryTimeoutSeconds;

		/*
		 * Cantidad total de intentos. Con valor 2: intento inicial + un reintento.
		 */
		@NotNull
		private Integer maxQueryAttempts;

		/*
		 * Espera antes de reintentar, en milisegundos.
		 */
		@NotNull
		private Long retryDelayMs;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getDriverClassName() {
			return driverClassName;
		}

		public void setDriverClassName(String driverClassName) {

			this.driverClassName = driverClassName;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public Integer getMaximumPoolSize() {
			return maximumPoolSize;
		}

		public void setMaximumPoolSize(Integer maximumPoolSize) {

			this.maximumPoolSize = maximumPoolSize;
		}

		public Integer getMinimumIdle() {
			return minimumIdle;
		}

		public void setMinimumIdle(Integer minimumIdle) {

			this.minimumIdle = minimumIdle;
		}

		public Long getConnectionTimeout() {
			return connectionTimeout;
		}

		public void setConnectionTimeout(Long connectionTimeout) {

			this.connectionTimeout = connectionTimeout;
		}

		public Long getValidationTimeout() {
			return validationTimeout;
		}

		public void setValidationTimeout(Long validationTimeout) {

			this.validationTimeout = validationTimeout;
		}

		public Long getInitializationFailTimeout() {
			return initializationFailTimeout;
		}

		public void setInitializationFailTimeout(Long initializationFailTimeout) {

			this.initializationFailTimeout = initializationFailTimeout;
		}

		public Integer getQueryTimeoutSeconds() {
			return queryTimeoutSeconds;
		}

		public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) {

			this.queryTimeoutSeconds = queryTimeoutSeconds;
		}

		public Integer getMaxQueryAttempts() {
			return maxQueryAttempts;
		}

		public void setMaxQueryAttempts(Integer maxQueryAttempts) {

			this.maxQueryAttempts = maxQueryAttempts;
		}

		public Long getRetryDelayMs() {
			return retryDelayMs;
		}

		public void setRetryDelayMs(Long retryDelayMs) {

			this.retryDelayMs = retryDelayMs;
		}
	}
}