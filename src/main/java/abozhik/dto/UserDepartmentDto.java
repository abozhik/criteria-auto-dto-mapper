package abozhik.dto;

import abozhik.model.UserDepartmentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDepartmentDto {

    private Long id;
    private String name;
    private UserDepartmentStatus status;

}
