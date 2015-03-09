#These are obviatd:
# mvn -o exec:java -DenvID=local
# mvn -o exec:java -DenvID=qa
# mvn -o exec:java -DenvID=dev


#these can not be run together right now, since masters do not make separate output report dirs yet.
./run.master _self_test/condensed-headers/master-condensed-headers-none.xml
./run.master _self_test/condensed-headers/master-condensed-headers-empty.xml
