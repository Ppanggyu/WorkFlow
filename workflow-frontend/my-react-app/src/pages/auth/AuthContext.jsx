import { createContext } from "react";

export const AuthCtx = createContext(null);

// createContext: React 전역 상태 통로를 만드는 함수
// AuthCtx: 인증(Auth) 전용 Context, accessToken 같은 걸 담아 전달할 그릇
// createContext(초기값): Provider가 없을 때 기본값
// createContext(null): 이 Context는 Provider가 없으면 아무 값도 없다