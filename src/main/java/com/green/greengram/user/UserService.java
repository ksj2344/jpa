package com.green.greengram.user;

import com.green.greengram.common.CookieUtils;
import com.green.greengram.common.MyFileUtils;
import com.green.greengram.common.exception.CustomException;
import com.green.greengram.common.exception.UserErrorCode;
import com.green.greengram.config.jwt.JwtUser;
import com.green.greengram.config.jwt.TokenProvider;
import com.green.greengram.config.security.AuthenticationFacade;
import com.green.greengram.entity.User;
import com.green.greengram.user.model.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper mapper;
    private final MyFileUtils myFileUtils;
    private final PasswordEncoder passwordEncoder; //WebSecurityConfig에서 빈등록된 메소드 객체
    private final TokenProvider tokenProvider;
    private final CookieUtils cookieUtils;
    private final AuthenticationFacade authenticationFacade;
    private final UserRepository userRepository;

    public int postSignUp(MultipartFile pic, UserSignUpReq p) {
        //프로필 이미지 파일 처리
        String savedPicName = (pic != null ? myFileUtils.makeRandomFileName(pic) : null);

        //String hashedPassword = BCrypt.hashpw(p.getUpw(), BCrypt.gensalt());
        String hashedPassword = passwordEncoder.encode(p.getUpw());
        log.info("hashedPassword: {}", hashedPassword);

        User user = new User();
        user.setNickName(p.getNickName());
        user.setUid(p.getUid());
        user.setUpw(hashedPassword);
        user.setPic(savedPicName);

        //int result = mapper.insUser(p);
        userRepository.save(user);

        if(pic == null) {
            return 1;
        }

        // 저장 위치 만든다.
        // middlePath = user/${userId}
        // filePath = user/${userId}/${savedPicName}
        long userId = user.getUserId(); //userId를 insert 후에 얻을 수 있다.
        String middlePath = String.format("user/%d", userId);
        myFileUtils.makeFolders(middlePath);
        log.info("middlePath: {}", middlePath);
        String filePath = String.format("%s/%s", middlePath, savedPicName);
        try {
            myFileUtils.transferTo(pic, filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public UserSignInRes postSignIn(UserSignInReq p, HttpServletResponse response) {
        User user=userRepository.findByUid(p.getUid()); //PK를 알 때는 findById를 사용 가능
        //UserSignInRes res= mapper.selUserByUid(p.getUid());
        if(user==null || !passwordEncoder.matches(p.getUpw(), user.getUpw())){
            throw new CustomException(UserErrorCode.INCORRECT_ID_PW);
        }

        // JWT 토큰 생성: AccessToken(100분)<-인증용, RefreshToken(15일)<-재발행용
        JwtUser jwtUser = new JwtUser();
        jwtUser.setSignedUserId(user.getUserId());
        List<String> roles = new ArrayList<>(2);
        roles.add("ROLE_USER");
        roles.add("ROLE_ADMIN");
        jwtUser.setRoles(roles);
        String accessToken=tokenProvider.generateToken(jwtUser, Duration.ofMinutes(30));
        String refreshToken=tokenProvider.generateToken(jwtUser, Duration.ofDays(15));

        //refreshToken은 쿠키에 담는다.
        int maxAge=1_296_000; //15*24*60*60, 15일의 초(second)값
        cookieUtils.setCookie(response,"refreshToken",refreshToken,maxAge); //요청, 이름, value, 만료시간

        return new UserSignInRes(user.getUserId(), user.getNickName(), user.getPic() ,accessToken);
    }

    public UserInfoGetRes GetUserInfo(UserInfoGetReq req) {
        req.setSignedUserId(authenticationFacade.getSignedUserId());
        return mapper.selUserInfo(req);
    }

    public String getAccessToken(HttpServletRequest req) {
        Cookie cookie= cookieUtils.getCookie(req,"refreshToken");
        String refreshToken=cookie.getValue();
        JwtUser jwtUser = tokenProvider.getJwtUserFromToken(refreshToken);
        String accessToken=tokenProvider.generateToken(jwtUser, Duration.ofMinutes(30));
        return accessToken;
    }

    public String patchUserPic (UserPicPatchReq p){
        p.setSignedUserId(authenticationFacade.getSignedUserId());
        //1. 저장할 파일명(랜덤한 파일명) 생성한다. 이때, 확장자는 오리지널 파일명과 일치하게 한다.
        String savedPicName=p.getPic() != null? myFileUtils.makeRandomFileName(p.getPic()):null;
        //파일 먼저 만들기(있으면 실행 안하고 없으면 오류남)
        myFileUtils.makeFolders(String.format("user/%d", p.getSignedUserId()));
        //2. 기존 파일 삭제(방법 2가지 [1]: 폴더를 지운다.  [2]:select해서 기존 파일명을 얻어온다. [3]:기존파일명을 FE에서 받는다.)
        String path=String.format("%s/user/%d",myFileUtils.getUploadPath(),p.getSignedUserId());
        myFileUtils.deleteFolder(path, false);
        //3. 원하는 위치에 저장할 파일명으로 파일을 이동(transferTo)한다.
        String filePath=String.format("user/%d/%s",p.getSignedUserId(),savedPicName);
        try {
            myFileUtils.transferTo(p.getPic(),filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //4. DB에 잇는 튜플을 수정(Update)한다.
        p.setPicName(savedPicName);
        mapper.updUserPic(p);

        return savedPicName;
    }
}
