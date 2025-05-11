package com.cloud.bff.controllers;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.UserModel;
import com.cloud.bff.services.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) { this.userService = userService; }

    @GetMapping("/getUsers")
    public ResponseModel getUsers() {
        return userService.getUsers();
    }

    @PostMapping("/registerUser")
    public ResponseModel registerUser(@RequestBody UserModel user) {
        return userService.addUser(user);
    }

    @PutMapping("/editUser")
    public ResponseModel editUser(@RequestBody UserModel user) {
        return userService.updateUser(user);
    }

    @DeleteMapping("/deleteUser/{id}")
    public ResponseModel deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }


}
