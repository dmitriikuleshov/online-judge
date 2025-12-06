FROM docker:24-dind

RUN apk add --no-cache openjdk21-jre

WORKDIR /app
COPY target/*.jar app.jar
COPY start.sh /start.sh
RUN chmod +x /start.sh

EXPOSE 8080
ENTRYPOINT ["/start.sh"]
