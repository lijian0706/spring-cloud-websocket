FROM java:8
VOLUME /images
RUN mkdir /app
ADD target/app.jar /app/app.jar
ADD runboot.sh /app/
RUN bash -c 'touch /app/app.jar'
WORKDIR /app
RUN chmod a+x runboot.sh
EXPOSE 80
CMD /app/runboot.sh
RUN echo "Asia/Shanghai" > /etc/timezone;