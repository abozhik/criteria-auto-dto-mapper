package abozhik.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "user_department")
public class UserDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_department_user_department_id_seq")
    @SequenceGenerator(name = "user_department_user_department_id_seq", sequenceName = "user_department_user_department_id_seq", allocationSize = 1)
    @Column(name = "user_department_id")
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_department_status_id")
    private UserDepartmentStatus status;

}
