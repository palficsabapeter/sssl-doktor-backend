FROM hseeberger/scala-sbt:8u222_1.3.4_2.13.1 as Builder

# resolve dependencies first (cache)
WORKDIR /app
COPY build.sbt /app/build.sbt
COPY project /app/project
RUN sbt update test:update it:update

# copy and build app
COPY . .
RUN sbt fmtCheck compile test stage && \
    chmod -R u=rX,g=rX /app/target/universal/stage && \
    chmod u+x,g+x /app/target/universal/stage/bin/sssl-doktor-backend

FROM openjdk:8-alpine

USER root
RUN apk add --no-cache bash && adduser -S -u 1001 sssl-doktor-backend

USER 1001
EXPOSE 9000
ENTRYPOINT ["/app/bin/sssl-doktor-backend"]
CMD []
COPY --from=Builder --chown=1001:root /app/target/universal/stage /app