package io.github.student.agile.resource.controller;


import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.dto.SprintRaportDTO;
import io.github.student.agile.resource.service.RaportSprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class RaportSprintController {

    private final RaportSprintService raportSprintService;


    @GetMapping("/sprint/raport/{sprintId}")
    public ResponseEntity<SprintRaportDTO> getSprintRaport(@PathVariable Integer sprintId) {
        return ResponseEntity.ok(raportSprintService.getRaport(sprintId));
    }

}
