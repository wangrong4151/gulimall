FROM java:8

ARG JAR_FILE
COPY ${JAR_FILE} gulimall-product.jar

EXPOSE 10000
#设置编码格式
ENV LANG="zh_CN.UTF-8"
ENV LANGUAGE="zh_CN:zh"
#设置时区为中国
ENV TZ=Asia/Shanghai
ENV DEBIAN_FRONTEND noninteractive
ENTRYPOINT ["java", "-jar", "gulimall-product.jar", "--server.port=10000"]
