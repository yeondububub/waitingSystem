import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 1000 },
        { duration: '1m', target: 1000 },
        { duration: '10s', target: 0 },    // 테스트 종료 시 부하를 서서히 감소 (안정적인 종료를 위해 권장됨)
    ],
};

export default function () {
    const userId = Math.floor(Math.random() * 999999) + 1;

    const url = `http://localhost:9090/api/v1/waiting/queue/checked?userId=${userId}`;
    const res = http.get(url);


    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}