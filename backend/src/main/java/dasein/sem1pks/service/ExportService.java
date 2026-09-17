package dasein.sem1pks.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.util.zip.*;

@Service
@RequiredArgsConstructor
public class ExportService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public byte[] export() {
        try (var bytes = new ByteArrayOutputStream(); var zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            writeTable(zip, "users", "id,username,email,is_blocked,role");
            writeTable(zip, "listings", "id,title,description,price,status,user_id,category,created_at,version");
            writeTable(zip, "orders", "id,listing_id,buyer_id,status,order_date,version");
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeTable(ZipOutputStream zip, String table, String columns) throws IOException {
        zip.putNextEntry(new ZipEntry(table + ".csv"));
        var writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        jdbc.query("SELECT " + columns + " FROM " + table + " ORDER BY id", rs -> {
            try {
                ResultSetMetaData metadata = rs.getMetaData();
                int count = metadata.getColumnCount();
                for (int i=1; i<=count; i++) {
                    if (i>1) writer.write(',');
                    writer.write(csv(metadata.getColumnLabel(i)));
                }
                writer.write("\r\n");
                while (rs.next()) {
                    for (int i=1; i<=count; i++) {
                        if (i>1) writer.write(',');
                        Object value = rs.getObject(i);
                        writer.write(csv(value == null ? "" : value.toString()));
                    }
                    writer.write("\r\n");
                }
                writer.flush();
            } catch(IOException e) { throw new UncheckedIOException(e); }
            return null;
        });
        zip.closeEntry();
    }

    static String csv(String value) {
        String stripped = value.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0))>=0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
