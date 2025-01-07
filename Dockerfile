FROM openjdk:17-jdk-slim

ARG SBT_VERSION=1.10.6
RUN apt-get update && apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99e82a75642ac823" | gpg --dearmor > /etc/apt/trusted.gpg.d/sbt.gpg && \
    apt-get update && apt-get install -y sbt=${SBT_VERSION} && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY build.sbt /app/

RUN sbt update

ENV SBT_OPTS="--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"

CMD ["sbt", "project remainder; run"]
