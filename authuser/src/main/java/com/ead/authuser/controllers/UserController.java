package com.ead.authuser.controllers;

import com.ead.authuser.dtos.UserDto;
import com.ead.authuser.models.UserModel;
import com.ead.authuser.services.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping
    public ResponseEntity<List<UserModel>> getAllUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getOneUser(@PathVariable(value = "id") UUID id) {
        Optional<UserModel> userModelOptional = userService.findById(id);

        return userModelOptional
                .<ResponseEntity<Object>>map(userModel -> ResponseEntity.status(HttpStatus.OK).body(userModel))
                .orElseGet(UserController::userNotFound);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable(value = "id") UUID id) {
        Optional<UserModel> userModelOptional = userService.findById(id);

        if (userModelOptional.isEmpty()) {
            return userNotFound();
        } else {
            userService.delete(id);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("User deleted success.");
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable(value = "userId") UUID userId,
            @Validated(UserDto.UserView.UserPut.class) @RequestBody @JsonView(UserDto.UserView.UserPut.class) UserDto userDto
    ) {

        Optional<UserModel> userModelOptional = userService.findById(userId);

        if(userModelOptional.isEmpty())
            return userNotFound();

        var userModel = userModelOptional.get();
        userModel.setFullName(userDto.getFullName());
        userModel.setPhoneNumber(userDto.getPhoneNumber());
        userModel.setCpf(userDto.getCpf());
        userModel.setLastUpdateDate(LocalDateTime.now(ZoneId.of("UTC")));

        userService.updateUser(userModel);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userModel);
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<?> updatePassword(
            @PathVariable(value = "userId") UUID userId,
            @RequestBody @Validated(UserDto.UserView.PasswordPut.class) @JsonView(UserDto.UserView.PasswordPut.class) UserDto userDto
    ) {
        Optional<UserModel> userModelOptional = userService.findById(userId);

        if(userModelOptional.isEmpty())
            return userNotFound();

        if(!userModelOptional.get().getPassword().equals(userDto.getOldPassword())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Error: Mismatched old password!");
        }

        var userModel = userModelOptional.get();
        userModel.setPassword(userDto.getPassword());
        userModel.setLastUpdateDate(LocalDateTime.now(ZoneId.of("UTC")));

        userService.updatePassword(userModel);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Password updated successfully.");
    }

    @PatchMapping("/{userId}/image")
    public ResponseEntity<?> updateImage(
            @PathVariable(value = "userId") UUID userId,
            @RequestBody @Validated(UserDto.UserView.ImagePut.class) @JsonView(UserDto.UserView.ImagePut.class) UserDto userDto
    ) {
        Optional<UserModel> userModelOptional = userService.findById(userId);

        if(userModelOptional.isEmpty())
            return userNotFound();

        var userModel = userModelOptional.get();
        userModel.setImageUrl(userDto.getImageUrl());
        userModel.setLastUpdateDate(LocalDateTime.now(ZoneId.of("UTC")));

        userService.updateUser(userModel);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userModel);
    }

    private static ResponseEntity<Object> userNotFound() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("User not found.");
    }

}
