package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.AuthToken;

public interface LoginService {

    AuthToken login(String username, String rawPassword);
}
