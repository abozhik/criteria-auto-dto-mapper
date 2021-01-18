package abozhik.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {

    private Long id;
    private String email;
    private String login;
    private RoleDto role;
    private UserDepartmentDto userDepartment;


    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoleDto {
        private Long id;
        private String code;
    }

}
