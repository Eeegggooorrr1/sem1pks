package dasein.sem1pks.controller;

import dasein.sem1pks.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ExportService exportService;
    @GetMapping(value="/export", produces="application/zip")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"domain-export.zip\"")
            .cacheControl(CacheControl.noStore())
            .body(exportService.export());
    }
}
