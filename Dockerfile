FROM maven:3.6.3-jdk-8 AS build
WORKDIR /build
COPY . .
RUN mvn -q -DskipTests package

FROM jboss/wildfly:10.1.0.Final
COPY --from=build /build/target/url-shortener.war /opt/jboss/wildfly/standalone/deployments/
EXPOSE 8080
