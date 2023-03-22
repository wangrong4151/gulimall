FROM java:8

VOLUME /tmp

COPY gulimall-product.jar gulimall-product.jar

RUN bash -c "touch /gulimall-product.jar"

EXPOSE 7070

ENTRYPOINT ["java", "-jar", "gulimall-product.jar", "--server.port=10000", "> /log/gulimall-product.log"]
