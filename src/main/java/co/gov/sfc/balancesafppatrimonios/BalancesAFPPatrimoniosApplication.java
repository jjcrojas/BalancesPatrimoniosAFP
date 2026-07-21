package co.gov.sfc.balancesafppatrimonios;

import co.gov.sfc.balancesafppatrimonios.config.BalancesAfpPatrimoniosProperties;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BalancesAfpPatrimoniosProperties.class)
public class BalancesAFPPatrimoniosApplication {

    public static void main(String[] args) {
        ZipSecureFile.setMaxEntrySize(80L * 1024 * 1024);
        SpringApplication.run(BalancesAFPPatrimoniosApplication.class, args);
    }
}
