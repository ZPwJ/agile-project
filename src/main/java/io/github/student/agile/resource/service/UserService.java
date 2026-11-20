package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.UserDTO;

import java.util.Set;

public interface UserService {

    UserDTO getCurrentLoggedUser();
    UserDTO getUserByEmail(String email);
    UserDTO getUserByUUID(String uuid);
    UserDTO getUserSafely(String uuid);
    boolean isProjectOwner(Integer projectID);
    boolean isAnyProjectOwner(String uuid);
    boolean hasProjectAccess(Integer projectID);
    void verifyProjectAccess(Integer projectID);
    Set<String> getAllProjectUsersUUIDs(Integer projectID);
}
