package abozhik.controller;

import abozhik.dto.UserDto;
import abozhik.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/api/users")
    public List<UserDto> getUserDtoList() {
        return userService.getUserDtoList();
    }

}
