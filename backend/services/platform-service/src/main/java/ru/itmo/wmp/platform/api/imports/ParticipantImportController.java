package ru.itmo.wmp.platform.api.imports;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.wmp.platform.api.imports.mapper.ImportMapper;
import ru.itmo.wmp.platform.application.imports.ParticipantImportService;
import ru.itmo.wmp.platform.config.security.JwtPrincipal;
import ru.itmo.wmp.platform.domain.imports.ImportBatch;
import ru.itmo.wmp.platform.dto.imports.ImportBatchResponse;
import ru.itmo.wmp.platform.dto.imports.ImportPreviewResponse;
import ru.itmo.wmp.platform.exception.imports.ImportFileException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/imports")
@PreAuthorize("@accessGuard.isOrganizationAdmin(#organizationId, authentication)")
@RequiredArgsConstructor
public class ParticipantImportController {
    private final ParticipantImportService participantImportService;
    private final ImportMapper importMapper;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Upload participant list",
        description = "Validates the file and returns the changes it would make, nothing is applied yet"
    )
    public ImportPreviewResponse upload(
        @PathVariable Long organizationId,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if (file.isEmpty()) {
            throw new ImportFileException("File is empty");
        }

        ImportBatch batch = participantImportService.upload(
            organizationId,
            file.getOriginalFilename(),
            read(file),
            JwtPrincipal.userId(jwt)
        );

        return preview(batch);
    }

    @GetMapping
    @Operation(
        summary = "List import batches"
    )
    public List<ImportBatchResponse> history(
        @PathVariable Long organizationId
    ) {
        return participantImportService.history(organizationId)
            .stream()
            .map(batch -> importMapper.toResponse(
                batch,
                participantImportService.summarize(batch.getId())
            ))
            .toList();
    }

    @GetMapping("/{batchId}")
    @Operation(
        summary = "Get import preview"
    )
    public ImportPreviewResponse getPreview(
        @PathVariable Long organizationId,
        @PathVariable Long batchId
    ) {
        return preview(participantImportService.getBatch(organizationId, batchId));
    }

    @PostMapping("/{batchId}/apply")
    @Operation(
        summary = "Apply import",
        description = "Applies previously calculated changes, refuses when the file had errors"
    )
    public ImportBatchResponse apply(
        @PathVariable Long organizationId,
        @PathVariable Long batchId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        ImportBatch batch = participantImportService.apply(
            organizationId,
            batchId,
            JwtPrincipal.userId(jwt)
        );

        return importMapper.toResponse(batch, participantImportService.summarize(batchId));
    }

    @PostMapping("/{batchId}/cancel")
    @Operation(
        summary = "Cancel import"
    )
    public ImportBatchResponse cancel(
        @PathVariable Long organizationId,
        @PathVariable Long batchId
    ) {
        ImportBatch batch = participantImportService.cancel(organizationId, batchId);

        return importMapper.toResponse(batch, participantImportService.summarize(batchId));
    }

    private ImportPreviewResponse preview(ImportBatch batch) {
        return importMapper.toPreview(
            batch,
            participantImportService.summarize(batch.getId()),
            participantImportService.changesOf(batch.getId())
        );
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ImportFileException("Cannot read uploaded file", exception);
        }
    }
}
