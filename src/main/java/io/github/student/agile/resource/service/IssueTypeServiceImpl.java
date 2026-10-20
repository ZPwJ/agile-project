package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.IssueType;
import io.github.student.agile.resource.repository.IssueTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueTypeServiceImpl implements IssueTypeService {

    private final IssueTypeRepository issueTypeRepository;

    @Override
    public List<IssueType> getAllIssueTypes() {
        return issueTypeRepository.findAll();
    }
}
