package com.workflow.auth.service;

import java.time.LocalDateTime;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.auth.dto.Tokens;
import com.workflow.auth.entity.AuthEntity;
import com.workflow.auth.jwt.JwtProvider;
import com.workflow.auth.repository.AuthRepository;
import com.workflow.common.exception.UnauthorizedException;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.UserStatus;
import com.workflow.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
// final이거나 @NonNull이 붙은 필드만 파라미터로 받는 생성자를 자동 생성
@RequiredArgsConstructor
@Transactional
public class AuthService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final AuthRepository authRepository;
	
	// 해시 secret 키 -> properties에 있음
	@Value("${hasher.secret}")
	private String hashSecret;
	
	
	public Tokens login(UserEntity userEntity) {
		
		UserEntity user = userRepository.findByEmail(userEntity.getEmail())
				.orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 다릅니다."));
		
		if(!passwordEncoder.matches(userEntity.getPassword(), user.getPassword())) {
			// throw new RuntimeException("비밀번호 다름"); = 애가 실행되면 메서드가 종료
			throw new UnauthorizedException("이메일 또는 비밀번호가 다릅니다.");
		}
		
		// 유저 상태 및 마지막 로그인 시간 업데이트
		user.setStatus(UserStatus.ACTIVE);
		user.setLastLoginAt(LocalDateTime.now());
		
		String accessToken = jwtProvider.createAccessToken(user);
		String refreshToken = jwtProvider.createRefreshToken(user.getId());
		
		// 해시값 변경
		String hashToken = hash(refreshToken);
		
		// DB에 리프래쉬 토큰 저장하기
	    // 있으면 갱신, 없으면 생성
		// 현재 유효한 토큰 열을 찾음
	    AuthEntity auth = authRepository.findByUserAndRevokedAtIsNull(user)
	    		// 있으면 그 열을 쓰고 없으면 새로 만듬
	            .orElseGet(() -> AuthEntity.builder().user(user).build());
	    auth.setTokenHash(hashToken);
	    auth.setRevokedAt(null);
	    auth.setExpiresAt(LocalDateTime.now().plusDays(7)); // 만료 갱신
	    authRepository.save(auth);
	    
	    
	    
		return new Tokens(accessToken, refreshToken);	
		
	}
	
	// 리프래쉬 토큰으로 액세스 토큰 발급
    @Transactional(readOnly = true)
    public String refresh(String refreshToken) {

        if (refreshToken == null) throw new UnauthorizedException("인증 정보가 유효하지 않습니다.");

        try {
            // 서명/만료 검증 + 파싱 (한 번)
            Claims claims = jwtProvider.parseAndValidate(refreshToken);

            // refresh 토큰인지 확인
            if (!jwtProvider.isRefreshToken(claims)) {
                throw new UnauthorizedException("리프레시 토큰 타입이 아닙니다.");
            }

            // sub(subject) = userId
            Long userId = jwtProvider.getUserId(claims);

            // RefreshToken 토큰 해시값 변경
            String hashToken = hash(refreshToken);

            // RefreshToken 토큰 찾기(폐기 안 된 것만)
            AuthEntity auth = authRepository.findByTokenHashAndRevokedAtIsNull(hashToken)
                    .orElseThrow(() -> new UnauthorizedException("로그아웃 또는 폐기된 토큰입니다."));

            // 토큰과 유저 매칭 검증
            // 토큰은 위조가 아니더라도, DB에 있는 토큰의 user와 claims의 userId를 비교해서 확인
            if (!auth.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("토큰 사용자가 불일치합니다.");
            }

            UserEntity user = auth.getUser();

            // 새 access 발급
            return jwtProvider.createAccessToken(user);

        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("리프레시 토큰이 유효하지 않습니다.");
        }
    }
	
	@Transactional 
	public void logout(String logoutRefreshToken) {
		
		if (logoutRefreshToken == null) {
	        throw new UnauthorizedException("인증 정보가 유효하지 않습니다.");
	    }
		
		// RefreshToken 가져와서 해시값으로 변경
		String hashToken = hash(logoutRefreshToken);
		
		// 해시값으로 변경한 RefreshToken 찾기
		AuthEntity auth = authRepository.findByTokenHashAndRevokedAtIsNull(hashToken)
		.orElseThrow(() -> new UnauthorizedException("인증 정보가 유효하지 않습니다."));
		
		// 로그아웃 누른 현 시점 시각 넣음
		auth.setRevokedAt(LocalDateTime.now());
		
		// 로그아웃 시 상태 업데이트
		auth.getUser().setStatus(UserStatus.DISABLED);
	}
	
	
	// 해시값 변경 로직
	// SHA_256 -> 시크릿키 없이도 입력 = 같은 결과
	// HMAC_SHA_256 -> 시크릿키 없으면 해시만들기 불가
	private String hash(String refreshToken) {
		return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hashSecret)
                .hmacHex(refreshToken);
	}
	

}
