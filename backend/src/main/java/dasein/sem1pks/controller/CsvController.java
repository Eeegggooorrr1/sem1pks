package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.csv.ImportMode;
import dasein.sem1pks.dto.request.listing.ListingFilter;
import dasein.sem1pks.dto.response.csv.ImportPreview;
import dasein.sem1pks.dto.response.csv.ImportResult;
import dasein.sem1pks.service.CatalogService;
import dasein.sem1pks.service.CsvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class CsvController {
    private final CsvService service;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@Valid @ModelAttribute ListingFilter filter) {
        if (!CatalogService.isAdmin()) {
            filter.setArchived(false);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"listings.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(service.export(filter));
    }

    @PostMapping(value = "/import/check", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportPreview check(@AuthenticationPrincipal CurrentUser user, @RequestParam MultipartFile file,
            @RequestParam ImportMode mode) throws IOException {
        return service.check(user.id(), file.getBytes(), mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult apply(@AuthenticationPrincipal CurrentUser user, @RequestParam MultipartFile file,
            @RequestParam ImportMode mode, @RequestParam String confirmation) throws IOException {
        return service.apply(user.id(), file.getBytes(), mode, confirmation);
    }
}
