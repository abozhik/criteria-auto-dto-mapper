package abozhik.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "user_department_status")
public class UserDepartmentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_department_status_user_department_status_id_seq")
    @SequenceGenerator(name = "user_department_status_user_department_status_id_seq", sequenceName = "user_department_status_user_department_status_id_seq", allocationSize = 1)
    @Column(name = "user_department_status_id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;


}
