#!/bin/bash

# 변수 선언
host="http://localhost:8080"
pointId="1"
amount=100

# 100번 반복
for i in {1..100}
do
  curl -X PATCH "$host/point/$pointId/charge" \
       -H "Content-Type: application/json" \
       -d "{\"amount\": $amount}" &
done

wait