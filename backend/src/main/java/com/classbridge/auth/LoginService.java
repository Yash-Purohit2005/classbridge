package com.classbridge.auth;

import com.classbridge.auth.dto.LoginRequest;
import com.classbridge.auth.dto.LoginResponse;
import com.classbridge.common.exception.InvalidCredentialsException;
import com.classbridge.security.JwtService;
import com.classbridge.user.User;
import com.classbridge.user.UserRepository;
import com.classbridge.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,JwtService jwtService,
                        RefreshTokenService refreshTokenService){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService;
        this.refreshTokenService=refreshTokenService;
    }


    @Transactional
    public LoginResponse loginWithCredentials(LoginRequest loginRequest){

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new InvalidCredentialsException());

       if(!passwordEncoder.matches(loginRequest.password(),user.getPasswordHash())){
           throw new InvalidCredentialsException();
       }

       if(user.getStatus() != UserStatus.ACTIVE){
           throw new InvalidCredentialsException();
       }

       String token = jwtService.generateToken(
               user.getId(),
               user.getOrganization().getId(),
               user.getRole().name()
       );

       String refreshToken = refreshTokenService.issue(user);

       return new LoginResponse(token,refreshToken);
    }
}
