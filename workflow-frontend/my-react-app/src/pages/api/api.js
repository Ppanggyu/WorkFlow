import axios from "axios";

// 기본 axios 인스턴스
export const api = axios.create({
  baseURL: "http://localhost:8081",
  withCredentials: true, // refreshToken 쿠키 보내려면 필수
});

// accessToken 주입(아래에서 setter로 연결)
let accessToken = null;
export const setApiAccessToken = (token) => {
  accessToken = token;
};

// 요청마다 Authorization 자동 세팅
// Authorization: Bearer <accessToken>
api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// refresh는 api 말고 "순수 axios"로 호출(무한루프 방지)
export const refreshClient = axios.create({
  baseURL: "http://localhost:8081",
  withCredentials: true,
});

// 리프래쉬 1번만
// 첫번째 요청만 수행 후 나머지는 queue에 대기
// 리프래쉬 성공하면 대기 중인 요청들을 새 토큰으로 다시 실행
let isRefreshing = false;
let queue = [];

// queue 실행 함수
// refresh 성공 → resolve(token) 호출해서 대기 요청들 재시도
// refresh 실패 → reject(error) 해서 대기 요청들 전부 실패 처리
const runQueue = (error, token = null) => {
  queue.forEach((p) => (error ? p.reject(error) : p.resolve(token)));
  queue = [];
};

// 응답 인터셉터: 401이면 리프래쉬 후 재시도
// 성공 응답은 통과 실패하면 여기서 처리
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;

    // // 로그 확인용
    // if (error.response?.status === 401) {
    //   console.log("[API 401]", original.method, original.url);
    // }

    // 401이 아니면 패스
    if (error.response?.status !== 401) return Promise.reject(error);

    // refresh 자체 호출이면 패스(무한루프 방지)
    if (original.url?.includes("/api/refresh")) return Promise.reject(error);

    // 이미 재시도 한 요청이면 패스(무한루프 방지)
    if (original._retry) return Promise.reject(error);
    original._retry = true;

    // 동시에 여러 요청이 401 나면 refresh 1번만 하고 나머진 대기
    // 이미 리프래쉬 중이면 queue에 대기
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        queue.push({
          resolve: (token) => {
            original.headers.Authorization = `Bearer ${token}`;
            resolve(api(original));
          },
          reject,
        });
      });
    }

    // 리프래쉬 시작
    isRefreshing = true;

    try {
      const res = await refreshClient.post("/api/refresh");
      const newToken = res.data.accessToken;

      setApiAccessToken(newToken);

      runQueue(null, newToken);

      original.headers.Authorization = `Bearer ${newToken}`;
      return api(original);

      // 실패 처리
    } catch (e) {
      runQueue(e, null);
      setApiAccessToken(null);
      return Promise.reject(e);

      // 리프래쉬 상태 해제
    } finally {
      isRefreshing = false;
    }
  }
);
