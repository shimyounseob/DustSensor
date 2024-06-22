# Air Quality Sensing Application

Dust Sensor 또는 Air Quality Sensor가 부착된 라즈베리 파이에서 Broadcasting되는 데이터 패킷을 수신해 정해진 서버에 전송하는 안드로이드 어플리케이션 제작 프로젝트

## 주요 기능 및 요구사항

- **데이터 패킷 파싱**: OTP 값, 센싱 시간, 센서 데이터 정보를 규칙에 따라 파싱.
- **Wi-Fi 위치 정보 인증**: Wi-Fi 네트워크 스캔 후 위치 인증을 위해 서버로 데이터를 전송하고 키 값을 수신.
- **Retrofit 객체 생성**: Dust Sensor와 Air Quality 인터페이스에 따라 Retrofit 객체 생성.
- **Event Reliability 충족**: 각 센싱 시스템에서 1시간 동안 120개의 데이터가 서버로 전송되게 함.

## 기술 스택

- **언어**: Java
- **개발 도구**: Android Studio
- **라이브러리**: Retrofit, Gson

## 시스템 아키텍처

![Opensource-6주차](https://github.com/shimyounseob/air-quality-sensing-application/assets/97441805/7823dbeb-a302-4397-8fcf-01640ee42ddd)

## 센싱 이미지

![KakaoTalk_20240623_011722914](https://github.com/shimyounseob/air-quality-sensing-application/assets/97441805/84d06a34-7583-4580-b5c7-7ccb7eb43b7c)
