package com.workflow.auth.jwt;

// jjwt 라이브러리(0.12.x)의 핵심 클래스들
// Jwts.builder() : 토큰 만들기
// Jwts.parser() : 토큰 파싱/검증하기
// Keys.hmacShaKeyFor() : HMAC 서명용 키 만들기
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// @Component로 빈 등록
// @Value로 application.properties/yml 값 주입
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// SecretKey : 서명/검증에 사용할 키 타입
// StandardCharsets.UTF_8 : 문자열을 바이트로 변환할 때 인코딩 고정
// Date : issuedAt / expiration 설정용
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT를 “발급(createToken)”, “검증(validate)”, “토큰에서 username 뽑기(getUsername)” 하는 유틸 클래스
// 이 클래스를 스프링이 자동으로 Bean으로 등록
// 다른 곳에서 JwtProvider를 주입받아 사용할 수 있게 됨
@Component
public class JwtProvider {
	
	// key: JWT 서명(Sign)과 검증(Verify)에 쓰는 비밀키
	// expMillis : 토큰 만료 시간(밀리초 단위)
    private final SecretKey key;
    private final long expMillis;

    // 생성자: 설정값 주입 + 가공
    // @Value("${jwt.secret}"): application.properties에 있는 jwt.secret 값을 문자열로 주입
    // @Value("${jwt.access-token-exp-min}"): jwt.access-token-exp-min 값을 long으로 주입 (예: 30)
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-exp-min}") long expMin
    ) {
    	// 비밀키로 SecretKey 생성
    	// secret 문자열을 UTF-8 바이트 배열로 바꿈
    	// Keys.hmacShaKeyFor(...): HMAC 알고리즘(HS256/HS384/HS512) 서명에 맞는 SecretKey로 변환
    	// 중요: secret이 너무 짧으면 여기서 예외가 나거나(혹은 validate에서 실패) 보안상 위험함으로 HS256 기준 최소 32바이트 이상 권장
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // 만료시간을 “분 → 밀리초”로 변환
        // expMin이 “분” 단위니까 분 × 60초 × 1000ms = 밀리초로 변환
        // ex) 30분이면 30 * 60_000 = 1,800,000ms
        this.expMillis = expMin * 60_000;
    }

    // 토큰 생성
    // now: 현재 시간
    // exp: 현재 시간 + 만료시간 = 토큰 만료 시각
    public String createToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expMillis);

        // Jwts.builder() : JWT 만들기 시작
        return Jwts.builder()
        		// .subject(username) : payload의 sub(subject) 클레임에 username 저장
        		// 이걸 나중에 getUsername()으로 다시 꺼냄
                .subject(username)
                // issuedAt(now) : iat(issued at): 언제 발급됐는지 기록
                .issuedAt(now)
                // .expiration(exp) : exp: 언제 만료되는지 기록
                .expiration(exp)
                // .signWith(key) : 위 payload를 key로 서명해서 위조/변조 방지
                .signWith(key)   // ← 0.12.x 방식
                // .compact() : 최종적으로 xxxxx.yyyyy.zzzzz 형태 문자열(JWT) 생성
                .compact();
    }

    // 토큰 검증
    public boolean validate(String token) {
        try {
        	// Jwts.parser() : JWT 파서 생성
            Jwts.parser()
            		// .verifyWith(key) : 이 key로 서명을 검증하겠다
            		// 토큰이 조작되었거나 다른 키로 서명되면 실패
                    .verifyWith(key)   // ← 0.12.x 핵심
                    // .build() : 파서 빌드
                    .build()
                    // .parseSignedClaims(token) : “서명된 JWT”를 파싱(검증 포함)
                    // 여기서 보통 다음을 검사하게 됨:
                    // 서명 유효한지, 만료됐는지(exp), 형식이 맞는지
                    // 예외 나면 false: 만료됨, 서명 불일치, 토큰 깨짐 등등
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // username 추출
    // 파서로 토큰을 검증하면서 파싱
    // payload(클레임들) 가져오기
    // payload의 sub 값을 반환
    // .getPayload() : 클레임 맵(내용) 부분
    // .getSubject() : 그중 sub 값 (createToken에서 넣어둔 username)
    // 즉, createToken()에서 .subject(username)로 넣고 getUsername()에서 .getSubject()로 꺼내는 구조
    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
