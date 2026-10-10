package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.User;

public interface RegisterUserService {

    User register(String username, String rawPassword);
}
