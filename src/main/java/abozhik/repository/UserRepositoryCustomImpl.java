package abozhik.repository;

import abozhik.dto.UserDto;
import abozhik.dto.automapper.DtoMapper;
import abozhik.model.User;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<UserDto> getUserDtoList() {
        DtoMapper<UserDto> generator = new DtoMapper<>(em, UserDto.class, User.class);
        return generator.getDtoList();
    }

}
