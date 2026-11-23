package dasein.sem1pks.service;

import dasein.sem1pks.repository.ExportRepository.Table;
import dasein.sem1pks.repository.ExportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExportRepository repository;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] export() {
        try (var bytes = new ByteArrayOutputStream();
             var zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Table table : Table.values()) {
                writeTable(zip, table);
            }

            zip.finish();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void writeTable(ZipOutputStream zip, Table table) throws IOException {
        zip.putNextEntry(new ZipEntry(table.fileName()));
        var writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);

        writer.write('\uFEFF');
        repository.read(
                table,
                header -> writeRow(writer, header, table != Table.TAGS),
                values -> writeRow(writer, values, true)
        );

        writer.flush();
        zip.closeEntry();
    }

    private void writeRow(Writer writer, List<String> values, boolean quote) {
        try {
            var columns = quote ? values.stream().map(ExportService::csv).toList() : values;
            writer.write(String.join(";", columns));
            writer.write("\r\n");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String csv(String value) {
        String stripped = value.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
