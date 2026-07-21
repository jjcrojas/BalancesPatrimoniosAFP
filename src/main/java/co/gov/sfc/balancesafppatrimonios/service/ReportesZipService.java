package co.gov.sfc.balancesafppatrimonios.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ReportesZipService {

    public byte[] crearZip(List<ArchivoZip> archivos) {
        try (ByteArrayOutputStream output =
                     new ByteArrayOutputStream();
             ZipOutputStream zip =
                     new ZipOutputStream(output)) {

            for (ArchivoZip archivo : archivos) {
                ZipEntry entry =
                        new ZipEntry(archivo.nombre());

                zip.putNextEntry(entry);
                zip.write(archivo.contenido());
                zip.closeEntry();
            }

            zip.finish();
            return output.toByteArray();

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No fue posible crear el ZIP de reportes.",
                    ex
            );
        }
    }

    public record ArchivoZip(
            String nombre,
            byte[] contenido) {
    }
}
