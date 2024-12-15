# tdd-jvm
this is tdd-jvm

## 요구사항

- PATCH  `/point/{id}/charge` : 포인트를 충전한다.
- PATCH `/point/{id}/use` : 포인트를 사용한다.
- *GET `/point/{id}` : 포인트를 조회한다.*
- *GET `/point/{id}/histories` : 포인트 내역을 조회한다.*
- *잔고가 부족할 경우, 포인트 사용은 실패하여야 합니다.*
- *동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리되어야 합니다.*