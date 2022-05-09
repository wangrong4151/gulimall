FROM java:8

VOLUME /tmp

COPY springBootDocker.jar app.jar

RUN bash -c "touch /app.jar"

EXPOSE 88

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=", "--server.port=88", "> /log/app.log"]