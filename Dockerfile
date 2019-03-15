FROM scf37/java

COPY target/pack /opt
COPY start.sh /opt/start.sh

ENTRYPOINT ["/opt/start.sh"]