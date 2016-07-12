FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/puff-puff-dash.jar /puff-puff-dash/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/puff-puff-dash/app.jar"]
