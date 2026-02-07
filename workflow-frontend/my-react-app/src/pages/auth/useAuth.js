import { useContext } from "react";
import { AuthCtx } from "./AuthContext";

export const useAuth = () => useContext(AuthCtx);


// useAuth: React 커스텀 훅(custom hook), AuthCtx Context를 쓰는 표준 방법을 하나 만들어둔 것
// useContext(AuthCtx)가 하는 일: AuthProvider에서 내려준 토큰들을 꺼냄.