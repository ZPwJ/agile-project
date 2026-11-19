package io.github.student.agile.builder;

import io.github.student.agile.resource.dto.UserDTO;

public class UserBuilder {

    public static final String CURRENT_USER_FIRSTNAME = "TestName";
    public static final String SECOND_USER_FIRSTNAME = "John";
    public static final String CURRENT_USER_LASTNAME = "TestSurname";
    public static final String SECOND_USER_LASTNAME = "Doe";
    public static final String CURRENT_USER_EMAIL = "test@susie.com";
    public static final String SECOND_USER_EMAIL = "john.doe@anonymous.com";
    public static final String CURRENT_USER_UUID = "be65431c-5951-11ee-8c99-0242ac120002";
    public static final String SECOND_USER_UUID = "08244ba8-6512-11ee-8c99-0242ac120002";

    public static UserDTO createCurrentLoggedInUser() {
        return UserDTO.builder()
                .firstName(CURRENT_USER_FIRSTNAME)
                .lastName(CURRENT_USER_LASTNAME)
                .email(CURRENT_USER_EMAIL)
                .uuid(CURRENT_USER_UUID)
                .build();
    }

    public static UserDTO prepareAnotherUserMock() {
        return UserDTO.builder()
                .firstName(SECOND_USER_FIRSTNAME)
                .lastName(SECOND_USER_LASTNAME)
                .email(SECOND_USER_EMAIL)
                .uuid(SECOND_USER_UUID)
                .build();
    }

    public static UserDTO createAnotherCurrentLoggedInUser(String uuid) {
        UserDTO userDTO = createCurrentLoggedInUser();
        userDTO.setUuid(uuid);
        return userDTO;
    }
}
