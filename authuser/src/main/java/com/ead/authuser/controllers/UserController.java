package com.ead.authuser.controllers;

import com.ead.authuser.dtos.UserDto;
import com.ead.authuser.models.UserModel;
import com.ead.authuser.services.UserService;
import com.ead.authuser.specifications.SpecificationTemplate;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Link;
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
import java.util.Optional;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserModel>> getAllUsers(SpecificationTemplate.UserSpec spec,
                                                       @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<UserModel> userModelPage = userService.findAll(spec, pageable);

        if(!userModelPage.isEmpty()) {
            for (UserModel user : userModelPage.toList()) {
                user.add(buildHateoas(user));
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(userModelPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getOneUser(@PathVariable(value = "id") UUID id) {
        Optional<UserModel> userModelOptional = userService.findById(id);

        if (userModelOptional.isEmpty()) {
            return userNotFound();
        }

        UserModel user = userModelOptional.get();
        user.add(buildHateoas(user));

        return ResponseEntity.status(HttpStatus.OK).body(user);
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

        userModel.add(buildHateoas(userModel));

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

    private ResponseEntity<Object> userNotFound() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("User not found.");
    }

    private Link buildHateoas(UserModel user) {
        return linkTo(methodOn(UserController.class)
                .getOneUser(user.getId())).withSelfRel();
    }

}
