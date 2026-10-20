package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.CommitmentRuleDTO;
import io.github.student.agile.resource.service.CommitmentRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/commitment/dod")
@RequiredArgsConstructor
public class CommitmentRuleRestController {

    private final CommitmentRuleService ruleService;

    @GetMapping("/project/{projectID}")
    public ResponseEntity<Set<CommitmentRuleDTO>> getAllProjectsDODRules(@PathVariable Integer projectID) {
        return ResponseEntity.ok(ruleService.getAllRules(projectID));
    }

    @PostMapping("/project/{projectID}/rule")
    public ResponseEntity<CommitmentRuleDTO> defineSingleRule(@PathVariable Integer projectID, @RequestParam String rule) {
        return ResponseEntity.ok(ruleService.defineRule(projectID, rule));
    }

    @PatchMapping("/rule/{ruleID}")
    public ResponseEntity<CommitmentRuleDTO> updateSingleRule(@PathVariable Integer ruleID, @RequestParam String rule) {
        return ResponseEntity.ok(ruleService.updateRule(ruleID, rule));
    }

    @DeleteMapping("/project/{projectID}/rule/{ruleID}")
    public void deleteSingleRules(@PathVariable Integer projectID, @PathVariable Integer ruleID) {
        ruleService.deleteRule(projectID, ruleID);
    }

    @DeleteMapping("/project/{projectID}/rule")
    public void deleteAllProjectDefinedRules(@PathVariable Integer projectID) {
        ruleService.deleteAllRules(projectID);
    }
}
