package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.AccountLoginRequest;
import dasein.sem1pks.dto.request.AccountRegisterRequest;
import dasein.sem1pks.dto.request.ListingCreateRequest;
import dasein.sem1pks.dto.response.AuthResponse;
import dasein.sem1pks.dto.response.ListingResponse;
import dasein.sem1pks.service.AccountService;
import dasein.sem1pks.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Validated
public class ListingController {

    private final ListingService listingService;


    @PostMapping("")
    public ResponseEntity<ListingResponse> createListing(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ListingCreateRequest request) {
        ListingResponse listingResponse = listingService.createListing(
                currentUser.id(),
                request.title(),
                request.description(),
                request.price(),
                request.category()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(listingResponse);
    }


}