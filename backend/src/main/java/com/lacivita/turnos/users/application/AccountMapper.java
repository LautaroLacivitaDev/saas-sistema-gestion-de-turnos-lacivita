package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.users.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface AccountMapper {

    @Mapping(target = "email", source = "email.value")
    @Mapping(target = "emailVerified", expression = "java(user.isEmailVerified())")
    @Mapping(target = "hasPassword", expression = "java(user.hasPassword())")
    AccountView toView(User user);

    default AuthenticatedUser toPrincipal(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getName(), user.getPlatformRole());
    }

    default SignIn toSignIn(User user) {
        return new SignIn(toPrincipal(user), toView(user));
    }
}
